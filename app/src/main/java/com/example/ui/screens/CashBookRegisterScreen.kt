package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import com.example.data.model.TransactionWithBalance
import com.example.data.model.isCashIn
import com.example.ui.components.bouncingClickable
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
    onDeleteTransactionClick: (CashTransaction) -> Unit = {},
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

    var expandedVoucherId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<CashTransaction?>(null) }

    // Group transactions by formatted date string while preserving order
    val groupedByDate = remember(uiState.transactionsWithBalance) {
        uiState.transactionsWithBalance.groupBy {
            StatementExportHelper.formatShortDate(it.transaction.transactionDate)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("cashbook_ledger_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Minimalist Modular Summary Card with Progressive Disclosure for Tools
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

            // 2. Minimalist Search & Progressive-Disclosure Filter Strip
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

            // 3. Empty State or Date-Grouped Modular Voucher Cards
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
                        val isExpanded = expandedVoucherId == item.transaction.id
                        VoucherLedgerCard(
                            item = item,
                            currencySymbol = currency,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedVoucherId = if (isExpanded) null else item.transaction.id
                            },
                            onEditClick = { onEditTransactionClick(item.transaction) },
                            onDeleteClick = { pendingDeleteTransaction = item.transaction }
                        )
                    }
                }
            }
        }

        // 4. Clean Bottom Action Bar (+ CASH IN / - CASH OUT) with Ample Breathing Room
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCashInClick,
                    shape = RoundedCornerShape(6.dp),
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
                    shape = RoundedCornerShape(6.dp),
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

    // Delete Entry Confirmation Dialog
    val txToDelete = pendingDeleteTransaction
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            shape = RoundedCornerShape(6.dp),
            title = {
                Text(
                    text = "DELETE ENTRY (एंट्री डिलीट करें?)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Are you sure you want to permanently delete this voucher entry?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = txToDelete.remark.ifBlank { txToDelete.category },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${if (txToDelete.isCashIn) "+ Cash In" else "- Cash Out"}: ${StatementExportHelper.formatCurrency(txToDelete.amount, currency)} • ${StatementExportHelper.formatShortDate(txToDelete.transactionDate)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (txToDelete.isCashIn) CashInGreen else CashOutRed
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransactionClick(txToDelete)
                        if (expandedVoucherId == txToDelete.id) {
                            expandedVoucherId = null
                        }
                        pendingDeleteTransaction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CashOutRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_delete_voucher_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete (डिलीट करें)")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("Cancel")
                }
            }
        )
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Net Balance Hero + Quick Utility Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isFiltered) "FILTERED NET BALANCE" else "NET CLOSING BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$totalEntries Entries",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isBalanceHidden) "$currencySymbol ******" else StatementExportHelper.formatCurrency(netBalance, currencySymbol),
                        style = MaterialTheme.typography.headlineSmall.copy(
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

                // Minimalist Utility Pill Bar (Eye Privacy, Contra, Export)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clickable(onClick = onToggleHideBalance)
                            .testTag("toggle_hide_balance_button"),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isBalanceHidden) "Show Balance" else "Hide Balance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clickable(onClick = onContraTransferClick)
                            .testTag("contra_transfer_button"),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Contra",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clickable(onClick = onExportClick)
                            .testTag("open_export_dialog_button"),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export PDF or Excel CSV",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2-Column Spacious Summary: Total Cash In (+) | Total Cash Out (-)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = CashInGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, CashInGreen.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = "TOTAL CASH IN (+)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isBalanceHidden) "$currencySymbol ******" else StatementExportHelper.formatCurrency(totalCashIn, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CashInGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = CashOutRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, CashOutRed.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = "TOTAL CASH OUT (-)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isBalanceHidden) "$currencySymbol ******" else StatementExportHelper.formatCurrency(totalCashOut, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CashOutRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

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
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPartyMenu by remember { mutableStateOf(false) }

    val activeFilterCount = listOf(
        filters.datePreset != DateFilterPreset.ALL_TIME,
        filters.entryType != null,
        filters.paymentMode != null,
        filters.category != null,
        filters.partyId != null
    ).count { it }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimalist Search Input
            OutlinedTextField(
                value = filters.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search party, date (DD-MM-YYYY), or ₹...",
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
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_vouchers_input")
            )

            // Progressive Disclosure Filter Toggle Button
            Surface(
                modifier = Modifier
                    .clickable { showAdvancedFilters = !showAdvancedFilters }
                    .testTag("toggle_filters_drawer_button"),
                shape = RoundedCornerShape(6.dp),
                color = if (showAdvancedFilters || activeFilterCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (showAdvancedFilters || activeFilterCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Toggle Filters",
                        tint = if (showAdvancedFilters || activeFilterCount > 0) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    if (activeFilterCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$activeFilterCount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Progressive Disclosure: Advanced Filter Pills revealed only when requested or active
        AnimatedVisibility(
            visible = showAdvancedFilters || filters.hasActiveFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (filters.hasActiveFilters) {
                    FilterPillButton(
                        label = "Reset All",
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
        }
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
        shape = RoundedCornerShape(6.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
            .padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
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
}

/**
 * Modular, Minimalist Voucher Card with Progressive Disclosure:
 * - Collapsed: Clean primary narration, single subtle metadata line, amount, and running balance.
 * - Expanded on tap: Full voucher details + explicit Edit Entry and Delete Entry buttons.
 */
@Composable
private fun VoucherLedgerCard(
    item: TransactionWithBalance,
    currencySymbol: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val tx = item.transaction
    val isCashIn = tx.isCashIn
    val amountColor = if (isCashIn) CashInGreen else CashOutRed
    val signPrefix = if (isCashIn) "+ " else "- "

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .testTag("voucher_row_${tx.id}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Primary Title & Minimalist Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.remark.ifBlank { tx.category },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 4 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tx.partyName.isNotBlank()) {
                            RegisterTagBadge(
                                text = tx.partyName,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        RegisterTagBadge(
                            text = tx.paymentMode,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• ${StatementExportHelper.formatTime(tx.transactionDate)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Amount, Running Balance & Expand Indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$signPrefix${StatementExportHelper.formatCurrency(tx.amount, currencySymbol)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = amountColor,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Bal: ${StatementExportHelper.formatCurrency(item.runningBalance, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse details" else "Expand Edit & Delete options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progressive Disclosure Drawer: Metadata + Explicit Edit & Delete Buttons
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Details Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voucher: ${tx.voucherNumber} • Head: ${tx.category}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = StatementExportHelper.formatShortDate(tx.transactionDate),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explicit Edit & Delete Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("edit_voucher_button_${tx.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Entry (एडिट)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CashOutRed),
                            border = BorderStroke(1.dp, CashOutRed.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("delete_voucher_button_${tx.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete (डिलीट)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
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
            .background(containerColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium
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
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (hasFilters) Icons.Default.FilterListOff else Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasFilters) {
                    "NO VOUCHERS MATCH CURRENT FILTERS"
                } else {
                    "NO VOUCHERS RECORDED IN THIS REGISTER"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
            Spacer(modifier = Modifier.height(16.dp))
            if (hasFilters) {
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Clear All Filters")
                }
            } else {
                OutlinedButton(
                    onClick = onSeedSampleVouchers,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("seed_sample_vouchers_button")
                ) {
                    Text("Load Sample Business Vouchers")
                }
            }
        }
    }
}
