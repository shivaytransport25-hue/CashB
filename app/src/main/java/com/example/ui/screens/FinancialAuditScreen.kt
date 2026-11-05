package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.isCashIn
import com.example.ui.theme.CashInGreen
import com.example.ui.theme.CashOutRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.util.FinancialCalculationEngine
import com.example.util.StatementExportHelper
import com.example.viewmodel.AuditUiState
import com.example.viewmodel.CashBookUiState

@Composable
fun FinancialAuditScreen(
    uiState: CashBookUiState,
    auditState: AuditUiState,
    onRunFullAudit: () -> Unit,
    onAskCustomQuery: (String) -> Unit,
    onEditBusinessProfileClick: () -> Unit,
    onPrintStatementClick: () -> Unit,
    onExportBackupClick: () -> Unit
) {
    var customQuery by remember { mutableStateOf("") }
    val currency = uiState.activeBookSummary.currencySymbol
    val txs = remember(uiState.transactionsWithBalance) {
        uiState.transactionsWithBalance.map { it.transaction }
    }

    val metrics = remember(txs) {
        FinancialCalculationEngine.computeMetrics(txs)
    }

    // Compute Head of Account (Category) Totals for Expense & Receipt Tables (excluding Contra Transfers)
    val expenseByHead = remember(txs) {
        txs.filter { !it.isCashIn && !FinancialCalculationEngine.isTransferTransaction(it) }
            .groupBy { it.category }
            .mapValues { (_, list) ->
                FinancialCalculationEngine.sumAmountsExact(list.map { it.amount })
            }
            .entries
            .sortedByDescending { it.value }
    }

    val receiptByMode = remember(txs) {
        txs.groupBy { it.paymentMode }
            .mapValues { (_, list) ->
                val totalIn = FinancialCalculationEngine.sumAmountsExact(
                    list.filter { it.isCashIn }.map { it.amount }
                )
                val totalOut = FinancialCalculationEngine.sumAmountsExact(
                    list.filter { !it.isCashIn }.map { it.amount }
                )
                totalIn to totalOut
            }
            .entries
            .sortedByDescending { it.value.first + it.value.second }
    }

    val isAnalyzing = auditState is AuditUiState.Analyzing || auditState is AuditUiState.Streaming

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("financial_audit_screen"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Business Profile & Period Financial Summary Dashboard Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.businessName.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "GSTIN: ${uiState.businessGstin.ifBlank { "Unregistered" }} • FY 2026-27 (INR)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onEditBusinessProfileClick,
                            modifier = Modifier.testTag("edit_business_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Firm", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // 2x3 Executive Accounting Metrics Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DashboardMetricCell(
                                label = "OPENING CAPITAL",
                                value = StatementExportHelper.formatCurrency(metrics.openingBalance, currency),
                                valueColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "OPERATING IN (+)",
                                value = StatementExportHelper.formatCurrency(metrics.operatingCashIn, currency),
                                valueColor = CashInGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "OPERATING OUT (-)",
                                value = StatementExportHelper.formatCurrency(metrics.operatingCashOut, currency),
                                valueColor = CashOutRed,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(modifier = Modifier.fillMaxWidth()) {
                            DashboardMetricCell(
                                label = "TODAY INFLOW",
                                value = StatementExportHelper.formatCurrency(metrics.todayIncome, currency),
                                valueColor = CashInGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "TODAY OUTFLOW",
                                value = StatementExportHelper.formatCurrency(metrics.todayExpense, currency),
                                valueColor = CashOutRed,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCell(
                                label = "MONTH NET FLOW",
                                value = StatementExportHelper.formatCurrency(metrics.monthlyNetCashFlow, currency),
                                valueColor = if (metrics.monthlyNetCashFlow >= 0.0) CashInGreen else CashOutRed,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPrintStatementClick,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("audit_print_statement_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print Statement", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = onExportBackupClick,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("audit_backup_json_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JSON Backup", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // 1. Head-of-Account Expense Breakdown Table
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HEAD-OF-ACCOUNT EXPENSE REGISTER",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.activeBookSummary.bookName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    if (expenseByHead.isEmpty()) {
                        Text(
                            text = "No Operating Cash Out expense vouchers recorded in this register yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    } else {
                        val totalOut = metrics.operatingCashOut.coerceAtLeast(1.0)
                        expenseByHead.forEachIndexed { idx, entry ->
                            val sharePct = ((entry.value / totalOut) * 100.0).toInt()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "$sharePct% of operating outflow",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = StatementExportHelper.formatCurrency(entry.value, currency),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CashOutRed
                                )
                            }
                            if (idx < expenseByHead.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        // 2. Payment Mode Reconciliation Table (Cash vs Bank vs UPI)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PAYMENT MODE RECONCILIATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CASH IN (+) / CASH OUT (-)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    if (receiptByMode.isEmpty()) {
                        Text(
                            text = "Record vouchers to view Cash, Bank Transfer, and UPI mode split.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    } else {
                        receiptByMode.forEachIndexed { idx, (mode, pair) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "+${StatementExportHelper.formatCurrency(pair.first, currency)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CashInGreen
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "-${StatementExportHelper.formatCurrency(pair.second, currency)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CashOutRed
                                )
                            }
                            if (idx < receiptByMode.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        // 3. Executive Financial Audit & Cashflow Projection Module
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF162B45))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FINANCIAL AUDIT & CASHFLOW PROJECTION",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Generate a formal working-capital audit, burn-rate check, party credit exposure review, and 30-day cashflow forecast based on your current register.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onRunFullAudit,
                            enabled = !isAnalyzing,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_financial_audit_button")
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPUTING FINANCIAL AUDIT...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GENERATE FULL AUDIT REPORT")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Accounting Query Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customQuery,
                                onValueChange = { customQuery = it },
                                placeholder = {
                                    Text(
                                        "Ask a specific accounting question (e.g. fuel cost ratio, overdue parties)...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(4.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("audit_custom_query_input")
                            )

                            OutlinedButton(
                                onClick = {
                                    if (customQuery.isNotBlank()) {
                                        onAskCustomQuery(customQuery)
                                    }
                                },
                                enabled = !isAnalyzing && customQuery.isNotBlank(),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("submit_audit_query_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Submit accounting query",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Audit Output Box
                        when (auditState) {
                            is AuditUiState.Idle -> {
                                // No report run yet
                            }
                            is AuditUiState.Analyzing -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Reconciling ledger vouchers, party balances, and head-of-account totals...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is AuditUiState.Streaming -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                AuditReportDocumentBox(
                                    title = auditState.auditTitle,
                                    reportText = auditState.reportText,
                                    isStreaming = true
                                )
                            }
                            is AuditUiState.Success -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                AuditReportDocumentBox(
                                    title = auditState.auditTitle,
                                    reportText = auditState.reportText,
                                    isStreaming = false
                                )
                            }
                            is AuditUiState.Error -> {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Text(
                                        text = auditState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
    }
}

@Composable
private fun AuditReportDocumentBox(
    title: String,
    reportText: String,
    isStreaming: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isStreaming) {
                Text(
                    text = "GENERATING...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = reportText,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("audit_report_text")
        )
    }
}
