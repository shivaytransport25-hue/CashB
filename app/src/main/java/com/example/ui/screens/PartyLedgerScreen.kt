package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashTransaction
import com.example.data.model.DateFilterPreset
import com.example.data.model.Party
import com.example.data.model.PartyBalanceSummary
import com.example.data.model.TransactionWithBalance
import com.example.data.model.isCashIn
import com.example.ui.components.LedgerSelectableChip
import com.example.ui.components.bouncingClickable
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.FinancialCalculationEngine
import com.example.util.StatementExportHelper
import com.example.viewmodel.CashBookUiState
import java.util.Calendar

private enum class PartyBalanceFilter(val label: String) {
    ALL("All Balances"),
    RECEIVABLE("You'll Get (Dr)"),
    PAYABLE("You'll Give (Cr)"),
    SETTLED("Settled (₹0)")
}

private fun computePartyPresetMinTimestamp(preset: DateFilterPreset): Long {
    if (preset == DateFilterPreset.ALL_TIME || preset == DateFilterPreset.CUSTOM) return 0L
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    when (preset) {
        DateFilterPreset.TODAY -> Unit
        DateFilterPreset.THIS_WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
        DateFilterPreset.THIS_MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
        else -> return 0L
    }
    return cal.timeInMillis
}

@Composable
fun PartyLedgerScreen(
    uiState: CashBookUiState,
    onAddPartyClick: () -> Unit,
    onEditPartyClick: (Party) -> Unit,
    onDeletePartyClick: (String) -> Unit,
    onViewPartyTransactions: (Party) -> Unit,
    onSettlePartyClick: (party: Party, defaultType: String, defaultAmount: Double) -> Unit,
    onEditTransactionClick: (CashTransaction) -> Unit = {},
    onDeleteTransactionClick: (CashTransaction) -> Unit = {},
    onExportPartyStatement: (Party) -> Unit = {},
    onSharePartyReminder: (Party, Double) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<String?>(null) }
    var selectedBalanceFilter by remember { mutableStateOf(PartyBalanceFilter.ALL) }
    var selectedDatePreset by remember { mutableStateOf(DateFilterPreset.ALL_TIME) }
    var showPartyFiltersDrawer by remember { mutableStateOf(false) }
    var selectedPassbookParty by remember { mutableStateOf<PartyBalanceSummary?>(null) }

    val currency = uiState.activeBookSummary.currencySymbol
    val minActivityTime = remember(selectedDatePreset) {
        computePartyPresetMinTimestamp(selectedDatePreset)
    }

    val filteredParties = remember(
        uiState.partySummaries,
        uiState.allTransactions,
        searchQuery,
        selectedRoleFilter,
        selectedBalanceFilter,
        selectedDatePreset,
        minActivityTime
    ) {
        uiState.partySummaries.filter { item ->
            val party = item.party
            val partyTxs = uiState.allTransactions.filter {
                it.partyId == party.id ||
                    (it.partyId.isBlank() && it.partyName.equals(party.name, ignoreCase = true))
            }

            // Role filter
            val matchesRole = selectedRoleFilter == null || party.role == selectedRoleFilter

            // Balance status filter
            val matchesBalance = when (selectedBalanceFilter) {
                PartyBalanceFilter.ALL -> true
                PartyBalanceFilter.RECEIVABLE -> item.netBalance > 0.0
                PartyBalanceFilter.PAYABLE -> item.netBalance < 0.0
                PartyBalanceFilter.SETTLED -> item.netBalance == 0.0
            }

            // Activity date filter
            val matchesDatePreset = selectedDatePreset == DateFilterPreset.ALL_TIME ||
                partyTxs.any { it.transactionDate >= minActivityTime }

            // Search by Party Name, Phone, Role, Notes, OR Transaction Date (DD-MM-YYYY), Voucher #, or Remark
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                party.name.lowercase().contains(q) ||
                party.phone.lowercase().contains(q) ||
                party.role.lowercase().contains(q) ||
                party.notes.lowercase().contains(q) ||
                partyTxs.any { tx ->
                    StatementExportHelper.formatShortDate(tx.transactionDate).lowercase().contains(q) ||
                        tx.id.lowercase().contains(q) ||
                        tx.remark.lowercase().contains(q) ||
                        tx.amount.toString().contains(q)
                }

            matchesRole && matchesBalance && matchesDatePreset && matchesQuery
        }
    }

    val activeFilterCount = (if (selectedRoleFilter != null) 1 else 0) +
        (if (selectedBalanceFilter != PartyBalanceFilter.ALL) 1 else 0) +
        (if (selectedDatePreset != DateFilterPreset.ALL_TIME) 1 else 0)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("party_ledger_list"),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Minimalist Party Khata Summary Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PARTY KHATA SUMMARY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${filteredParties.size} / ${uiState.parties.size} Accounts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                color = CashInGreen.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, CashInGreen.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "YOU'LL GET (Dr)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CashInGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = StatementExportHelper.formatCurrency(
                                            uiState.totalReceivable,
                                            currency
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CashInGreen
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                color = CashOutRed.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, CashOutRed.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "YOU'LL GIVE (Cr)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CashOutRed
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = StatementExportHelper.formatCurrency(
                                            uiState.totalPayable,
                                            currency
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CashOutRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Search Bar + Progressive Disclosure Filter Drawer + Add Party Button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search party, phone, or date...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_parties_input")
                        )

                        OutlinedButton(
                            onClick = { showPartyFiltersDrawer = !showPartyFiltersDrawer },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text(
                                text = if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = onAddPartyClick,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .bouncingClickable(onClick = onAddPartyClick)
                                .testTag("add_party_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Party")
                        }
                    }

                    if (showPartyFiltersDrawer || activeFilterCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        // Row 1: Role + Balance Filters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LedgerSelectableChip(
                                label = "All Roles",
                                selected = selectedRoleFilter == null,
                                onClick = { selectedRoleFilter = null }
                            )
                            CashTransaction.PARTY_ROLES.forEach { role ->
                                LedgerSelectableChip(
                                    label = role,
                                    selected = selectedRoleFilter == role,
                                    onClick = { selectedRoleFilter = role }
                                )
                            }
                            PartyBalanceFilter.entries.forEach { balFilter ->
                                if (balFilter != PartyBalanceFilter.ALL) {
                                    LedgerSelectableChip(
                                        label = balFilter.label,
                                        selected = selectedBalanceFilter == balFilter,
                                        onClick = {
                                            selectedBalanceFilter = if (selectedBalanceFilter == balFilter) {
                                                PartyBalanceFilter.ALL
                                            } else {
                                                balFilter
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Activity Date Filters + Reset Filters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            listOf(
                                DateFilterPreset.ALL_TIME,
                                DateFilterPreset.TODAY,
                                DateFilterPreset.THIS_WEEK,
                                DateFilterPreset.THIS_MONTH
                            ).forEach { preset ->
                                LedgerSelectableChip(
                                    label = if (preset == DateFilterPreset.ALL_TIME) "All Dates" else "Active: ${preset.label}",
                                    selected = selectedDatePreset == preset,
                                    onClick = { selectedDatePreset = preset }
                                )
                            }

                            if (searchQuery.isNotBlank() || activeFilterCount > 0) {
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedRoleFilter = null
                                        selectedBalanceFilter = PartyBalanceFilter.ALL
                                        selectedDatePreset = DateFilterPreset.ALL_TIME
                                    }
                                ) {
                                    Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 3. Column Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PARTY ACCOUNT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "NET KHATA POSITION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredParties.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "NO PARTY ACCOUNTS MATCH FILTERS",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add customers, suppliers, or staff to track party-wise receivables and payables, or clear active search filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onAddPartyClick,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Add Party Account")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredParties,
                    key = { it.party.id }
                ) { summaryItem ->
                    PartyKhataRow(
                        summary = summaryItem,
                        currencySymbol = currency,
                        onOpenPassbook = { selectedPassbookParty = summaryItem },
                        onCashInClick = {
                            onSettlePartyClick(summaryItem.party, CashTransaction.TYPE_CASH_IN, 0.0)
                        },
                        onCashOutClick = {
                            onSettlePartyClick(summaryItem.party, CashTransaction.TYPE_CASH_OUT, 0.0)
                        },
                        onSettleClick = {
                            val absAmt = kotlin.math.abs(summaryItem.netBalance)
                            val settleType = if (summaryItem.netBalance >= 0.0) {
                                CashTransaction.TYPE_CASH_IN
                            } else {
                                CashTransaction.TYPE_CASH_OUT
                            }
                            onSettlePartyClick(summaryItem.party, settleType, absAmt)
                        },
                        onExportStatement = { onExportPartyStatement(summaryItem.party) },
                        onShareReminder = { onSharePartyReminder(summaryItem.party, summaryItem.netBalance) },
                        onEdit = { onEditPartyClick(summaryItem.party) },
                        onDelete = { onDeletePartyClick(summaryItem.party.id) }
                    )
                }
            }
        }
    }

    // Interactive Party Passbook & Transaction History Modal Sheet
    val passbookTarget = selectedPassbookParty
    if (passbookTarget != null) {
        val currentSummary = uiState.partySummaries.firstOrNull { it.party.id == passbookTarget.party.id }
            ?: passbookTarget
        val partyTransactions = remember(uiState.allTransactions, currentSummary.party.id) {
            uiState.allTransactions.filter {
                it.partyId == currentSummary.party.id ||
                    (it.partyId.isBlank() && it.partyName.equals(currentSummary.party.name, ignoreCase = true))
            }
        }
        PartyPassbookModalSheet(
            summary = currentSummary,
            partyTransactions = partyTransactions,
            currencySymbol = currency,
            onDismiss = { selectedPassbookParty = null },
            onCashInClick = {
                selectedPassbookParty = null
                onSettlePartyClick(currentSummary.party, CashTransaction.TYPE_CASH_IN, 0.0)
            },
            onCashOutClick = {
                selectedPassbookParty = null
                onSettlePartyClick(currentSummary.party, CashTransaction.TYPE_CASH_OUT, 0.0)
            },
            onEditTransactionClick = { tx ->
                selectedPassbookParty = null
                onEditTransactionClick(tx)
            },
            onDeleteTransactionClick = { tx ->
                onDeleteTransactionClick(tx)
            },
            onExportPdfClick = { onExportPartyStatement(currentSummary.party) },
            onOpenInMainRegister = {
                selectedPassbookParty = null
                onViewPartyTransactions(currentSummary.party)
            }
        )
    }
}

@Composable
private fun PartyKhataRow(
    summary: PartyBalanceSummary,
    currencySymbol: String,
    onOpenPassbook: () -> Unit,
    onCashInClick: () -> Unit,
    onCashOutClick: () -> Unit,
    onSettleClick: () -> Unit,
    onExportStatement: () -> Unit,
    onShareReminder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val party = summary.party
    val net = summary.netBalance
    val statusLabel = when {
        net > 0.0 -> "YOU'LL GET (Dr)"
        net < 0.0 -> "YOU'LL GIVE (Cr)"
        else -> "SETTLED"
    }
    val statusColor = when {
        net > 0.0 -> CashInGreen
        net < 0.0 -> CashOutRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onOpenPassbook)
            .testTag("party_row_${party.id}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = party.role.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (party.phone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = party.phone,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "${summary.transactionCount} Vouchers",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (party.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = party.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = StatementExportHelper.formatCurrency(
                            kotlin.math.abs(net),
                            currencySymbol
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action strip per party row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onCashInClick,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CashInGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .bouncingClickable(onClick = onCashInClick)
                            .testTag("party_cash_in_${party.id}")
                    ) {
                        Text(
                            text = "+ Cash In",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = onCashOutClick,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CashOutRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .bouncingClickable(onClick = onCashOutClick)
                            .testTag("party_cash_out_${party.id}")
                    ) {
                        Text(
                            text = "- Cash Out",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (net != 0.0) {
                        Button(
                            onClick = onSettleClick,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .bouncingClickable(onClick = onSettleClick)
                                .testTag("settle_party_${party.id}")
                        ) {
                            Text(
                                text = "Settle",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenPassbook,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Passbook (${summary.transactionCount})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedButton(
                        onClick = onExportStatement,
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CashOutRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (net != 0.0) {
                        OutlinedButton(
                            onClick = onShareReminder,
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reminder",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${party.name}",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${party.name}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartyPassbookModalSheet(
    summary: PartyBalanceSummary,
    partyTransactions: List<CashTransaction>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onCashInClick: () -> Unit,
    onCashOutClick: () -> Unit,
    onEditTransactionClick: (CashTransaction) -> Unit,
    onDeleteTransactionClick: (CashTransaction) -> Unit,
    onExportPdfClick: () -> Unit,
    onOpenInMainRegister: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txSearchQuery by remember { mutableStateOf("") }
    var txDatePreset by remember { mutableStateOf(DateFilterPreset.ALL_TIME) }
    var txTypeFilter by remember { mutableStateOf<String?>(null) }

    val chronologicalRows = remember(partyTransactions) {
        val sortedAsc = partyTransactions.sortedWith(
            compareBy<CashTransaction> { it.transactionDate }
                .thenBy { it.createdAt?.seconds ?: 0L }
                .thenBy { it.id }
        )
        var runningPaise = 0L
        sortedAsc.map { tx ->
            val paise = FinancialCalculationEngine.toPaise(tx.amount)
            // In Party Khata: Cash Out = Paid to Party (Dr +), Cash In = Received from Party (Cr -)
            if (tx.isCashIn) {
                runningPaise -= paise
            } else {
                runningPaise += paise
            }
            TransactionWithBalance(
                transaction = tx,
                runningBalance = FinancialCalculationEngine.fromPaise(runningPaise)
            )
        }
    }

    val minDateMillis = remember(txDatePreset) {
        computePartyPresetMinTimestamp(txDatePreset)
    }

    val filteredPartyRowsDesc = remember(
        chronologicalRows,
        txSearchQuery,
        txDatePreset,
        txTypeFilter,
        minDateMillis
    ) {
        chronologicalRows.filter { item ->
            val tx = item.transaction
            val matchesDate = txDatePreset == DateFilterPreset.ALL_TIME || tx.transactionDate >= minDateMillis
            val matchesType = txTypeFilter == null || tx.type == txTypeFilter
            val q = txSearchQuery.trim().lowercase()
            val shortDate = StatementExportHelper.formatShortDate(tx.transactionDate).lowercase()
            val matchesSearch = q.isEmpty() ||
                tx.remark.lowercase().contains(q) ||
                tx.id.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.paymentMode.lowercase().contains(q) ||
                tx.amount.toString().contains(q) ||
                shortDate.contains(q)
            matchesDate && matchesType && matchesSearch
        }.reversed()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Passbook Header Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${summary.party.name.uppercase()} — KHATA PASSBOOK",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${summary.party.role} • ${summary.party.phone.ifBlank { "No Phone" }} • ${partyTransactions.size} Vouchers",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Passbook",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Summary Strip + Quick Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("RECEIVED (CASH IN)", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = StatementExportHelper.formatCurrency(summary.totalCashIn, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = CashInGreen
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PAID (CASH OUT)", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = StatementExportHelper.formatCurrency(summary.totalCashOut, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = CashOutRed
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val net = summary.netBalance
                        Text(
                            text = if (net >= 0) "NET DR (YOU'LL GET)" else "NET CR (YOU'LL GIVE)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = StatementExportHelper.formatCurrency(kotlin.math.abs(net), currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) CashInGreen else CashOutRed
                            )
                        )
                    }
                }

                // Quick Entry + Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCashInClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CashInGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Cash In")
                    }
                    Button(
                        onClick = onCashOutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CashOutRed, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("- Cash Out")
                    }
                    OutlinedButton(
                        onClick = onExportPdfClick,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("PDF")
                    }
                    OutlinedButton(
                        onClick = onOpenInMainRegister,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Register")
                    }
                }

                // Search Party Transaction History by Date or Remark
                OutlinedTextField(
                    value = txSearchQuery,
                    onValueChange = { txSearchQuery = it },
                    placeholder = {
                        Text(
                            "Search history by date (DD-MM-YYYY), voucher # or remark...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (txSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { txSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_passbook_search_input")
                )

                // Date & Type Filter Chips inside Passbook
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        DateFilterPreset.ALL_TIME,
                        DateFilterPreset.TODAY,
                        DateFilterPreset.THIS_WEEK,
                        DateFilterPreset.THIS_MONTH
                    ).forEach { preset ->
                        LedgerSelectableChip(
                            label = preset.label,
                            selected = txDatePreset == preset,
                            onClick = { txDatePreset = preset }
                        )
                    }
                    LedgerSelectableChip(
                        label = "Cash In (+)",
                        selected = txTypeFilter == CashTransaction.TYPE_CASH_IN,
                        onClick = {
                            txTypeFilter = if (txTypeFilter == CashTransaction.TYPE_CASH_IN) null else CashTransaction.TYPE_CASH_IN
                        }
                    )
                    LedgerSelectableChip(
                        label = "Cash Out (-)",
                        selected = txTypeFilter == CashTransaction.TYPE_CASH_OUT,
                        onClick = {
                            txTypeFilter = if (txTypeFilter == CashTransaction.TYPE_CASH_OUT) null else CashTransaction.TYPE_CASH_OUT
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Transaction History List for this Party (with explicit Edit and Delete entry buttons)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (filteredPartyRowsDesc.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions match the selected date or search filter for ${summary.party.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(
                        items = filteredPartyRowsDesc,
                        key = { it.transaction.id }
                    ) { row ->
                        val tx = row.transaction
                        val isCashIn = tx.isCashIn
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditTransactionClick(tx) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.remark.ifBlank { tx.category },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${StatementExportHelper.formatShortDate(tx.transactionDate)} • VCH-${tx.id.take(6).uppercase()} • ${tx.paymentMode}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (isCashIn) "+ " else "- ") + StatementExportHelper.formatCurrency(tx.amount, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCashIn) CashInGreen else CashOutRed
                                    )
                                )
                                val bal = row.runningBalance
                                val drCr = if (bal >= 0) "Dr" else "Cr"
                                Text(
                                    text = "Bal: ${StatementExportHelper.formatCurrency(kotlin.math.abs(bal), currencySymbol)} $drCr",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                IconButton(
                                    onClick = { onEditTransactionClick(tx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Voucher",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTransactionClick(tx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Voucher",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
