package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Canonical Firestore collection names and scoped document path builders for
 * persistent cloud-synced CashBook, Party, and Transaction ledger records.
 */
object FirestoreCollections {
    const val USERS = "users"
    const val CASHBOOKS = "cashbooks"
    const val PARTIES = "parties"
    const val TRANSACTIONS = "transactions"

    fun cashBooksPath(userId: String): String = "$USERS/$userId/$CASHBOOKS"
    fun partiesPath(userId: String): String = "$USERS/$userId/$PARTIES"
    fun transactionsPath(userId: String): String = "$USERS/$userId/$TRANSACTIONS"
}

@IgnoreExtraProperties
data class CashBook(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val currencySymbol: String = "₹",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Firestore data model for `/users/{userId}/parties/{partyId}`.
 * Stores Customer, Supplier, and Staff khata accounts with server timestamps.
 */
@IgnoreExtraProperties
data class Party(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = ROLE_CUSTOMER, // "Customer", "Supplier", "Staff"
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isCustomer: Boolean
        get() = role.equals(ROLE_CUSTOMER, ignoreCase = true)

    @get:Exclude
    val isSupplier: Boolean
        get() = role.equals(ROLE_SUPPLIER, ignoreCase = true)

    @get:Exclude
    val isStaff: Boolean
        get() = role.equals(ROLE_STAFF, ignoreCase = true)

    @get:Exclude
    val initials: String
        get() = name.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "P" }

    companion object {
        const val ROLE_CUSTOMER = "Customer"
        const val ROLE_SUPPLIER = "Supplier"
        const val ROLE_STAFF = "Staff"
        val VALID_ROLES = listOf(ROLE_CUSTOMER, ROLE_SUPPLIER, ROLE_STAFF)
    }
}

/**
 * Firestore data model for `/users/{userId}/transactions/{txId}`.
 * Stores persistent cloud-synced Cash In and Cash Out ledger vouchers linked to a CashBook and optional Party.
 */
@IgnoreExtraProperties
data class CashTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val type: String = TYPE_CASH_IN, // "CASH_IN" or "CASH_OUT"
    val amount: Double = 0.0,
    val remark: String = "",
    val category: String = "Sales",
    val paymentMode: String = "Cash", // "Cash", "Bank Transfer", "UPI", "Cheque", "Card"
    val partyId: String = "",
    val partyName: String = "",
    val transactionDate: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val voucherNumber: String
        get() = if (id.isNotBlank()) "VCH-${id.take(6).uppercase()}" else "VCH-NEW"

    @get:Exclude
    val isLinkedToParty: Boolean
        get() = partyId.isNotBlank() || partyName.isNotBlank()

    @get:Exclude
    val signedAmount: Double
        get() = if (type == TYPE_CASH_IN) amount else -amount

    companion object {
        const val TYPE_CASH_IN = "CASH_IN"
        const val TYPE_CASH_OUT = "CASH_OUT"

        val PAYMENT_MODES = listOf("Cash", "Bank Transfer", "UPI", "Cheque", "Card")

        val CATEGORIES_CASH_IN = listOf(
            "Sales",
            "Freight / Trip Receipt",
            "Customer Advance",
            "Party Recovery",
            "Commission",
            "Capital / Opening",
            "Account Transfer (Contra)",
            "Other Receipt"
        )

        val CATEGORIES_CASH_OUT = listOf(
            "Petty Cash (छोटे खर्चे)",
            "Fuel & Diesel",
            "Supplier Payment",
            "Salary & Bhatta",
            "Vehicle Maintenance",
            "Toll & Fastag",
            "Office & Rent",
            "Loading / Unloading",
            "Account Transfer (Contra)",
            "Other Expense"
        )

        val PETTY_EXPENSE_TEMPLATES = listOf(
            Triple("☕ चाय-नाश्ता (Tea)", "चाय-नाश्ता / Tea & Snacks", 30),
            Triple("🅿️ पार्किंग/ऑटो", "पार्किंग / ऑटो भाड़ा (Parking/Auto)", 50),
            Triple("📦 हम्माली (Labour)", "फुटकर हम्माली / मजदूरी (Petty Labour)", 100),
            Triple("📄 ज़ेरॉक्स/स्टेशनरी", "फोटोकॉपी / स्टेशनरी (Stationery)", 20),
            Triple("💧 पानी/सफाई", "पानी बोतल / ऑफिस सफाई (Water/Cleaning)", 40),
            Triple("🛠️ फुटकर रिपेयर", "पंचर / फुटकर रिपेयर (Petty Repair)", 100)
        )

        val QUICK_AMOUNT_STEPS = listOf(10, 20, 50, 100, 200, 500)

        val ALL_CATEGORIES = (CATEGORIES_CASH_IN + CATEGORIES_CASH_OUT).distinct()

        val PARTY_ROLES = Party.VALID_ROLES
    }
}

typealias Transaction = CashTransaction

val CashTransaction.isCashIn: Boolean
    get() = type == CashTransaction.TYPE_CASH_IN

data class TransactionWithBalance(
    val transaction: CashTransaction,
    val runningBalance: Double
)

data class BookSummary(
    val bookId: String = "",
    val bookName: String = "",
    val currencySymbol: String = "₹",
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalEntries: Int = 0
)

data class PartyBalanceSummary(
    val party: Party,
    val totalCashIn: Double = 0.0, // Received from Party
    val totalCashOut: Double = 0.0, // Paid to Party
    val netBalance: Double = 0.0, // Positive = Paid Out > Received (Receivable / Advance given), Negative = Received > Paid Out (Payable / Credit)
    val transactionCount: Int = 0,
    val lastTransactionDate: Long = 0L
)

enum class DateFilterPreset(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Range")
}

data class LedgerFilterState(
    val searchQuery: String = "",
    val entryType: String? = null, // null = All, "CASH_IN", "CASH_OUT"
    val paymentMode: String? = null,
    val category: String? = null,
    val partyId: String? = null,
    val datePreset: DateFilterPreset = DateFilterPreset.ALL_TIME,
    val customStartDate: Long = 0L,
    val customEndDate: Long = 0L
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            entryType != null ||
            paymentMode != null ||
            category != null ||
            partyId != null ||
            datePreset != DateFilterPreset.ALL_TIME
}

data class BusinessProfile(
    val businessName: String = "Shivay Transport & Logistics",
    val ownerName: String = "Shivay Owner",
    val operatorUsername: String = "shivay_admin",
    val operatorPassword: String = "",
    val profilePhotoPath: String = "",
    val phone: String = "+91 98201 14455",
    val email: String = "shivaytransport25@gmail.com",
    val address: String = "Plot 42, Transport Nagar, Mumbai - 400001",
    val gstin: String = "27AABCS1429B1Z1",
    val pan: String = "AABCS1429B",
    val currencySymbol: String = "₹",
    val financialYear: String = "2026-2027",
    val appLockEnabled: Boolean = false,
    val hideBalanceOnHome: Boolean = false,
    val backDateLockEnabled: Boolean = false,
    val backDateLockDays: Int = 7,
    val defaultPaymentMode: String = "Cash",
    val voucherPrefix: String = "VCH",
    val pdfSignatoryName: String = "Authorized Signatory",
    val pdfFooterNote: String = "भूल-चूक लेनी देनी • Subject to Mumbai Jurisdiction",
    val pdfShowRunningBalance: Boolean = true,
    val pdfShowCategoryAndMode: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: String = "08:00 PM",
    val whatsappReminderTemplate: String = "नमस्ते {party}, आपका {business} में {amount} बकाया है। कृपया जल्द भुगतान करें। धन्यवाद!",
    val appLanguage: String = "Hinglish (हिंदी + English)",
    val customCashInCategories: List<String> = emptyList(),
    val customCashOutCategories: List<String> = emptyList()
)

