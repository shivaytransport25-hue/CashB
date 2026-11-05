package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.bouncingClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.TransactionWithBalance
import com.example.data.model.isCashIn
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.StatementExportHelper
import com.example.viewmodel.CashBookUiState

@Composable
fun CashBookRegisterScreen(
    uiState: CashBookUiState,
    onCashInClick: () -> Unit,
    onCashOutClick: () -> Unit,
    onContraTransferClick: () -> Unit,
    onEditTransactionClick: (CashTransaction) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDatePresetChange: (DateFilterPreset) -> Unit,
    onEntryTypeFilterChange: (String?) -> Unit,
    onPaymentModeFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPartyFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onCustomDateRangeClick: () -> Unit = {},
    onExportStatementClick: () -> Unit,
    onSeedSampleVouchers: () -> Unit,
    onToggleHideBalance: () -> Unit = {}
) {
    val summary = if (uiState.filterState.hasActiveFilters) {
        uiState.filteredBookSummary
    } else {
        uiState.activeBookSummary
    }
    val currency = summary.currencySymbol
    val isBalanceHidden = uiState.businessProfile.hideBalanceOnHome

    // Group transactions by formatted date string while preserving order
    val groupedByDate = remember(uiState.transactionsWithBalance) {
        uiState.transactionsWithBalance.groupBy {
            StatementExportHelper.formatShortDate(it.transaction.transactionDate)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("cashbook_ledger_list"),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            // 1. Passbook Summary Header Table
            item {
                PassbookBalanceSummaryCard(
                    totalCashIn = summary.totalCashIn,
                    totalCashOut = summary.totalCashOut,
                    netBalance = summary.netBalance,
                    totalEntries = summary.totalEntries,
                    currencySymbol = currency,
                    isFiltered = uiState.filterState.hasActiveFilters,
                    isBalanceHidden = isBalanceHidden,
                    onToggleHideBalance = onToggleHideBalance,
                    onContraTransferClick = onContraTransferClick,
                    onExportClick = onExportStatementClick
                )
            }

            // 2. Search & Filter Strip
            item {
                RegisterFilterBar(
                    uiState = uiState,
                    onSearchQueryChange = onSearchQueryChange,
                    onDatePresetChange = onDatePresetChange,
                    onCustomDateRangeClick = onCustomDateRangeClick,
                    onEntryTypeFilterChange = onEntryTypeFilterChange,
                    onPaymentModeFilterChange = onPaymentModeFilterChange,
                    onCategoryFilterChange = onCategoryFilterChange,
                    onPartyFilterChange = onPartyFilterChange,
                    onClearFilters = onClearFilters
                )
            }

            // 3. Column Header Bar (Tally / Passbook Style)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARTICULARS / ACCOUNT HEAD / MODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "AMOUNT / RUNNING BAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Empty State or Date-Grouped Tabular Voucher Rows
            if (uiState.transactionsWithBalance.isEmpty()) {
                item {
                    EmptyRegisterView(
                        hasFilters = uiState.filterState.hasActiveFilters,
                        onClearFilters = onClearFilters,
                        onSeedSampleVouchers = onSeedSampleVouchers
                    )
                }
            } else {
                groupedByDate.forEach { (dateLabel, dayItems) ->
                    item(key = "header_$dateLabel") {
                        DateSubHeaderRow(
                            dateLabel = dateLabel,
                            entryCount = dayItems.size
                        )
                    }
                    items(
                        items = dayItems,
                        key = { it.transaction.id }
                    ) { item ->
                        VoucherLedgerRow(
                            item = item,
                            currencySymbol = currency,
                            onClick = { onEditTransactionClick(item.transaction) }
                        )
                    }
                }
            }
        }

        // 5. Sticky Bottom Dual Action Bar (+ CASH IN / - CASH OUT)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCashInClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CashInGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bouncingClickable(onClick = onCashInClick)
                        .testTag("cash_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CASH IN (+)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    )
                }

                Button(
                    onClick = onCashOutClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CashOutRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bouncingClickable(onClick = onCashOutClick)
                        .testTag("cash_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CASH OUT (-)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PassbookBalanceSummaryCard(
    totalCashIn: Double,
    totalCashOut: Double,
    netBalance: Double,
    totalEntries: Int,
    currencySymbol: String,
    isFiltered: Boolean,
    isBalanceHidden: Boolean = false,
    onToggleHideBalance: () -> Unit = {},
    onContraTransferClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top bar inside passbook box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isFiltered) "FILTERED STATEMENT" else "REGISTER SUMMARY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$totalEntries Vch",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clipOrBorder()
                            .clickable(onClick = onToggleHideBalance)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("toggle_hide_balance_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isBalanceHidden) "Show Balance" else "Hide Balance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clipOrBorder()
                            .clickable(onClick = onContraTransferClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("contra_transfer_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contra",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clipOrBorder()
                            .clickable(onClick = onExportClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("open_export_dialog_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export PDF or Excel CSV",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PDF / Excel",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 3-Column Tabular Figures: Cash In | Cash Out | Net Balance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "TOTAL CASH IN (+)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isBalanceHidden) "$currencySymbol ******" else StatementExportHelper.formatCurrency(totalCashIn, currencySymbol),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CashInGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "TOTAL CASH OUT (-)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isBalanceHidden) "$currencySymbol ******" else StatementExportHelper.formatCurrency(totalCashOut, currencySymbol),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CashOutRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "NET BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isBalanceHidden) "$currencySymbol ******" else StatementExportHelper.formatCurrency(netBalance, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (netBalance >= 0.0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            CashOutRed
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("net_balance_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.clipOrBorder(): Modifier =
    this.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))

@Composable
private fun RegisterFilterBar(
    uiState: CashBookUiState,
    onSearchQueryChange: (String) -> Unit,
    onDatePresetChange: (DateFilterPreset) -> Unit,
    onCustomDateRangeClick: () -> Unit,
    onEntryTypeFilterChange: (String?) -> Unit,
    onPaymentModeFilterChange: (String?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPartyFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    val filters = uiState.filterState
    var showModeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPartyMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        // Search Input (by Party Name, Date DD-MM-YYYY, Voucher #, Remark, or Amount)
        OutlinedTextField(
            value = filters.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search party name, date (DD-MM-YYYY), voucher # or ₹...",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (filters.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onCustomDateRangeClick,
                        modifier = Modifier.testTag("search_bar_date_range_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Filter by Date Range",
                            tint = if (filters.datePreset == DateFilterPreset.CUSTOM) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_vouchers_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Filter Pills (Date Range, Entry Type, Mode, Category, Party)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (filters.hasActiveFilters) {
                FilterPillButton(
                    label = "Reset Filters",
                    active = true,
                    onClick = onClearFilters,
                    modifier = Modifier.testTag("reset_filters_button")
                )
            }

            // Date Presets
            DateFilterPreset.entries.forEach { preset ->
                val label = if (preset == DateFilterPreset.CUSTOM && filters.customStartDate > 0L) {
                    "${StatementExportHelper.formatShortDate(filters.customStartDate)}..${StatementExportHelper.formatShortDate(filters.customEndDate)}"
                } else {
                    preset.label
                }
                FilterPillButton(
                    label = label,
                    active = filters.datePreset == preset && preset != DateFilterPreset.ALL_TIME,
                    onClick = {
                        if (preset == DateFilterPreset.CUSTOM) {
                            onCustomDateRangeClick()
                        } else {
                            onDatePresetChange(preset)
                        }
                    }
                )
            }

            // Entry Type Filter
            FilterPillButton(
                label = when (filters.entryType) {
                    CashTransaction.TYPE_CASH_IN -> "Type: Cash In (+)"
                    CashTransaction.TYPE_CASH_OUT -> "Type: Cash Out (-)"
                    else -> "Type: All"
                },
                active = filters.entryType != null,
                onClick = {
                    val next = when (filters.entryType) {
                        null -> CashTransaction.TYPE_CASH_IN
                        CashTransaction.TYPE_CASH_IN -> CashTransaction.TYPE_CASH_OUT
                        else -> null
                    }
                    onEntryTypeFilterChange(next)
                }
            )

            // Payment Mode Dropdown Pill
            Box {
                FilterPillButton(
                    label = filters.paymentMode?.let { "Mode: $it" } ?: "Mode: All",
                    active = filters.paymentMode != null,
                    onClick = { showModeMenu = true }
                )
                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Payment Modes") },
                        onClick = {
                            onPaymentModeFilterChange(null)
                            showModeMenu = false
                        }
                    )
                    CashTransaction.PAYMENT_MODES.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                onPaymentModeFilterChange(mode)
                                showModeMenu = false
                            }
                        )
                    }
                }
            }

            // Head of Account (Category) Dropdown Pill
            Box {
                FilterPillButton(
                    label = filters.category?.let { "Head: $it" } ?: "Account Head: All",
                    active = filters.category != null,
                    onClick = { showCategoryMenu = true }
                )
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Account Heads") },
                        onClick = {
                            onCategoryFilterChange(null)
                            showCategoryMenu = false
                        }
                    )
                    CashTransaction.ALL_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onCategoryFilterChange(cat)
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Party Filter Dropdown Pill
            if (uiState.parties.isNotEmpty()) {
                val activePartyName = uiState.parties.find { it.id == filters.partyId }?.name
                Box {
                    FilterPillButton(
                        label = activePartyName?.let { "Party: $it" } ?: "Party: All",
                        active = filters.partyId != null,
                        onClick = { showPartyMenu = true }
                    )
                    DropdownMenu(
                        expanded = showPartyMenu,
                        onDismissRequest = { showPartyMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Parties") },
                            onClick = {
                                onPartyFilterChange(null)
                                showPartyMenu = false
                            }
                        )
                        uiState.parties.forEach { party ->
                            DropdownMenuItem(
                                text = { Text("${party.name} (${party.role})") },
                                onClick = {
                                    onPartyFilterChange(party.id)
                                    showPartyMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FilterPillButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DateSubHeaderRow(
    dateLabel: String,
    entryCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (entryCount == 1) "1 Entry" else "$entryCount Entries",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun VoucherLedgerRow(
    item: TransactionWithBalance,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val tx = item.transaction
    val isCashIn = tx.isCashIn
    val amountColor = if (isCashIn) CashInGreen else CashOutRed
    val signPrefix = if (isCashIn) "+ " else "- "

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("voucher_row_${tx.id}"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Particulars, Party, Category & Mode Badges
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.remark.ifBlank { tx.category },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Payment Mode Badge
                        RegisterTagBadge(
                            text = tx.paymentMode.uppercase(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Category / Account Head Badge
                        RegisterTagBadge(
                            text = tx.category,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Time
                        Text(
                            text = StatementExportHelper.formatTime(tx.transactionDate),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tx.partyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Party A/c: ${tx.partyName}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Amount & Running Balance (Monospaced Right-Aligned)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$signPrefix${StatementExportHelper.formatCurrency(tx.amount, currencySymbol)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = amountColor,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Bal: ${StatementExportHelper.formatCurrency(item.runningBalance, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RegisterTagBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyRegisterView(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onSeedSampleVouchers: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(4.dp),
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
                imageVector = if (hasFilters) Icons.Default.FilterListOff else Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (hasFilters) {
                    "NO VOUCHERS MATCH CURRENT FILTERS"
                } else {
                    "NO VOUCHERS RECORDED IN THIS REGISTER"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasFilters) {
                    "Reset your date, party, or payment mode filter to view all register entries."
                } else {
                    "Use the + CASH IN or - CASH OUT buttons below to log your first entry, or load sample commercial vouchers to inspect the register."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (hasFilters) {
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Reset Active Filters")
                }
            } else {
                OutlinedButton(
                    onClick = onSeedSampleVouchers,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("seed_sample_vouchers_button")
                ) {
                    Text("Load Sample Business Vouchers")
                }
            }
        }
    }
}
