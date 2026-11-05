package com.example.util

import com.example.data.model.CashTransaction
import com.example.data.model.isCashIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Centralized decimal-safe (paise minor-unit) Financial Calculation Engine.
 *
 * Enforces:
 * - Closing Balance = Opening Balance + Money Received - Money Paid + Transfers In - Transfers Out
 * - Transfers (TYPE_TRANSFER or "[TRANSFER]" category) adjust account balances but NEVER inflate Income or Expense totals.
 * - Opening Balances ("Capital / Opening" or "Opening Balance") are tracked distinctly from current-period operating income.
 * - Reversals restore balances accurately while preserving audit history.
 * - Indian Rupee Lakh/Crore formatting (e.g., ₹1,25,000.00) and DD-MM-YYYY date formatting.
 */
object FinancialCalculationEngine {

    const val TYPE_TRANSFER = "TRANSFER"
    const val CATEGORY_TRANSFER_IN = "Transfer In (Contra)"
    const val CATEGORY_TRANSFER_OUT = "Transfer Out (Contra)"
    const val CATEGORY_OPENING_BALANCE = "Capital / Opening"

    private val indianDateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val indianTimeFormatter = SimpleDateFormat("hh:mm a", Locale.US)

    /**
     * Converts a decimal Rupee amount to exact integer minor units (paise) using HALF_UP rounding.
     */
    fun toPaise(rupees: Double): Long {
        if (rupees.isNaN() || rupees.isInfinite()) return 0L
        return BigDecimal.valueOf(rupees)
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Converts integer minor units (paise) back to exact decimal Rupees.
     */
    fun toRupees(paise: Long): Double {
        return BigDecimal.valueOf(paise)
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun fromPaise(paise: Long): Double = toRupees(paise)

    fun sumAmountsExact(amounts: Iterable<Double>): Double {
        var totalPaise = 0L
        for (a in amounts) {
            totalPaise += toPaise(a)
        }
        return toRupees(totalPaise)
    }

    fun isTransferTransaction(tx: CashTransaction): Boolean {
        return tx.type == TYPE_TRANSFER ||
            tx.category.contains("Transfer", ignoreCase = true) ||
            tx.remark.startsWith("[CONTRA", ignoreCase = true)
    }

    fun isOpeningBalanceTransaction(tx: CashTransaction): Boolean {
        return tx.category.equals(CATEGORY_OPENING_BALANCE, ignoreCase = true) ||
            tx.category.equals("Opening Balance", ignoreCase = true)
    }

    /**
     * Formats an amount using standard Indian numbering system (Lakhs / Crores):
     * ₹1,000.00 | ₹25,500.00 | ₹1,25,000.00
     */
    fun formatIndianCurrency(amount: Double, currencySymbol: String = "₹"): String {
        val paise = toPaise(amount)
        val isNegative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val wholeRupees = absPaise / 100L
        val decimalPaise = absPaise % 100L

        val formattedWhole = formatIndianInteger(wholeRupees)
        val formattedDecimal = String.format(Locale.US, "%02d", decimalPaise)
        val sign = if (isNegative) "-" else ""
        return "$sign$currencySymbol$formattedWhole.$formattedDecimal"
    }

    private fun formatIndianInteger(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val groups = mutableListOf<String>()
        var i = remaining.length
        while (i > 0) {
            val start = (i - 2).coerceAtLeast(0)
            groups.add(0, remaining.substring(start, i))
            i = start
        }
        return groups.joinToString(",") + "," + lastThree
    }

    /**
     * Formats epoch milliseconds in standard Indian date format: DD-MM-YYYY.
     */
    fun formatDateDdMmYyyy(epochMillis: Long): String {
        if (epochMillis <= 0L) return "-"
        return synchronized(indianDateFormatter) {
            indianDateFormatter.format(Date(epochMillis))
        }
    }

    fun formatTime(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return synchronized(indianTimeFormatter) {
            indianTimeFormatter.format(Date(epochMillis))
        }
    }

    /**
     * Converts a monetary amount into formal Indian English words for Receipt & Payment Vouchers.
     * Example: 25500.0 -> "Rupees Twenty-Five Thousand Five Hundred Only"
     */
    fun amountToIndianWords(amount: Double): String {
        val absPaise = kotlin.math.abs(toPaise(amount))
        val rupees = absPaise / 100L
        val paise = (absPaise % 100L).toInt()

        if (rupees == 0L && paise == 0) {
            return "Rupees Zero Only"
        }

        val rupeeWords = if (rupees > 0L) convertNumberToIndianWords(rupees) else ""
        val paiseWords = if (paise > 0) convertBelowHundred(paise) else ""

        return buildString {
            append("Rupees ")
            if (rupeeWords.isNotEmpty()) {
                append(rupeeWords)
            } else {
                append("Zero")
            }
            if (paiseWords.isNotEmpty()) {
                append(" and ")
                append(paiseWords)
                append(" Paise")
            }
            append(" Only")
        }
    }

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private fun convertBelowHundred(n: Int): String {
        if (n < 20) return units[n]
        val t = tens[n / 10]
        val u = units[n % 10]
        return if (u.isEmpty()) t else "$t-$u"
    }

    private fun convertNumberToIndianWords(n: Long): String {
        if (n == 0L) return ""
        var rem = n
        val parts = mutableListOf<String>()

        val crores = rem / 10_000_000L
        rem %= 10_000_000L
        if (crores > 0) {
            parts.add("${convertNumberToIndianWords(crores)} Crore")
        }

        val lakhs = (rem / 100_000L).toInt()
        rem %= 100_000L
        if (lakhs > 0) {
            parts.add("${convertBelowHundred(lakhs)} Lakh")
        }

        val thousands = (rem / 1_000L).toInt()
        rem %= 1_000L
        if (thousands > 0) {
            parts.add("${convertBelowHundred(thousands)} Thousand")
        }

        val hundreds = (rem / 100L).toInt()
        rem %= 100L
        if (hundreds > 0) {
            parts.add("${units[hundreds]} Hundred")
        }

        if (rem > 0) {
            parts.add(convertBelowHundred(rem.toInt()))
        }

        return parts.joinToString(" ")
    }

    data class DashboardMetrics(
        val openingBalance: Double = 0.0,
        val totalOperatingIncome: Double = 0.0,
        val totalOperatingExpense: Double = 0.0,
        val totalTransfersIn: Double = 0.0,
        val totalTransfersOut: Double = 0.0,
        val closingBalance: Double = 0.0,
        val todayIncome: Double = 0.0,
        val todayExpense: Double = 0.0,
        val todayNetFlow: Double = 0.0,
        val monthIncome: Double = 0.0,
        val monthExpense: Double = 0.0
    ) {
        val operatingCashIn: Double get() = totalOperatingIncome
        val operatingCashOut: Double get() = totalOperatingExpense
        val monthlyNetCashFlow: Double get() = toRupees(toPaise(monthIncome) - toPaise(monthExpense))
    }

    /**
     * Computes exact paise-based financial metrics for a collection of transactions.
     * Transfers and Opening Balances are excluded from Operating Income and Operating Expense totals.
     */
    fun computeMetrics(
        transactions: List<CashTransaction>,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardMetrics {
        val calToday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calToday.timeInMillis

        val calMonth = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calMonth.timeInMillis

        var openingPaise = 0L
        var operatingInPaise = 0L
        var operatingOutPaise = 0L
        var transferInPaise = 0L
        var transferOutPaise = 0L
        var todayInPaise = 0L
        var todayOutPaise = 0L
        var monthInPaise = 0L
        var monthOutPaise = 0L

        for (tx in transactions) {
            val p = toPaise(tx.amount)
            val isTransfer = isTransferTransaction(tx)
            val isOpening = isOpeningBalanceTransaction(tx)
            val isCredit = tx.type == CashTransaction.TYPE_CASH_IN ||
                tx.category.equals(CATEGORY_TRANSFER_IN, ignoreCase = true)

            when {
                isOpening -> {
                    if (isCredit) openingPaise += p else openingPaise -= p
                }
                isTransfer -> {
                    if (isCredit) transferInPaise += p else transferOutPaise += p
                }
                else -> {
                    if (tx.isCashIn) {
                        operatingInPaise += p
                        if (tx.transactionDate >= startOfToday) todayInPaise += p
                        if (tx.transactionDate >= startOfMonth) monthInPaise += p
                    } else {
                        operatingOutPaise += p
                        if (tx.transactionDate >= startOfToday) todayOutPaise += p
                        if (tx.transactionDate >= startOfMonth) monthOutPaise += p
                    }
                }
            }
        }

        val closingPaise = openingPaise + operatingInPaise - operatingOutPaise + transferInPaise - transferOutPaise

        return DashboardMetrics(
            openingBalance = toRupees(openingPaise),
            totalOperatingIncome = toRupees(operatingInPaise),
            totalOperatingExpense = toRupees(operatingOutPaise),
            totalTransfersIn = toRupees(transferInPaise),
            totalTransfersOut = toRupees(transferOutPaise),
            closingBalance = toRupees(closingPaise),
            todayIncome = toRupees(todayInPaise),
            todayExpense = toRupees(todayOutPaise),
            todayNetFlow = toRupees(todayInPaise - todayOutPaise),
            monthIncome = toRupees(monthInPaise),
            monthExpense = toRupees(monthOutPaise)
        )
    }
}
