package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.BookSummary
import com.example.data.model.BusinessProfile
import com.example.data.model.CashTransaction
import com.example.data.model.Party
import com.example.data.model.TransactionWithBalance
import com.example.data.model.isCashIn
import com.example.data.repository.CashBookRepository
import com.example.viewmodel.CashBookUiState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class ImportStats(
    val booksCount: Int,
    val partiesCount: Int,
    val vouchersCount: Int
)

object StatementExportHelper {

    private val fileStampFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)

    /**
     * Formats an amount using standard Indian Rupee Lakh/Crore grouping:
     * ₹1,000.00 | ₹25,500.00 | ₹1,25,000.00
     */
    fun formatCurrency(amount: Double, currencySymbol: String = "₹"): String {
        return FinancialCalculationEngine.formatIndianCurrency(amount, currencySymbol)
    }

    /**
     * Formats epoch milliseconds using standard Indian date format: DD-MM-YYYY.
     */
    fun formatShortDate(epochMillis: Long): String {
        return FinancialCalculationEngine.formatDateDdMmYyyy(epochMillis)
    }

    fun formatTime(epochMillis: Long): String {
        return FinancialCalculationEngine.formatTime(epochMillis)
    }

    /**
     * Generates an Excel-compatible UTF-8 CSV file (with BOM so Microsoft Excel on PC
     * renders ₹ and Unicode characters properly) and returns the File.
     */
    fun exportToCsvFile(
        context: Context,
        summary: BookSummary,
        rows: List<TransactionWithBalance>,
        filterLabel: String,
        businessName: String = "Shivay Transport & Logistics"
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedBookName = summary.bookName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(30)
            .ifEmpty { "CashBook" }
        val timestamp = fileStampFormatter.format(Date())
        val csvFile = File(exportDir, "${sanitizedBookName}_Statement_$timestamp.csv")

        val chronologicalRows = rows.sortedBy { it.transaction.transactionDate }

        FileOutputStream(csvFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val sb = StringBuilder()
            sb.appendLine("\"$businessName - STATEMENT OF ACCOUNT (${escapeCsv(summary.bookName)})\"")
            sb.appendLine("\"Generated On\",\"${formatShortDate(System.currentTimeMillis())} ${formatTime(System.currentTimeMillis())}\",\"Filter Scope\",\"${escapeCsv(filterLabel)}\"")
            sb.appendLine("\"Total Cash In (+)\",\"${summary.totalCashIn}\",\"Total Cash Out (-)\",\"${summary.totalCashOut}\",\"Net Closing Balance\",\"${summary.netBalance}\"")
            sb.appendLine()
            sb.appendLine("\"Date (DD-MM-YYYY)\",\"Time\",\"Voucher No\",\"Voucher Type\",\"Particulars / Narration\",\"Party Name\",\"Head of Account\",\"Payment Mode\",\"Cash In (+)\",\"Cash Out (-)\",\"Running Balance\"")

            chronologicalRows.forEachIndexed { idx, item ->
                val tx = item.transaction
                val voucherNo = "VCH-${String.format(Locale.US, "%04d", idx + 1)}"
                val dateStr = formatShortDate(tx.transactionDate)
                val timeStr = formatTime(tx.transactionDate)
                val typeLabel = when {
                    FinancialCalculationEngine.isTransferTransaction(tx) -> "CONTRA TRANSFER"
                    tx.isCashIn -> "CASH IN (Cr)"
                    else -> "CASH OUT (Dr)"
                }
                val cashInCol = if (tx.isCashIn) String.format(Locale.US, "%.2f", tx.amount) else ""
                val cashOutCol = if (!tx.isCashIn) String.format(Locale.US, "%.2f", tx.amount) else ""
                val balCol = String.format(Locale.US, "%.2f", item.runningBalance)

                sb.appendLine(
                    listOf(
                        dateStr,
                        timeStr,
                        voucherNo,
                        typeLabel,
                        tx.remark.ifBlank { tx.category },
                        tx.partyName.ifBlank { "-" },
                        tx.category,
                        tx.paymentMode,
                        cashInCol,
                        cashOutCol,
                        balCol
                    ).joinToString(",") { "\"${escapeCsv(it)}\"" }
                )
            }
            fos.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        return csvFile
    }

    /**
     * Generates a formal, multi-page A4 Statement of Account PDF for printing or PC archiving.
     */
    fun exportToPdfFile(
        context: Context,
        summary: BookSummary,
        rows: List<TransactionWithBalance>,
        filterLabel: String,
        businessName: String = "SHIVAY TRANSPORT & LOGISTICS",
        gstin: String = ""
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedBookName = summary.bookName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(30)
            .ifEmpty { "CashBook" }
        val timestamp = fileStampFormatter.format(Date())
        val pdfFile = File(exportDir, "${sanitizedBookName}_Statement_$timestamp.pdf")

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 14f
            color = Color.rgb(16, 35, 56)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 9f
            color = Color.rgb(71, 85, 105)
        }
        val tableHeaderBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(30, 58, 95)
        }
        val summaryBoxBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(243, 245, 248)
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.rgb(203, 213, 225)
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 8f
            color = Color.WHITE
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 8f
            color = Color.rgb(15, 23, 42)
        }
        val cellMonoGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 8f
            color = Color.rgb(21, 128, 61)
        }
        val cellMonoRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 8f
            color = Color.rgb(185, 28, 28)
        }
        val cellMonoBalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 8f
            color = Color.rgb(15, 23, 42)
        }

        val chronologicalRows = rows.sortedBy { it.transaction.transactionDate }
        val rowsPerPage = 28
        val totalPages = kotlin.math.max(1, (chronologicalRows.size + rowsPerPage - 1) / rowsPerPage)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val margin = 28f
            var y = 34f

            val gstSuffix = if (gstin.isNotBlank()) "   |   GSTIN: $gstin" else ""
            canvas.drawText("${businessName.uppercase()} - STATEMENT OF ACCOUNT", margin, y, titlePaint)
            y += 14f
            canvas.drawText(
                "Account Register: ${summary.bookName}   |   Scope: $filterLabel   |   Date: ${formatShortDate(System.currentTimeMillis())}$gstSuffix",
                margin,
                y,
                subtitlePaint
            )
            y += 12f

            if (pageIndex == 0) {
                canvas.drawRect(margin, y, pageWidth - margin, y + 40f, summaryBoxBgPaint)
                canvas.drawRect(margin, y, pageWidth - margin, y + 40f, borderPaint)

                val colW = (pageWidth - 2 * margin) / 4f
                val sym = summary.currencySymbol
                canvas.drawText("TOTAL RECEIVED (+)", margin + 8f, y + 14f, subtitlePaint)
                canvas.drawText(formatCurrency(summary.totalCashIn, sym), margin + 8f, y + 30f, cellMonoGreenPaint)

                canvas.drawText("TOTAL PAID (-)", margin + colW + 8f, y + 14f, subtitlePaint)
                canvas.drawText(formatCurrency(summary.totalCashOut, sym), margin + colW + 8f, y + 30f, cellMonoRedPaint)

                canvas.drawText("CLOSING BALANCE", margin + 2 * colW + 8f, y + 14f, subtitlePaint)
                canvas.drawText(formatCurrency(summary.netBalance, sym), margin + 2 * colW + 8f, y + 30f, cellMonoBalPaint)

                canvas.drawText("VOUCHER COUNT", margin + 3 * colW + 8f, y + 14f, subtitlePaint)
                canvas.drawText("${chronologicalRows.size} Vouchers", margin + 3 * colW + 8f, y + 30f, cellMonoBalPaint)

                y += 50f
            } else {
                y += 8f
            }

            val tableTop = y
            val rowH = 21f
            canvas.drawRect(margin, tableTop, pageWidth - margin, tableTop + rowH, tableHeaderBgPaint)

            val colDateX = margin + 4f
            val colParticularsX = margin + 66f
            val colPartyX = margin + 215f
            val colModeX = margin + 305f
            val colCashInX = margin + 362f
            val colCashOutX = margin + 432f
            val colBalX = margin + 498f

            canvas.drawText("DATE", colDateX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("PARTICULARS", colParticularsX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("PARTY / KHATA", colPartyX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("MODE", colModeX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("CASH IN (+)", colCashInX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("CASH OUT (-)", colCashOutX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("BALANCE", colBalX, tableTop + 14f, tableHeaderTextPaint)

            val startIndex = pageIndex * rowsPerPage
            val endIndex = kotlin.math.min(startIndex + rowsPerPage, chronologicalRows.size)

            var rowY = tableTop + rowH
            for (i in startIndex until endIndex) {
                val item = chronologicalRows[i]
                val tx = item.transaction

                if (i % 2 == 1) {
                    canvas.drawRect(margin, rowY, pageWidth - margin, rowY + rowH, summaryBoxBgPaint)
                }
                canvas.drawLine(margin, rowY + rowH, pageWidth - margin, rowY + rowH, borderPaint)

                val dateStr = formatShortDate(tx.transactionDate)
                val partText = tx.remark.ifBlank { tx.category }.take(28)
                val partyText = tx.partyName.ifBlank { "-" }.take(16)
                val modeText = tx.paymentMode.take(8)

                canvas.drawText(dateStr, colDateX, rowY + 14f, cellTextPaint)
                canvas.drawText(partText, colParticularsX, rowY + 14f, cellTextPaint)
                canvas.drawText(partyText, colPartyX, rowY + 14f, cellTextPaint)
                canvas.drawText(modeText, colModeX, rowY + 14f, cellTextPaint)

                if (tx.isCashIn) {
                    canvas.drawText(formatCurrency(tx.amount, summary.currencySymbol), colCashInX, rowY + 14f, cellMonoGreenPaint)
                    canvas.drawText("-", colCashOutX, rowY + 14f, cellTextPaint)
                } else {
                    canvas.drawText("-", colCashInX, rowY + 14f, cellTextPaint)
                    canvas.drawText(formatCurrency(tx.amount, summary.currencySymbol), colCashOutX, rowY + 14f, cellMonoRedPaint)
                }

                canvas.drawText(formatCurrency(item.runningBalance, summary.currencySymbol), colBalX, rowY + 14f, cellMonoBalPaint)
                rowY += rowH
            }

            canvas.drawRect(margin, tableTop, pageWidth - margin, rowY, borderPaint)

            canvas.drawText(
                "Page ${pageIndex + 1} of $totalPages   •   $businessName Commercial Register   •   Authorized Signatory: ____________________",
                margin,
                pageHeight - 22f,
                subtitlePaint
            )

            pdfDocument.finishPage(page)
        }

        FileOutputStream(pdfFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return pdfFile
    }

    /**
     * Generates a formal Party Khata Statement of Account PDF (A4) showing all debits,
     * credits, running balance, and net settlement position for a customer or supplier.
     */
    fun exportPartyStatementPdf(
        context: Context,
        profile: BusinessProfile,
        party: Party,
        partyTransactions: List<CashTransaction>,
        currencySymbol: String = "₹"
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedParty = party.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(25)
        val timestamp = fileStampFormatter.format(Date())
        val pdfFile = File(exportDir, "PartyStatement_${sanitizedParty}_$timestamp.pdf")

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 14f
            color = Color.rgb(16, 35, 56)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 9f
            color = Color.rgb(71, 85, 105)
        }
        val tableHeaderBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(30, 58, 95)
        }
        val summaryBoxBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(243, 245, 248)
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.rgb(203, 213, 225)
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 8.5f
            color = Color.WHITE
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 8.5f
            color = Color.rgb(15, 23, 42)
        }
        val cellMonoGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 8.5f
            color = Color.rgb(21, 128, 61)
        }
        val cellMonoRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 8.5f
            color = Color.rgb(185, 28, 28)
        }
        val cellMonoBalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 8.5f
            color = Color.rgb(15, 23, 42)
        }

        val chronological = partyTransactions.sortedBy { it.transactionDate }

        var totalInPaise = 0L
        var totalOutPaise = 0L
        var runningPaise = 0L

        class PartyTxRow(
            val tx: CashTransaction,
            val runningNet: Double
        )

        val rowsWithBalance = chronological.map { tx ->
            val p = FinancialCalculationEngine.toPaise(tx.amount)
            if (tx.isCashIn) {
                totalInPaise += p
                runningPaise -= p
            } else {
                totalOutPaise += p
                runningPaise += p
            }
            PartyTxRow(tx, FinancialCalculationEngine.fromPaise(runningPaise))
        }

        val netPaise = totalOutPaise - totalInPaise
        val netDouble = FinancialCalculationEngine.fromPaise(netPaise)

        val rowsPerPage = 28
        val totalPages = kotlin.math.max(1, (rowsWithBalance.size + rowsPerPage - 1) / rowsPerPage)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val margin = 28f
            var y = 34f

            canvas.drawText("${profile.businessName.uppercase()} - PARTY KHATA STATEMENT", margin, y, titlePaint)
            y += 14f
            val contactInfo = listOfNotNull(
                profile.address.takeIf { it.isNotBlank() },
                profile.phone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
                profile.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" }
            ).joinToString("   |   ")
            canvas.drawText(contactInfo, margin, y, subtitlePaint)
            y += 14f

            if (pageIndex == 0) {
                // Party Details & Khata Summary Box
                canvas.drawRect(margin, y, pageWidth - margin, y + 54f, summaryBoxBgPaint)
                canvas.drawRect(margin, y, pageWidth - margin, y + 54f, borderPaint)

                canvas.drawText("ACCOUNT NAME: ${party.name.uppercase()}", margin + 10f, y + 16f, Paint(titlePaint).apply { textSize = 11f })
                canvas.drawText(
                    "Classification: ${party.role}   |   Phone: ${party.phone.ifBlank { "N/A" }}   |   Notes: ${party.notes.ifBlank { "Regular Account" }}",
                    margin + 10f,
                    y + 32f,
                    subtitlePaint
                )

                val netStatusText = when {
                    netDouble > 0.0 -> "YOU'LL GET: ${formatCurrency(netDouble, currencySymbol)} (Dr / Receivable)"
                    netDouble < 0.0 -> "YOU'LL GIVE: ${formatCurrency(kotlin.math.abs(netDouble), currencySymbol)} (Cr / Payable)"
                    else -> "ACCOUNT FULLY SETTLED (NIL BALANCE)"
                }
                val netColor = when {
                    netDouble > 0.0 -> Color.rgb(21, 128, 61)
                    netDouble < 0.0 -> Color.rgb(185, 28, 28)
                    else -> Color.rgb(71, 85, 105)
                }
                canvas.drawText(
                    netStatusText,
                    margin + 10f,
                    y + 47f,
                    Paint(titlePaint).apply {
                        textSize = 10f
                        color = netColor
                    }
                )

                y += 64f
            } else {
                y += 8f
            }

            val tableTop = y
            val rowH = 21f
            canvas.drawRect(margin, tableTop, pageWidth - margin, tableTop + rowH, tableHeaderBgPaint)

            val colDateX = margin + 4f
            val colParticularsX = margin + 74f
            val colCategoryX = margin + 265f
            val colModeX = margin + 355f
            val colReceivedX = margin + 415f
            val colPaidX = margin + 480f

            canvas.drawText("DATE", colDateX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("PARTICULARS / VOUCHER", colParticularsX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("HEAD / CATEGORY", colCategoryX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("MODE", colModeX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("RECEIVED (+)", colReceivedX, tableTop + 14f, tableHeaderTextPaint)
            canvas.drawText("PAID (-)", colPaidX, tableTop + 14f, tableHeaderTextPaint)

            val startIndex = pageIndex * rowsPerPage
            val endIndex = kotlin.math.min(startIndex + rowsPerPage, rowsWithBalance.size)

            var rowY = tableTop + rowH
            for (i in startIndex until endIndex) {
                val rowItem = rowsWithBalance[i]
                val tx = rowItem.tx

                if (i % 2 == 1) {
                    canvas.drawRect(margin, rowY, pageWidth - margin, rowY + rowH, summaryBoxBgPaint)
                }
                canvas.drawLine(margin, rowY + rowH, pageWidth - margin, rowY + rowH, borderPaint)

                val dateStr = formatShortDate(tx.transactionDate)
                val partText = tx.remark.ifBlank { tx.category }.take(32)
                val catText = tx.category.take(16)
                val modeText = tx.paymentMode.take(8)

                canvas.drawText(dateStr, colDateX, rowY + 14f, cellTextPaint)
                canvas.drawText(partText, colParticularsX, rowY + 14f, cellTextPaint)
                canvas.drawText(catText, colCategoryX, rowY + 14f, cellTextPaint)
                canvas.drawText(modeText, colModeX, rowY + 14f, cellTextPaint)

                if (tx.isCashIn) {
                    canvas.drawText(formatCurrency(tx.amount, currencySymbol), colReceivedX, rowY + 14f, cellMonoGreenPaint)
                    canvas.drawText("-", colPaidX, rowY + 14f, cellTextPaint)
                } else {
                    canvas.drawText("-", colReceivedX, rowY + 14f, cellTextPaint)
                    canvas.drawText(formatCurrency(tx.amount, currencySymbol), colPaidX, rowY + 14f, cellMonoRedPaint)
                }

                rowY += rowH
            }

            canvas.drawRect(margin, tableTop, pageWidth - margin, rowY, borderPaint)

            canvas.drawText(
                "Page ${pageIndex + 1} of $totalPages   •   Party: ${party.name}   •   Authorized Signatory: ____________________",
                margin,
                pageHeight - 22f,
                subtitlePaint
            )

            pdfDocument.finishPage(page)
        }

        FileOutputStream(pdfFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return pdfFile
    }

    /**
     * Generates a formal Receipt / Payment Voucher PDF including Amount in Words,
     * Voucher Number, Business Profile, Party Name, Payment Mode, and Signature block.
     */
    fun exportSingleVoucherReceiptPdf(
        context: Context,
        businessName: String,
        businessGstin: String,
        businessAddress: String,
        bookName: String,
        currencySymbol: String,
        tx: CashTransaction
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val vchCode = "VCH-${tx.id.take(6).uppercase()}"
        val pdfFile = File(exportDir, "Voucher_${vchCode}.pdf")

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 420 // Half-A4 commercial voucher slip
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.rgb(30, 58, 95)
        }
        val headerBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = if (tx.isCashIn) Color.rgb(21, 128, 61) else Color.rgb(185, 28, 28)
        }
        val titleWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 14f
            color = Color.WHITE
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 10f
            color = Color.rgb(71, 85, 105)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 11f
            color = Color.rgb(15, 23, 42)
        }
        val amountBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 18f
            color = if (tx.isCashIn) Color.rgb(21, 128, 61) else Color.rgb(185, 28, 28)
        }

        val m = 24f
        canvas.drawRect(m, m, pageWidth - m, pageHeight - m, borderPaint)
        canvas.drawRect(m, m, pageWidth - m, m + 46f, headerBgPaint)

        val voucherHeading = if (tx.isCashIn) "RECEIPT VOUCHER (CASH IN / CR)" else "PAYMENT VOUCHER (CASH OUT / DR)"
        canvas.drawText(businessName.uppercase(), m + 14f, m + 20f, titleWhitePaint)
        canvas.drawText(voucherHeading, m + 14f, m + 38f, Paint(titleWhitePaint).apply { textSize = 10.5f })
        canvas.drawText("Voucher No: $vchCode", pageWidth - m - 155f, m + 22f, Paint(titleWhitePaint).apply { textSize = 10f })
        canvas.drawText("Date: ${formatShortDate(tx.transactionDate)}", pageWidth - m - 155f, m + 38f, Paint(titleWhitePaint).apply { textSize = 10f })

        var y = m + 70f
        if (businessGstin.isNotBlank() || businessAddress.isNotBlank()) {
            canvas.drawText(
                listOfNotNull(
                    businessAddress.takeIf { it.isNotBlank() },
                    businessGstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" }
                ).joinToString("   |   "),
                m + 14f,
                y,
                labelPaint
            )
            y += 22f
        }

        canvas.drawText("Account Register:", m + 14f, y, labelPaint)
        canvas.drawText(bookName, m + 135f, y, valuePaint)
        canvas.drawText("Payment Mode:", pageWidth - 230f, y, labelPaint)
        canvas.drawText(tx.paymentMode, pageWidth - 135f, y, valuePaint)
        y += 24f

        canvas.drawText(if (tx.isCashIn) "Received From Party:" else "Paid To Party:", m + 14f, y, labelPaint)
        canvas.drawText(tx.partyName.ifBlank { "General Counter / Self" }, m + 135f, y, valuePaint)
        y += 24f

        canvas.drawText("Head of Account:", m + 14f, y, labelPaint)
        canvas.drawText(tx.category, m + 135f, y, valuePaint)
        y += 24f

        canvas.drawText("Particulars / Note:", m + 14f, y, labelPaint)
        canvas.drawText(tx.remark.ifBlank { tx.category }.take(65), m + 135f, y, valuePaint)
        y += 28f

        canvas.drawText("Amount in Words:", m + 14f, y, labelPaint)
        canvas.drawText(
            FinancialCalculationEngine.amountToIndianWords(tx.amount).take(68),
            m + 135f,
            y,
            Paint(valuePaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        )
        y += 34f

        // Amount Box
        canvas.drawRect(m + 14f, y, m + 250f, y + 40f, borderPaint)
        canvas.drawText(formatCurrency(tx.amount, currencySymbol), m + 24f, y + 27f, amountBigPaint)

        // Signatures
        canvas.drawText("Receiver's Signature", pageWidth - 280f, y + 36f, labelPaint)
        canvas.drawText("Authorized Signatory", pageWidth - 145f, y + 36f, labelPaint)

        pdfDocument.finishPage(page)
        FileOutputStream(pdfFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return pdfFile
    }

    /**
     * Sends a generated PDF file directly to Android's native PrintManager.
     */
    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("No print destination")
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    /**
     * Generates a complete JSON Backup of all Cash Books, Parties, and Vouchers with an integrity record count.
     */
    fun exportFullBackupJson(
        context: Context,
        state: CashBookUiState,
        profile: BusinessProfile
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = fileStampFormatter.format(Date())
        val backupFile = File(exportDir, "ShivayCashBook_Backup_$timestamp.json")

        val root = JSONObject().apply {
            put("backupVersion", "1.0")
            put("generatedAt", formatShortDate(System.currentTimeMillis()) + " " + formatTime(System.currentTimeMillis()))
            put("businessProfile", JSONObject().apply {
                put("businessName", profile.businessName)
                put("ownerName", profile.ownerName)
                put("phone", profile.phone)
                put("email", profile.email)
                put("address", profile.address)
                put("gstin", profile.gstin)
                put("pan", profile.pan)
                put("currencySymbol", profile.currencySymbol)
                put("financialYear", profile.financialYear)
            })
            put("booksCount", state.cashBooks.size)
            put("partiesCount", state.parties.size)
            put("vouchersCount", state.transactionsWithBalance.size)

            val booksArr = JSONArray()
            state.cashBooks.forEach { b ->
                booksArr.put(
                    JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("description", b.description)
                        put("currencySymbol", b.currencySymbol)
                    }
                )
            }
            put("cashBooks", booksArr)

            val partiesArr = JSONArray()
            state.parties.forEach { p ->
                partiesArr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("phone", p.phone)
                        put("role", p.role)
                        put("notes", p.notes)
                    }
                )
            }
            put("parties", partiesArr)

            val txArr = JSONArray()
            state.transactionsWithBalance.forEach { item ->
                val tx = item.transaction
                txArr.put(
                    JSONObject().apply {
                        put("id", tx.id)
                        put("bookId", tx.bookId)
                        put("type", tx.type)
                        put("amount", tx.amount)
                        put("remark", tx.remark)
                        put("category", tx.category)
                        put("paymentMode", tx.paymentMode)
                        put("partyId", tx.partyId)
                        put("partyName", tx.partyName)
                        put("transactionDate", tx.transactionDate)
                        put("runningBalance", item.runningBalance)
                    }
                )
            }
            put("transactions", txArr)
        }

        FileOutputStream(backupFile).use { fos ->
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }
        return backupFile
    }

    /**
     * Imports a JSON backup file and restores missing Books, Parties, and Transactions into Firestore.
     */
    suspend fun importBackupJson(
        jsonString: String,
        repository: CashBookRepository
    ): Result<ImportStats> {
        return try {
            val root = JSONObject(jsonString)
            val booksArr = root.optJSONArray("cashBooks") ?: JSONArray()
            val partiesArr = root.optJSONArray("parties") ?: JSONArray()
            val txArr = root.optJSONArray("transactions") ?: JSONArray()

            val bookIdMapping = mutableMapOf<String, String>()
            val partyIdMapping = mutableMapOf<String, String>()

            var booksImported = 0
            for (i in 0 until booksArr.length()) {
                val bObj = booksArr.getJSONObject(i)
                val originalId = bObj.optString("id")
                val name = bObj.optString("name", "Imported Register")
                val desc = bObj.optString("description", "")
                val sym = bObj.optString("currencySymbol", "₹")

                val result = repository.createCashBook(name = name, description = desc, currencySymbol = sym)
                val newId = result.getOrNull()
                if (newId != null) {
                    booksImported++
                    if (originalId.isNotBlank()) {
                        bookIdMapping[originalId] = newId
                    }
                }
            }

            var partiesImported = 0
            for (i in 0 until partiesArr.length()) {
                val pObj = partiesArr.getJSONObject(i)
                val originalId = pObj.optString("id")
                val name = pObj.optString("name", "Imported Party")
                val phone = pObj.optString("phone", "")
                val role = pObj.optString("role", "Customer")
                val notes = pObj.optString("notes", "")

                val result = repository.createParty(name = name, phone = phone, role = role, notes = notes)
                val newId = result.getOrNull()
                if (newId != null) {
                    partiesImported++
                    if (originalId.isNotBlank()) {
                        partyIdMapping[originalId] = newId
                    }
                }
            }

            var txImported = 0
            for (i in 0 until txArr.length()) {
                val tObj = txArr.getJSONObject(i)
                val originalBookId = tObj.optString("bookId")
                val targetBookId = bookIdMapping[originalBookId] ?: originalBookId
                if (targetBookId.isBlank()) continue

                val originalPartyId = tObj.optString("partyId")
                val targetPartyId = partyIdMapping[originalPartyId] ?: originalPartyId

                val type = tObj.optString("type", CashTransaction.TYPE_CASH_IN)
                val amount = tObj.optDouble("amount", 0.0)
                if (amount <= 0.0) continue

                val remark = tObj.optString("remark", "")
                val category = tObj.optString("category", "General")
                val mode = tObj.optString("paymentMode", "Cash")
                val partyName = tObj.optString("partyName", "")
                val txDate = tObj.optLong("transactionDate", System.currentTimeMillis())

                val result = repository.createTransaction(
                    bookId = targetBookId,
                    type = type,
                    amount = amount,
                    remark = remark,
                    category = category,
                    paymentMode = mode,
                    partyId = targetPartyId,
                    partyName = partyName,
                    transactionDate = txDate
                )
                if (result.isSuccess) {
                    txImported++
                }
            }

            Result.success(
                ImportStats(
                    booksCount = booksImported,
                    partiesCount = partiesImported,
                    vouchersCount = txImported
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareTextMessage(context: Context, text: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(input: String): String {
        return input.replace("\"", "\"\"")
    }
}
