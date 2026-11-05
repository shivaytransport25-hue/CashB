package com.example

import com.example.data.model.CashTransaction
import com.example.util.FinancialCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationEngineTest {

    @Test
    fun `formatIndianCurrency formats thousands lakhs and crores accurately`() {
        assertEquals("₹1,000.00", FinancialCalculationEngine.formatIndianCurrency(1000.0))
        assertEquals("₹25,500.00", FinancialCalculationEngine.formatIndianCurrency(25500.0))
        assertEquals("₹1,25,000.00", FinancialCalculationEngine.formatIndianCurrency(125000.0))
        assertEquals("-₹45,250.50", FinancialCalculationEngine.formatIndianCurrency(-45250.5))
    }

    @Test
    fun `amountToIndianWords converts rupees and paise to formal voucher words`() {
        assertEquals(
            "Rupees Twenty-Five Thousand Five Hundred Only",
            FinancialCalculationEngine.amountToIndianWords(25500.0)
        )
        assertEquals(
            "Rupees One Lakh Twenty-Five Thousand Only",
            FinancialCalculationEngine.amountToIndianWords(125000.0)
        )
        assertEquals(
            "Rupees One Thousand Two Hundred Fifty and Fifty Paise Only",
            FinancialCalculationEngine.amountToIndianWords(1250.50)
        )
    }

    @Test
    fun `computeMetrics excludes transfers and opening balances from operating income and expense`() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            CashTransaction(
                id = "1",
                type = CashTransaction.TYPE_CASH_IN,
                amount = 100000.0,
                category = FinancialCalculationEngine.CATEGORY_OPENING_BALANCE,
                transactionDate = now
            ),
            CashTransaction(
                id = "2",
                type = CashTransaction.TYPE_CASH_IN,
                amount = 25500.25,
                category = "Sales",
                transactionDate = now
            ),
            CashTransaction(
                id = "3",
                type = CashTransaction.TYPE_CASH_OUT,
                amount = 5200.10,
                category = "Fuel & Diesel",
                transactionDate = now
            ),
            CashTransaction(
                id = "4",
                type = CashTransaction.TYPE_CASH_OUT,
                amount = 10000.0,
                category = FinancialCalculationEngine.CATEGORY_TRANSFER_OUT,
                remark = "[CONTRA] Cash to SBI Bank Transfer",
                transactionDate = now
            )
        )

        val metrics = FinancialCalculationEngine.computeMetrics(txs, now)
        assertEquals(100000.0, metrics.openingBalance, 0.001)
        assertEquals(25500.25, metrics.totalOperatingIncome, 0.001)
        assertEquals(5200.10, metrics.totalOperatingExpense, 0.001)
        assertEquals(10000.0, metrics.totalTransfersOut, 0.001)
        // Closing = 100,000 + 25,500.25 - 5,200.10 - 10,000 = 110,300.15
        assertEquals(110300.15, metrics.closingBalance, 0.001)
    }
}
