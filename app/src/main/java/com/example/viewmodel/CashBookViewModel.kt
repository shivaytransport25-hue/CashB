package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BookSummary
import com.example.data.model.BusinessProfile
import com.example.data.model.CashBook
import com.example.data.model.CashTransaction
import com.example.data.model.DateFilterPreset
import com.example.data.model.LedgerFilterState
import com.example.data.model.Party
import com.example.data.model.PartyBalanceSummary
import com.example.data.model.TransactionWithBalance
import com.example.data.model.isCashIn
import com.example.data.repository.CashBookRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.FinancialCalculationEngine
import com.example.util.StatementExportHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashBookUiState(
    val isLoading: Boolean = true,
    val cashBooks: List<CashBook> = emptyList(),
    val activeBook: CashBook? = null,
    val activeBookSummary: BookSummary = BookSummary(),
    val filteredBookSummary: BookSummary = BookSummary(),
    val transactionsWithBalance: List<TransactionWithBalance> = emptyList(),
    val allTransactions: List<CashTransaction> = emptyList(),
    val allBookTransactionsCount: Int = 0,
    val parties: List<Party> = emptyList(),
    val partySummaries: List<PartyBalanceSummary> = emptyList(),
    val totalReceivable: Double = 0.0, // Total You'll Get
    val totalPayable: Double = 0.0, // Total You'll Give
    val filterState: LedgerFilterState = LedgerFilterState(),
    val businessProfile: BusinessProfile = BusinessProfile(),
    val errorMessage: String? = null,
    val actionBannerMessage: String? = null
) {
    val businessName: String get() = businessProfile.businessName
    val businessGstin: String get() = businessProfile.gstin
}

class CashBookViewModel(
    private val repository: CashBookRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedBookId = MutableStateFlow<String?>(null)
    private val _filterState = MutableStateFlow(LedgerFilterState())
    private val _businessProfile = MutableStateFlow(BusinessProfile())
    private val _transientMessage = MutableStateFlow<Pair<String?, String?>>(null to null) // error to banner
    private val _themeMode = MutableStateFlow(AppThemeMode.CLASSIC_NAVY)
    private val _isDarkTheme = MutableStateFlow(false)
    private var hasAttemptedInitialBookCreation = false

    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    val filterState: StateFlow<LedgerFilterState> = _filterState.asStateFlow()

    fun updateThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    private val booksFlow = repository.observeCashBooks(currentUserId)
        .catch { e ->
            val root = unwrapFirestoreError(e)
            Log.w(TAG, "Error observing cashbooks", root)
            _transientMessage.update { (root.message ?: "Failed to load cash books") to it.second }
            emit(emptyList())
        }

    private val partiesFlow = repository.observeParties(currentUserId)
        .catch { e ->
            val root = unwrapFirestoreError(e)
            Log.w(TAG, "Error observing parties", root)
            _transientMessage.update { (root.message ?: "Failed to load parties") to it.second }
            emit(emptyList())
        }

    private val transactionsFlow = repository.observeTransactions(currentUserId)
        .catch { e ->
            val root = unwrapFirestoreError(e)
            Log.w(TAG, "Error observing transactions", root)
            _transientMessage.update { (root.message ?: "Failed to load transactions") to it.second }
            emit(emptyList())
        }

    val uiState: StateFlow<CashBookUiState> = combine(
        booksFlow,
        partiesFlow,
        transactionsFlow,
        _selectedBookId,
        combine(_filterState, _transientMessage, _businessProfile) { f, msg, biz -> Triple(f, msg, biz) }
    ) { books, parties, allTransactions, selectedId, (filters, messages, bizProfile) ->

        // Automatically seed a default Main Business Cash Book if the user has zero books
        if (books.isEmpty() && !hasAttemptedInitialBookCreation) {
            hasAttemptedInitialBookCreation = true
            viewModelScope.launch {
                repository.createCashBook(
                    name = "Main Office Cash Book",
                    description = "Daily business cash & bank register",
                    currencySymbol = "₹"
                )
            }
        }

        val activeBook = books.find { it.id == selectedId } ?: books.firstOrNull()
        val activeBookId = activeBook?.id.orEmpty()
        val currency = activeBook?.currencySymbol ?: "₹"

        // Compute chronological running balances for all transactions in the active book using decimal-safe minor units
        val bookTransactionsChronological = allTransactions
            .filter { it.bookId == activeBookId }
            .sortedWith(
                compareBy<CashTransaction> { it.transactionDate }
                    .thenBy { it.createdAt?.seconds ?: 0L }
                    .thenBy { it.id }
            )

        var runningPaise = 0L
        var bookTotalInPaise = 0L
        var bookTotalOutPaise = 0L

        val chronologicalWithBal = bookTransactionsChronological.map { tx ->
            val paise = FinancialCalculationEngine.toPaise(tx.amount)
            if (tx.isCashIn) {
                runningPaise += paise
                bookTotalInPaise += paise
            } else {
                runningPaise -= paise
                bookTotalOutPaise += paise
            }
            TransactionWithBalance(
                transaction = tx,
                runningBalance = FinancialCalculationEngine.fromPaise(runningPaise)
            )
        }

        val fullBookSummary = BookSummary(
            bookId = activeBookId,
            bookName = activeBook?.name ?: "Cash Book",
            currencySymbol = currency,
            totalCashIn = FinancialCalculationEngine.fromPaise(bookTotalInPaise),
            totalCashOut = FinancialCalculationEngine.fromPaise(bookTotalOutPaise),
            netBalance = FinancialCalculationEngine.fromPaise(bookTotalInPaise - bookTotalOutPaise),
            totalEntries = chronologicalWithBal.size
        )

        // Apply active filters (Date range, Entry Type, Payment Mode, Category, Party, Search Query)
        val minTimestamp = computeMinTimestamp(filters.datePreset)
        val filteredRowsDesc = chronologicalWithBal
            .filter { item ->
                val tx = item.transaction
                val matchesDate = when (filters.datePreset) {
                    DateFilterPreset.CUSTOM -> {
                        val start = filters.customStartDate
                        val end = if (filters.customEndDate > 0L) filters.customEndDate else Long.MAX_VALUE
                        tx.transactionDate in start..end
                    }
                    else -> tx.transactionDate >= minTimestamp
                }
                val matchesType = filters.entryType == null || tx.type == filters.entryType
                val matchesMode = filters.paymentMode == null || tx.paymentMode == filters.paymentMode
                val matchesCategory = filters.category == null || tx.category == filters.category
                val matchesParty = filters.partyId == null || tx.partyId == filters.partyId
                val q = filters.searchQuery.trim().lowercase()
                val shortDateStr = StatementExportHelper.formatShortDate(tx.transactionDate).lowercase()
                val formattedAmtStr = StatementExportHelper.formatCurrency(tx.amount, "").lowercase()
                val matchesSearch = q.isEmpty() ||
                    tx.remark.lowercase().contains(q) ||
                    tx.partyName.lowercase().contains(q) ||
                    tx.id.lowercase().contains(q) ||
                    tx.category.lowercase().contains(q) ||
                    tx.paymentMode.lowercase().contains(q) ||
                    tx.amount.toString().contains(q) ||
                    formattedAmtStr.contains(q) ||
                    shortDateStr.contains(q)

                matchesDate && matchesType && matchesMode && matchesCategory && matchesParty && matchesSearch
            }
            .reversed() // Display newest vouchers at top of register, preserving chronological running balance

        var filteredInPaise = 0L
        var filteredOutPaise = 0L
        filteredRowsDesc.forEach { item ->
            val paise = FinancialCalculationEngine.toPaise(item.transaction.amount)
            if (item.transaction.isCashIn) {
                filteredInPaise += paise
            } else {
                filteredOutPaise += paise
            }
        }

        val filteredSummary = BookSummary(
            bookId = activeBookId,
            bookName = activeBook?.name ?: "Cash Book",
            currencySymbol = currency,
            totalCashIn = FinancialCalculationEngine.fromPaise(filteredInPaise),
            totalCashOut = FinancialCalculationEngine.fromPaise(filteredOutPaise),
            netBalance = FinancialCalculationEngine.fromPaise(filteredInPaise - filteredOutPaise),
            totalEntries = filteredRowsDesc.size
        )

        // Compute Party Khata Summaries across all books using exact minor-unit arithmetic
        var totalReceivablePaise = 0L
        var totalPayablePaise = 0L
        val partySummaries = parties.map { party ->
            val partyTxs = allTransactions.filter { it.partyId == party.id }
            var receivedFromPartyPaise = 0L
            var paidToPartyPaise = 0L
            var lastDate = 0L
            for (tx in partyTxs) {
                val p = FinancialCalculationEngine.toPaise(tx.amount)
                if (tx.isCashIn) {
                    receivedFromPartyPaise += p
                } else {
                    paidToPartyPaise += p
                }
                if (tx.transactionDate > lastDate) {
                    lastDate = tx.transactionDate
                }
            }
            val netPaise = paidToPartyPaise - receivedFromPartyPaise
            if (netPaise > 0L) {
                totalReceivablePaise += netPaise
            } else if (netPaise < 0L) {
                totalPayablePaise += kotlin.math.abs(netPaise)
            }
            PartyBalanceSummary(
                party = party,
                totalCashIn = FinancialCalculationEngine.fromPaise(receivedFromPartyPaise),
                totalCashOut = FinancialCalculationEngine.fromPaise(paidToPartyPaise),
                netBalance = FinancialCalculationEngine.fromPaise(netPaise),
                transactionCount = partyTxs.size,
                lastTransactionDate = lastDate
            )
        }

        CashBookUiState(
            isLoading = false,
            cashBooks = books,
            activeBook = activeBook,
            activeBookSummary = fullBookSummary,
            filteredBookSummary = filteredSummary,
            transactionsWithBalance = filteredRowsDesc,
            allTransactions = allTransactions,
            allBookTransactionsCount = chronologicalWithBal.size,
            parties = parties,
            partySummaries = partySummaries,
            totalReceivable = FinancialCalculationEngine.fromPaise(totalReceivablePaise),
            totalPayable = FinancialCalculationEngine.fromPaise(totalPayablePaise),
            filterState = filters,
            businessProfile = bizProfile,
            errorMessage = messages.first,
            actionBannerMessage = messages.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CashBookUiState(isLoading = true)
    )

    fun selectCashBook(bookId: String) {
        _selectedBookId.value = bookId
    }

    fun updateBusinessProfile(profile: BusinessProfile) {
        if (profile.businessName.isBlank()) {
            _transientMessage.value = "Business name cannot be empty." to null
            return
        }
        _businessProfile.value = profile
        _transientMessage.value = null to "Business profile updated for printed statements & vouchers"
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun updateEntryTypeFilter(type: String?) {
        _filterState.update { it.copy(entryType = type) }
    }

    fun updatePaymentModeFilter(mode: String?) {
        _filterState.update { it.copy(paymentMode = mode) }
    }

    fun updateCategoryFilter(category: String?) {
        _filterState.update { it.copy(category = category) }
    }

    fun updatePartyFilter(partyId: String?) {
        _filterState.update { it.copy(partyId = partyId) }
    }

    fun updateDatePreset(preset: DateFilterPreset) {
        _filterState.update { it.copy(datePreset = preset) }
    }

    fun updateCustomDateRange(startDateMillis: Long, endDateMillis: Long) {
        _filterState.update {
            it.copy(
                datePreset = DateFilterPreset.CUSTOM,
                customStartDate = startDateMillis,
                customEndDate = endDateMillis
            )
        }
    }

    fun clearAllFilters() {
        _filterState.value = LedgerFilterState()
    }

    fun clearMessages() {
        _transientMessage.value = null to null
    }

    fun createCashBook(name: String, description: String, currencySymbol: String) {
        if (name.isBlank()) {
            _transientMessage.value = "Book name cannot be empty." to null
            return
        }
        viewModelScope.launch {
            val result = repository.createCashBook(name, description, currencySymbol)
            result.onSuccess { newBookId ->
                _selectedBookId.value = newBookId
                _transientMessage.value = null to "Created cash book '${name.trim()}'"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to create book") to null
            }
        }
    }

    fun updateCashBook(book: CashBook, newName: String, newDescription: String, newCurrency: String) {
        if (newName.isBlank()) {
            _transientMessage.value = "Book name cannot be empty." to null
            return
        }
        viewModelScope.launch {
            val result = repository.updateCashBook(
                book.copy(
                    name = newName,
                    description = newDescription,
                    currencySymbol = newCurrency
                )
            )
            result.onSuccess {
                _transientMessage.value = null to "Updated book '${newName.trim()}'"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to update book") to null
            }
        }
    }

    fun deleteCashBook(bookId: String) {
        viewModelScope.launch {
            val result = repository.deleteCashBook(bookId)
            result.onSuccess {
                if (_selectedBookId.value == bookId) {
                    _selectedBookId.value = null
                }
                _transientMessage.value = null to "Cash book deleted"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to delete book") to null
            }
        }
    }

    fun addTransaction(
        type: String,
        amount: Double,
        remark: String,
        category: String,
        paymentMode: String,
        partyId: String = "",
        partyName: String = "",
        transactionDate: Long = System.currentTimeMillis()
    ) {
        val activeBookId = uiState.value.activeBook?.id
        if (activeBookId.isNullOrBlank()) {
            _transientMessage.value = "Please create or select a Cash Book first." to null
            return
        }
        if (amount <= 0.0) {
            _transientMessage.value = "Please enter a valid amount greater than 0." to null
            return
        }
        viewModelScope.launch {
            val result = repository.createTransaction(
                bookId = activeBookId,
                type = type,
                amount = amount,
                remark = remark,
                category = category,
                paymentMode = paymentMode,
                partyId = partyId,
                partyName = partyName,
                transactionDate = transactionDate
            )
            result.onSuccess {
                val label = if (type == CashTransaction.TYPE_CASH_IN) "Cash In" else "Cash Out"
                _transientMessage.value = null to "$label voucher saved"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to save voucher") to null
            }
        }
    }

    /**
     * Posts a formal Contra Reversal Entry to reverse an existing posted voucher while preserving
     * full chronological audit history.
     */
    fun reverseTransaction(originalTx: CashTransaction) {
        val reversedType = if (originalTx.isCashIn) {
            CashTransaction.TYPE_CASH_OUT
        } else {
            CashTransaction.TYPE_CASH_IN
        }
        val refCode = originalTx.id.take(6).uppercase()
        val reversalRemark = "[REVERSAL of VCH-$refCode] ${originalTx.remark}".take(450)
        viewModelScope.launch {
            val result = repository.createTransaction(
                bookId = originalTx.bookId,
                type = reversedType,
                amount = originalTx.amount,
                remark = reversalRemark,
                category = originalTx.category,
                paymentMode = originalTx.paymentMode,
                partyId = originalTx.partyId,
                partyName = originalTx.partyName,
                transactionDate = System.currentTimeMillis()
            )
            result.onSuccess {
                _transientMessage.value = null to "Reversal voucher posted for VCH-$refCode"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to post reversal entry") to null
            }
        }
    }

    /**
     * Records an Account-to-Account Contra Transfer (e.g. Cash Deposit into Bank or Bank Withdrawal to Cash)
     * across two registers without inflating operating income or operating expense totals.
     */
    fun transferBetweenBooks(
        fromBookId: String,
        toBookId: String,
        amount: Double,
        paymentMode: String,
        remark: String,
        transactionDate: Long = System.currentTimeMillis()
    ) {
        if (fromBookId.isBlank() || toBookId.isBlank() || fromBookId == toBookId) {
            _transientMessage.value = "Select two distinct Cash/Bank Books for Contra Transfer." to null
            return
        }
        if (amount <= 0.0) {
            _transientMessage.value = "Transfer amount must be greater than 0." to null
            return
        }
        val books = uiState.value.cashBooks
        val fromBookName = books.find { it.id == fromBookId }?.name ?: "Source Book"
        val toBookName = books.find { it.id == toBookId }?.name ?: "Target Book"
        val cleanNote = remark.trim().ifEmpty { "Contra fund transfer" }

        viewModelScope.launch {
            val outResult = repository.createTransaction(
                bookId = fromBookId,
                type = CashTransaction.TYPE_CASH_OUT,
                amount = amount,
                remark = "[CONTRA TO $toBookName] $cleanNote",
                category = "Account Transfer (Contra)",
                paymentMode = paymentMode,
                transactionDate = transactionDate
            )
            val inResult = repository.createTransaction(
                bookId = toBookId,
                type = CashTransaction.TYPE_CASH_IN,
                amount = amount,
                remark = "[CONTRA FROM $fromBookName] $cleanNote",
                category = "Account Transfer (Contra)",
                paymentMode = paymentMode,
                transactionDate = transactionDate
            )
            if (outResult.isSuccess && inResult.isSuccess) {
                _transientMessage.value = null to "Contra transfer posted: $fromBookName → $toBookName"
            } else {
                val err = outResult.exceptionOrNull() ?: inResult.exceptionOrNull()
                _transientMessage.value = (err?.localizedMessage ?: "Contra transfer failed") to null
            }
        }
    }

    private fun isProtectedByBackDateLock(transactionDateMillis: Long): Boolean {
        val profile = _businessProfile.value
        if (!profile.backDateLockEnabled || transactionDateMillis <= 0L) return false
        val cutoffMillis = System.currentTimeMillis() - (profile.backDateLockDays.toLong() * 86_400_000L)
        return transactionDateMillis < cutoffMillis
    }

    fun updateTransaction(updatedTx: CashTransaction) {
        if (updatedTx.amount <= 0.0) {
            _transientMessage.value = "Amount must be greater than 0." to null
            return
        }
        if (isProtectedByBackDateLock(updatedTx.transactionDate)) {
            _transientMessage.value = "Back-Date Lock active: Entries older than ${_businessProfile.value.backDateLockDays} days cannot be modified." to null
            return
        }
        viewModelScope.launch {
            val result = repository.updateTransaction(updatedTx)
            result.onSuccess {
                _transientMessage.value = null to "Voucher updated"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to update voucher") to null
            }
        }
    }

    fun deleteTransaction(txId: String) {
        val existingTx = uiState.value.allTransactions.find { it.id == txId }
        if (existingTx != null && isProtectedByBackDateLock(existingTx.transactionDate)) {
            _transientMessage.value = "Back-Date Lock active: Entries older than ${_businessProfile.value.backDateLockDays} days cannot be deleted." to null
            return
        }
        viewModelScope.launch {
            val result = repository.deleteTransaction(txId)
            result.onSuccess {
                _transientMessage.value = null to "Voucher deleted"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to delete voucher") to null
            }
        }
    }

    fun createParty(name: String, phone: String, role: String, notes: String) {
        if (name.isBlank()) {
            _transientMessage.value = "Party name cannot be empty." to null
            return
        }
        viewModelScope.launch {
            val result = repository.createParty(name, phone, role, notes)
            result.onSuccess {
                _transientMessage.value = null to "Party '${name.trim()}' added to ledger"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to add party") to null
            }
        }
    }

    fun updateParty(party: Party, name: String, phone: String, role: String, notes: String) {
        if (name.isBlank()) {
            _transientMessage.value = "Party name cannot be empty." to null
            return
        }
        viewModelScope.launch {
            val result = repository.updateParty(
                party.copy(name = name, phone = phone, role = role, notes = notes)
            )
            result.onSuccess {
                _transientMessage.value = null to "Party '${name.trim()}' updated"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to update party") to null
            }
        }
    }

    fun deleteParty(partyId: String) {
        viewModelScope.launch {
            val result = repository.deleteParty(partyId)
            result.onSuccess {
                if (_filterState.value.partyId == partyId) {
                    _filterState.update { it.copy(partyId = null) }
                }
                _transientMessage.value = null to "Party removed"
            }.onFailure { err ->
                _transientMessage.value = (err.localizedMessage ?: "Failed to delete party") to null
            }
        }
    }

    fun restoreFromJson(jsonString: String) {
        viewModelScope.launch {
            val result = StatementExportHelper.importBackupJson(jsonString, repository)
            result.onSuccess { stats ->
                _transientMessage.value = null to "Restored ${stats.booksCount} registers, ${stats.partiesCount} parties, ${stats.vouchersCount} vouchers"
            }.onFailure { err ->
                _transientMessage.value = ("Import failed: ${err.localizedMessage ?: "Invalid JSON backup"}") to null
            }
        }
    }

    /**
     * Populates realistic commercial business vouchers (including a Bank Register & Contra Transfer)
     * so the user can immediately test multi-book switching, running balances, party reconciliation,
     * PDF/CSV export, and financial audit.
     */
    fun seedSampleBusinessEntries() {
        viewModelScope.launch {
            val state = uiState.value
            val cashBookId = state.activeBook?.id ?: repository.createCashBook(
                name = "Main Office Cash Book",
                description = "Daily business cash & bank register",
                currencySymbol = "₹"
            ).getOrNull() ?: return@launch

            val bankBookId = state.cashBooks.find {
                it.id != cashBookId && it.name.contains("Bank", ignoreCase = true)
            }?.id ?: repository.createCashBook(
                name = "SBI Current A/c #4092",
                description = "Corporate Bank & NEFT/RTGS Register",
                currencySymbol = "₹"
            ).getOrNull()

            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            val p1 = repository.createParty(
                name = "Bharat Logistics & Freight",
                phone = "9820114455",
                role = "Customer",
                notes = "Monthly contract billing"
            ).getOrDefault("")

            val p2 = repository.createParty(
                name = "Highway Diesel Point",
                phone = "9811223344",
                role = "Supplier",
                notes = "Fleet fuel account"
            ).getOrDefault("")

            val p3 = repository.createParty(
                name = "Ramesh Kumar (Driver)",
                phone = "9711002299",
                role = "Staff",
                notes = "Route bhatta & salary"
            ).getOrDefault("")

            repository.createTransaction(
                bookId = cashBookId,
                type = CashTransaction.TYPE_CASH_IN,
                amount = 150000.0,
                remark = "Opening Working Capital Deposit",
                category = "Capital / Opening",
                paymentMode = "Cash",
                transactionDate = now - 5 * dayMs
            )

            repository.createTransaction(
                bookId = cashBookId,
                type = CashTransaction.TYPE_CASH_IN,
                amount = 68500.0,
                remark = "Invoice #BL-208 Ahmedabad-Mumbai Consignment",
                category = "Freight / Trip Receipt",
                paymentMode = "UPI",
                partyId = p1,
                partyName = "Bharat Logistics & Freight",
                transactionDate = now - 4 * dayMs
            )

            repository.createTransaction(
                bookId = cashBookId,
                type = CashTransaction.TYPE_CASH_OUT,
                amount = 24200.0,
                remark = "HSD Diesel 260L - Vehicle GJ-01-XX-4412",
                category = "Fuel & Diesel",
                paymentMode = "UPI",
                partyId = p2,
                partyName = "Highway Diesel Point",
                transactionDate = now - 3 * dayMs
            )

            repository.createTransaction(
                bookId = cashBookId,
                type = CashTransaction.TYPE_CASH_OUT,
                amount = 8500.0,
                remark = "Trip Advance & Route Bhatta",
                category = "Salary & Bhatta",
                paymentMode = "Cash",
                partyId = p3,
                partyName = "Ramesh Kumar (Driver)",
                transactionDate = now - 2 * dayMs
            )

            if (!bankBookId.isNullOrBlank()) {
                repository.createTransaction(
                    bookId = cashBookId,
                    type = CashTransaction.TYPE_CASH_OUT,
                    amount = 25000.0,
                    remark = "[CONTRA TO SBI Current A/c #4092] Cash deposited in bank branch",
                    category = "Account Transfer (Contra)",
                    paymentMode = "Bank Transfer",
                    transactionDate = now - 36 * 3600_000L
                )
                repository.createTransaction(
                    bookId = bankBookId,
                    type = CashTransaction.TYPE_CASH_IN,
                    amount = 25000.0,
                    remark = "[CONTRA FROM Main Office Cash Book] Cash deposited in bank branch",
                    category = "Account Transfer (Contra)",
                    paymentMode = "Bank Transfer",
                    transactionDate = now - 36 * 3600_000L
                )
            }

            repository.createTransaction(
                bookId = cashBookId,
                type = CashTransaction.TYPE_CASH_OUT,
                amount = 4650.0,
                remark = "NH-48 Fastag Recharge & Weighbridge Slip",
                category = "Toll & Fastag",
                paymentMode = "Card",
                transactionDate = now - 1 * dayMs
            )

            repository.createTransaction(
                bookId = cashBookId,
                type = CashTransaction.TYPE_CASH_IN,
                amount = 42000.0,
                remark = "Part Settlement Against Consignment #BL-214",
                category = "Sales",
                paymentMode = "Bank Transfer",
                partyId = p1,
                partyName = "Bharat Logistics & Freight",
                transactionDate = now - 3600_000L
            )

            _transientMessage.value = null to "Sample commercial vouchers & contra bank register loaded"
        }
    }

    private fun computeMinTimestamp(preset: DateFilterPreset): Long {
        if (preset == DateFilterPreset.ALL_TIME || preset == DateFilterPreset.CUSTOM) return 0L
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return when (preset) {
            DateFilterPreset.ALL_TIME, DateFilterPreset.CUSTOM -> 0L
            DateFilterPreset.TODAY -> cal.timeInMillis
            DateFilterPreset.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            DateFilterPreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "CashBookViewModel"
    }
}
