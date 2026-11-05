package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.isCashIn
import com.example.util.StatementExportHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface AuditUiState {
    data object Idle : AuditUiState
    data object Analyzing : AuditUiState
    data class Streaming(val reportText: String, val auditTitle: String) : AuditUiState
    data class Success(val reportText: String, val auditTitle: String) : AuditUiState
    data class Error(val message: String) : AuditUiState
}

class FinancialAuditViewModel : ViewModel() {

    private val model by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = config
        )
    }

    private val _auditState = MutableStateFlow<AuditUiState>(AuditUiState.Idle)
    val auditState: StateFlow<AuditUiState> = _auditState.asStateFlow()

    fun runComprehensiveAudit(ledgerState: CashBookUiState) {
        val summary = ledgerState.activeBookSummary
        val sym = summary.currencySymbol
        val contextLedger = buildLedgerContextPrompt(ledgerState)

        val prompt = """
            You are a Senior Chartered Accountant and Commercial Financial Auditor reviewing the following business Cash Book and Party Khata register.
            Provide a concise, structured, utilitarian commercial audit report using plain accounting sections (no conversational fluff, no emojis):

            1. EXECUTIVE POSITION & WORKING CAPITAL RATIO
            Evaluate Net Balance vs Total Cash In and Total Cash Out, liquidity buffer, and net retention percentage.

            2. HEAD-OF-ACCOUNT EXPENSE CONCENTRATION & BURN ANALYSIS
            Identify the top expense heads and payment mode concentrations, flagging any unusual cost outflows.

            3. PARTY KHATA RECEIVABLE / PAYABLE EXPOSURE
            Assess outstanding party receivables ("To Receive / Dr") vs payables ("To Pay / Cr") and recommend collection or settlement priorities.

            4. 30-DAY CASHFLOW PROJECTION & ACTIONABLE CONTROLS
            Provide 3 concrete bookkeeping or cashflow control steps based on the actual figures below.

            --- LEDGER REGISTER DATA (Currency: $sym) ---
            $contextLedger
        """.trimIndent()

        executeStreamingAudit(
            prompt = prompt,
            auditTitle = "AUDIT REPORT: ${summary.bookName.uppercase()}"
        )
    }

    fun askCustomAuditQuery(userQuery: String, ledgerState: CashBookUiState) {
        if (userQuery.isBlank()) return
        val sym = ledgerState.activeBookSummary.currencySymbol
        val contextLedger = buildLedgerContextPrompt(ledgerState)

        val prompt = """
            You are a Senior Chartered Accountant analyzing the user's live Cash Book and Party Ledger.
            Answer the following accounting / financial question accurately using the exact ledger figures provided below. Keep the tone formal, numerical, and concise (no emojis).

            USER QUESTION:
            ${userQuery.trim()}

            --- LEDGER REGISTER DATA (Currency: $sym) ---
            $contextLedger
        """.trimIndent()

        executeStreamingAudit(
            prompt = prompt,
            auditTitle = "QUERY ANALYSIS: ${userQuery.trim().take(42)}"
        )
    }

    private fun executeStreamingAudit(prompt: String, auditTitle: String) {
        viewModelScope.launch {
            _auditState.value = AuditUiState.Analyzing
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    model.generateContentStream(prompt).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) {
                            accumulated.append(text)
                            _auditState.value = AuditUiState.Streaming(
                                reportText = accumulated.toString(),
                                auditTitle = auditTitle
                            )
                        }
                    }
                }
                if (accumulated.isNotBlank()) {
                    _auditState.value = AuditUiState.Success(
                        reportText = accumulated.toString(),
                        auditTitle = auditTitle
                    )
                } else {
                    _auditState.value = AuditUiState.Error("Audit engine returned an empty report.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Financial audit timed out", e)
                _auditState.value = AuditUiState.Error(
                    "Audit calculation timed out after 60 seconds. Please retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Financial audit generation failed", e)
                _auditState.value = AuditUiState.Error(
                    e.localizedMessage ?: "Audit generation failed."
                )
            }
        }
    }

    private fun buildLedgerContextPrompt(state: CashBookUiState): String {
        val sym = state.activeBookSummary.currencySymbol
        val sb = StringBuilder()
        sb.appendLine("Active Cash Book: ${state.activeBookSummary.bookName}")
        sb.appendLine("Total Cash Books in Business: ${state.cashBooks.size}")
        sb.appendLine("Total Cash In (+): ${StatementExportHelper.formatCurrency(state.activeBookSummary.totalCashIn, sym)}")
        sb.appendLine("Total Cash Out (-): ${StatementExportHelper.formatCurrency(state.activeBookSummary.totalCashOut, sym)}")
        sb.appendLine("Net Closing Balance: ${StatementExportHelper.formatCurrency(state.activeBookSummary.netBalance, sym)}")
        sb.appendLine("Total Vouchers in Active Book: ${state.allBookTransactionsCount}")
        sb.appendLine("Total Party Receivable (To Receive / Dr): ${StatementExportHelper.formatCurrency(state.totalReceivable, sym)}")
        sb.appendLine("Total Party Payable (To Pay / Cr): ${StatementExportHelper.formatCurrency(state.totalPayable, sym)}")

        val txs = state.transactionsWithBalance.map { it.transaction }
        val byCategory = txs.groupBy { "${it.type}:${it.category}" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        if (byCategory.isNotEmpty()) {
            sb.appendLine("\nHead of Account Breakdown:")
            byCategory.forEach { (key, sum) ->
                sb.appendLine(" - $key = ${StatementExportHelper.formatCurrency(sum, sym)}")
            }
        }

        if (state.partySummaries.isNotEmpty()) {
            sb.appendLine("\nParty Khata Balances:")
            state.partySummaries.take(15).forEach { ps ->
                val status = when {
                    ps.netBalance > 0 -> "Receivable (You'll Get) ${StatementExportHelper.formatCurrency(ps.netBalance, sym)}"
                    ps.netBalance < 0 -> "Payable (You'll Give) ${StatementExportHelper.formatCurrency(kotlin.math.abs(ps.netBalance), sym)}"
                    else -> "Settled"
                }
                sb.appendLine(" - ${ps.party.name} (${ps.party.role}): $status [${ps.transactionCount} entries]")
            }
        }

        if (state.transactionsWithBalance.isNotEmpty()) {
            sb.appendLine("\nRecent Vouchers (Up to 25):")
            state.transactionsWithBalance.take(25).forEach { item ->
                val tx = item.transaction
                val dateStr = StatementExportHelper.formatShortDate(tx.transactionDate)
                val sign = if (tx.isCashIn) "+" else "-"
                sb.appendLine(
                    " - [$dateStr] $sign${StatementExportHelper.formatCurrency(tx.amount, sym)} | Head: ${tx.category} | Mode: ${tx.paymentMode} | Party: ${tx.partyName.ifBlank { "None" }} | Note: ${tx.remark} | Running Bal: ${StatementExportHelper.formatCurrency(item.runningBalance, sym)}"
                )
            }
        } else {
            sb.appendLine("\nNo vouchers recorded in the active book yet.")
        }

        return sb.toString()
    }

    private companion object {
        const val TAG = "FinancialAuditVM"
    }
}
