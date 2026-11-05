import 'dart:typed_data';
import 'package:csv/csv.dart';
import 'package:excel/excel.dart';
import 'package:flutter/foundation.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

import '../accounting/financial_engine.dart';
import '../core/constants/enums.dart';
import '../core/utils/currency_formatter.dart';
import '../core/utils/date_formatter.dart';
import '../models/business_model.dart';
import '../models/transaction_model.dart';

class ReportService {
  ReportService._();

  /// Generates a professional Excel (.xlsx) spreadsheet for transactions
  static Future<List<int>> generateExcelReport({
    required BusinessModel business,
    required String reportTitle,
    required String dateRange,
    required List<TransactionModel> transactions,
  }) async {
    final excel = Excel.createExcel();
    final sheetName = 'CashBook_Report';
    final Sheet sheet = excel[sheetName];
    excel.setDefaultSheet(sheetName);

    // Business Header
    sheet.appendRow([TextCellValue(business.name.toUpperCase())]);
    sheet.appendRow([TextCellValue(business.address ?? '')]);
    sheet.appendRow([TextCellValue('GSTIN: ${business.gstin ?? 'N/A'} | PAN: ${business.pan ?? 'N/A'}')]);
    sheet.appendRow([TextCellValue('$reportTitle ($dateRange)')]);
    sheet.appendRow([TextCellValue('')]); // Blank row

    // Table Column Headers
    sheet.appendRow([
      TextCellValue('Sr No.'),
      TextCellValue('Date'),
      TextCellValue('Voucher No.'),
      TextCellValue('Type'),
      TextCellValue('Account'),
      TextCellValue('Category'),
      TextCellValue('Party Name'),
      TextCellValue('Payment Mode'),
      TextCellValue('Income (INR)'),
      TextCellValue('Expense (INR)'),
      TextCellValue('Status'),
      TextCellValue('Description'),
    ]);

    int sr = 1;
    int totalIncomePaise = 0;
    int totalExpensePaise = 0;

    for (final txn in transactions) {
      final double income = txn.isIncome ? txn.amountPaise / 100.0 : 0.0;
      final double expense = txn.isExpense ? txn.amountPaise / 100.0 : 0.0;

      if (txn.status == TransactionStatus.posted) {
        if (txn.isIncome) totalIncomePaise += txn.amountPaise;
        if (txn.isExpense) totalExpensePaise += txn.amountPaise;
      }

      sheet.appendRow([
        IntCellValue(sr++),
        TextCellValue(txn.transactionDate),
        TextCellValue(txn.voucherNumber),
        TextCellValue(txn.transactionType.displayName),
        TextCellValue(txn.accountName ?? ''),
        TextCellValue(txn.categoryName ?? '-'),
        TextCellValue(txn.partyName ?? '-'),
        TextCellValue(txn.paymentMode),
        DoubleCellValue(income),
        DoubleCellValue(expense),
        TextCellValue(txn.status.name.toUpperCase()),
        TextCellValue(txn.description ?? ''),
      ]);
    }

    // Totals Row
    sheet.appendRow([TextCellValue('')]);
    sheet.appendRow([
      TextCellValue('TOTAL'),
      TextCellValue(''),
      TextCellValue(''),
      TextCellValue(''),
      TextCellValue(''),
      TextCellValue(''),
      TextCellValue(''),
      TextCellValue(''),
      DoubleCellValue(totalIncomePaise / 100.0),
      DoubleCellValue(totalExpensePaise / 100.0),
      TextCellValue('Net: ${CurrencyFormatter.formatPaise(totalIncomePaise - totalExpensePaise)}'),
      TextCellValue(''),
    ]);

    final bytes = excel.encode();
    return bytes ?? [];
  }

  /// Generates a professional PDF Cash Book Report
  static Future<Uint8List> generatePdfReport({
    required BusinessModel business,
    required String reportTitle,
    required String dateRange,
    required List<TransactionModel> transactions,
  }) async {
    final pdf = pw.Document();
    final font = await PdfGoogleFonts.robotoRegular();
    final fontBold = await PdfGoogleFonts.robotoBold();

    final summary = FinancialEngine.calculateSummary(transactions);

    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4.landscape,
        margin: const pw.EdgeInsets.all(24),
        theme: pw.ThemeData.withFont(base: font, bold: fontBold),
        header: (context) => pw.Column(
          crossAxisAlignment: pw.CrossAxisAlignment.start,
          children: [
            pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
              children: [
                pw.Column(
                  crossAxisAlignment: pw.CrossAxisAlignment.start,
                  children: [
                    pw.Text(
                      business.name,
                      style: pw.TextStyle(fontSize: 18, fontWeight: pw.FontWeight.bold, color: PdfColors.blueGrey800),
                    ),
                    pw.Text(business.address ?? '', style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700)),
                    pw.Text('GSTIN: ${business.gstin ?? 'N/A'} | PAN: ${business.pan ?? 'N/A'}', style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700)),
                  ],
                ),
                pw.Column(
                  crossAxisAlignment: pw.CrossAxisAlignment.end,
                  children: [
                    pw.Text(reportTitle, style: pw.TextStyle(fontSize: 14, fontWeight: pw.FontWeight.bold, color: PdfColors.indigo800)),
                    pw.Text('Period: $dateRange', style: const pw.TextStyle(fontSize: 10, color: PdfColors.grey800)),
                    pw.Text('Generated: ${DateFormatter.formatDateTime(DateTime.now())}', style: const pw.TextStyle(fontSize: 8, color: PdfColors.grey600)),
                  ],
                ),
              ],
            ),
            pw.Divider(thickness: 1, color: PdfColors.grey400),
            pw.SizedBox(height: 6),
          ],
        ),
        build: (context) => [
          // Financial Summary Cards Row
          pw.Row(
            mainAxisAlignment: pw.MainAxisAlignment.spaceAround,
            children: [
              _buildPdfStatBox('Total Income', CurrencyFormatter.formatPaise(summary.totalIncomePaise), PdfColors.green700),
              _buildPdfStatBox('Total Expense', CurrencyFormatter.formatPaise(summary.totalExpensePaise), PdfColors.red700),
              _buildPdfStatBox('Net Cash Flow', CurrencyFormatter.formatPaise(summary.netCashFlowPaise), summary.netCashFlowPaise >= 0 ? PdfColors.blue700 : PdfColors.orange800),
              _buildPdfStatBox('Transfers', CurrencyFormatter.formatPaise(summary.totalTransfersPaise), PdfColors.grey700),
            ],
          ),
          pw.SizedBox(height: 12),

          // Transactions Table
          pw.TableHelper.fromTextArray(
            headers: [
              'Sr', 'Date', 'Voucher', 'Account', 'Category', 'Party', 'Mode', 'Income (INR)', 'Expense (INR)', 'Status'
            ],
            data: List<List<String>>.generate(transactions.length, (index) {
              final txn = transactions[index];
              return [
                (index + 1).toString(),
                txn.transactionDate,
                txn.voucherNumber,
                txn.accountName ?? '',
                txn.categoryName ?? '-',
                txn.partyName ?? '-',
                txn.paymentMode,
                txn.isIncome ? CurrencyFormatter.formatPaise(txn.amountPaise, includeSymbol: false) : '-',
                txn.isExpense ? CurrencyFormatter.formatPaise(txn.amountPaise, includeSymbol: false) : '-',
                txn.status.name.toUpperCase(),
              ];
            }),
            border: pw.TableBorder.all(color: PdfColors.grey300, width: 0.5),
            headerStyle: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 8, color: PdfColors.white),
            headerDecoration: const pw.BoxDecoration(color: PdfColors.blueGrey800),
            cellStyle: const pw.TextStyle(fontSize: 8),
            cellAlignment: pw.Alignment.centerLeft,
            cellAlignments: {
              0: pw.Alignment.center,
              7: pw.Alignment.centerRight,
              8: pw.Alignment.centerRight,
              9: pw.Alignment.center,
            },
          ),
        ],
        footer: (context) => pw.Row(
          mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
          children: [
            pw.Text('Cash Book Management System - Confidential', style: const pw.TextStyle(fontSize: 8, color: PdfColors.grey600)),
            pw.Text('Page ${context.pageNumber} of ${context.pagesCount}', style: const pw.TextStyle(fontSize: 8, color: PdfColors.grey600)),
          ],
        ),
      ),
    );

    return pdf.save();
  }

  static pw.Widget _buildPdfStatBox(String title, String value, PdfColor color) {
    return pw.Container(
      padding: const pw.EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: pw.BoxDecoration(
        border: pw.Border.all(color: color, width: 1),
        borderRadius: const pw.BorderRadius.all(pw.Radius.circular(4)),
      ),
      child: pw.Column(
        children: [
          pw.Text(title, style: const pw.TextStyle(fontSize: 8, color: PdfColors.grey700)),
          pw.SizedBox(height: 2),
          pw.Text(value, style: pw.TextStyle(fontSize: 12, fontWeight: pw.FontWeight.bold, color: color)),
        ],
      ),
    );
  }

  /// Generates a printable Single Voucher / Receipt PDF
  static Future<Uint8List> generateVoucherPdf({
    required BusinessModel business,
    required TransactionModel transaction,
  }) async {
    final pdf = pw.Document();
    final font = await PdfGoogleFonts.robotoRegular();
    final fontBold = await PdfGoogleFonts.robotoBold();

    final isIncome = transaction.isIncome;
    final voucherTitle = isIncome ? 'CASH / BANK RECEIPT VOUCHER' : 'PAYMENT VOUCHER';
    final amountWords = CurrencyFormatter.toWords(transaction.amountPaise);

    pdf.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(32),
        theme: pw.ThemeData.withFont(base: font, bold: fontBold),
        build: (context) => pw.Container(
          padding: const pw.EdgeInsets.all(24),
          decoration: pw.BoxDecoration(
            border: pw.Border.all(color: PdfColors.blueGrey700, width: 2),
            borderRadius: const pw.BorderRadius.all(pw.Radius.circular(8)),
          ),
          child: pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              // Business Header
              pw.Center(
                child: pw.Text(
                  business.name.toUpperCase(),
                  style: pw.TextStyle(fontSize: 20, fontWeight: pw.FontWeight.bold, color: PdfColors.blueGrey900),
                ),
              ),
              if (business.address != null)
                pw.Center(
                  child: pw.Text(business.address!, style: const pw.TextStyle(fontSize: 10, color: PdfColors.grey700)),
                ),
              pw.Center(
                child: pw.Text(
                  'Phone: ${business.phone ?? 'N/A'} | Email: ${business.email ?? 'N/A'}',
                  style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700),
                ),
              ),
              if (business.gstin != null)
                pw.Center(
                  child: pw.Text('GSTIN: ${business.gstin} | PAN: ${business.pan ?? ''}', style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey800)),
                ),
              pw.Divider(thickness: 1.5, color: PdfColors.blueGrey800),
              pw.SizedBox(height: 8),

              // Title Ribbon
              pw.Center(
                child: pw.Container(
                  padding: const pw.EdgeInsets.symmetric(horizontal: 20, vertical: 4),
                  decoration: pw.BoxDecoration(
                    color: isIncome ? PdfColors.green800 : PdfColors.red800,
                    borderRadius: const pw.BorderRadius.all(pw.Radius.circular(4)),
                  ),
                  child: pw.Text(
                    voucherTitle,
                    style: pw.TextStyle(fontSize: 12, fontWeight: pw.FontWeight.bold, color: PdfColors.white),
                  ),
                ),
              ),
              pw.SizedBox(height: 20),

              // Voucher Meta
              pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Text('Voucher No: ${transaction.voucherNumber}', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 11)),
                  pw.Text('Date: ${transaction.transactionDate}  Time: ${transaction.transactionTime}', style: const pw.TextStyle(fontSize: 11)),
                ],
              ),
              pw.SizedBox(height: 12),
              pw.Divider(thickness: 0.5, color: PdfColors.grey400),
              pw.SizedBox(height: 12),

              // Particulars Table
              pw.Row(
                children: [
                  pw.SizedBox(width: 120, child: pw.Text('Account Name:', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10))),
                  pw.Expanded(child: pw.Text(transaction.accountName ?? '', style: const pw.TextStyle(fontSize: 10))),
                ],
              ),
              pw.SizedBox(height: 8),

              pw.Row(
                children: [
                  pw.SizedBox(width: 120, child: pw.Text('Party Name:', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10))),
                  pw.Expanded(child: pw.Text(transaction.partyName ?? 'General / Cash', style: const pw.TextStyle(fontSize: 10))),
                ],
              ),
              pw.SizedBox(height: 8),

              pw.Row(
                children: [
                  pw.SizedBox(width: 120, child: pw.Text('Category:', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10))),
                  pw.Expanded(child: pw.Text(transaction.categoryName ?? '-', style: const pw.TextStyle(fontSize: 10))),
                ],
              ),
              pw.SizedBox(height: 8),

              pw.Row(
                children: [
                  pw.SizedBox(width: 120, child: pw.Text('Payment Mode:', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10))),
                  pw.Expanded(child: pw.Text('${transaction.paymentMode} ${transaction.referenceNumber != null ? "(Ref: ${transaction.referenceNumber})" : ""}', style: const pw.TextStyle(fontSize: 10))),
                ],
              ),
              pw.SizedBox(height: 8),

              pw.Row(
                crossAxisAlignment: pw.CrossAxisAlignment.start,
                children: [
                  pw.SizedBox(width: 120, child: pw.Text('Description / Narration:', style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 10))),
                  pw.Expanded(child: pw.Text(transaction.description ?? '-', style: const pw.TextStyle(fontSize: 10))),
                ],
              ),
              pw.SizedBox(height: 20),

              // Amount Highlight Box
              pw.Container(
                padding: const pw.EdgeInsets.all(12),
                decoration: pw.BoxDecoration(
                  color: PdfColors.grey100,
                  border: pw.Border.all(color: PdfColors.grey400),
                  borderRadius: const pw.BorderRadius.all(pw.Radius.circular(6)),
                ),
                child: pw.Column(
                  crossAxisAlignment: pw.CrossAxisAlignment.start,
                  children: [
                    pw.Row(
                      mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                      children: [
                        pw.Text('AMOUNT (INR):', style: pw.TextStyle(fontSize: 14, fontWeight: pw.FontWeight.bold)),
                        pw.Text(
                          CurrencyFormatter.formatPaise(transaction.amountPaise),
                          style: pw.TextStyle(fontSize: 16, fontWeight: pw.FontWeight.bold, color: isIncome ? PdfColors.green900 : PdfColors.red900),
                        ),
                      ],
                    ),
                    pw.SizedBox(height: 4),
                    pw.Text('Amount in Words: $amountWords', style: pw.TextStyle(fontSize: 10, fontStyle: pw.FontStyle.italic, color: PdfColors.grey800)),
                  ],
                ),
              ),

              pw.Spacer(),

              // Signatures Area
              pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Column(
                    children: [
                      pw.Container(width: 160, height: 1, color: PdfColors.grey700),
                      pw.SizedBox(height: 4),
                      pw.Text('Receiver / Payee Signature', style: const pw.TextStyle(fontSize: 9)),
                    ],
                  ),
                  pw.Column(
                    children: [
                      pw.Container(width: 160, height: 1, color: PdfColors.grey700),
                      pw.SizedBox(height: 4),
                      pw.Text('Authorized Signatory (${business.name})', style: const pw.TextStyle(fontSize: 9)),
                    ],
                  ),
                ],
              ),
              pw.SizedBox(height: 12),
              pw.Center(
                child: pw.Text('This is a computer-generated voucher issued by Cash Book Management System.', style: const pw.TextStyle(fontSize: 7, color: PdfColors.grey600)),
              ),
            ],
          ),
        ),
      ),
    );

    return pdf.save();
  }

  /// Exports transactions to CSV format
  static String generateCsvReport(List<TransactionModel> transactions) {
    final List<List<dynamic>> rows = [];
    rows.add([
      'Date',
      'Voucher No',
      'Type',
      'Account',
      'Category',
      'Party',
      'Mode',
      'Amount (Paise)',
      'Income (INR)',
      'Expense (INR)',
      'Status',
      'Description',
    ]);

    for (final txn in transactions) {
      rows.add([
        txn.transactionDate,
        txn.voucherNumber,
        txn.transactionType.displayName,
        txn.accountName ?? '',
        txn.categoryName ?? '',
        txn.partyName ?? '',
        txn.paymentMode,
        txn.amountPaise,
        txn.isIncome ? (txn.amountPaise / 100.0).toStringAsFixed(2) : '0.00',
        txn.isExpense ? (txn.amountPaise / 100.0).toStringAsFixed(2) : '0.00',
        txn.status.name,
        txn.description ?? '',
      ]);
    }

    return const ListToCsvConverter().convert(rows);
  }
}

