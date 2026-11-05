import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:printing/printing.dart';
import 'package:provider/provider.dart';

import '../accounting/financial_engine.dart';
import '../core/utils/currency_formatter.dart';
import '../core/utils/date_formatter.dart';
import '../models/transaction_model.dart';
import '../services/accounting_service.dart';
import '../services/auth_service.dart';
import '../services/report_service.dart';

class ReportsScreen extends StatefulWidget {
  const ReportsScreen({super.key});

  @override
  State<ReportsScreen> createState() => _ReportsScreenState();
}

class _ReportsScreenState extends State<ReportsScreen> {
  String _selectedReportType = 'Daily Cash Book';
  String _dateFilterPreset = 'This Month';
  String? _selectedAccountId;
  bool _isGenerating = false;

  final List<String> _reportTypes = [
    'Daily Cash Book',
    'Monthly Cash Book',
    'Full Transaction Register',
    'Income Report',
    'Expense Report',
    'Category-wise Breakdown',
    'Account Statement',
  ];

  final List<String> _datePresets = [
    'Today',
    'Yesterday',
    'This Month',
    'Last Month',
    'Financial Year',
  ];

  @override
  Widget build(BuildContext context) {
    final accounting = context.watch<AccountingService>();
    final auth = context.watch<AuthService>();
    final isDesktop = MediaQuery.of(context).size.width >= 900;

    final filteredTxns = _getFilteredTransactions(accounting.transactions);
    final summary = FinancialEngine.calculateSummary(filteredTxns);

    return Scaffold(
      body: SingleChildScrollView(
        padding: EdgeInsets.all(isDesktop ? 24 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Title & Export Action Buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Financial Reports & Statements',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                Wrap(
                  spacing: 8,
                  children: [
                    ElevatedButton.icon(
                      onPressed: _isGenerating ? null : () => _exportExcel(context, auth, filteredTxns),
                      icon: const Icon(Icons.table_chart, color: Colors.green),
                      label: const Text('Export Excel (.xlsx)'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green.shade50,
                        foregroundColor: Colors.green.shade900,
                      ),
                    ),
                    ElevatedButton.icon(
                      onPressed: _isGenerating ? null : () => _printOrExportPdf(context, auth, filteredTxns),
                      icon: const Icon(Icons.picture_as_pdf, color: Colors.red),
                      label: const Text('PDF / Print'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.red.shade50,
                        foregroundColor: Colors.red.shade900,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Controls Card
            Card(
              elevation: 1,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: DropdownButtonFormField<String>(
                            value: _selectedReportType,
                            decoration: const InputDecoration(labelText: 'Report Type', border: OutlineInputBorder()),
                            items: _reportTypes.map((r) => DropdownMenuItem(value: r, child: Text(r))).toList(),
                            onChanged: (val) {
                              if (val != null) setState(() => _selectedReportType = val);
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: DropdownButtonFormField<String>(
                            value: _dateFilterPreset,
                            decoration: const InputDecoration(labelText: 'Date Range Preset', border: OutlineInputBorder()),
                            items: _datePresets.map((p) => DropdownMenuItem(value: p, child: Text(p))).toList(),
                            onChanged: (val) {
                              if (val != null) setState(() => _dateFilterPreset = val);
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: DropdownButtonFormField<String?>(
                            value: _selectedAccountId,
                            decoration: const InputDecoration(labelText: 'Account Filter', border: OutlineInputBorder()),
                            items: [
                              const DropdownMenuItem(value: null, child: Text('All Accounts')),
                              ...accounting.accounts.map((a) => DropdownMenuItem(value: a.id, child: Text(a.accountName))),
                            ],
                            onChanged: (val) {
                              setState(() => _selectedAccountId = val);
                            },
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Summary Bar
            _buildSummaryRow(summary),
            const SizedBox(height: 20),

            // Report Preview Data Table
            _buildReportPreview(filteredTxns, isDesktop),
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryRow(FinancialSummary summary) {
    return Row(
      children: [
        _buildStatTile('Total Income', CurrencyFormatter.formatPaise(summary.totalIncomePaise), Colors.green),
        const SizedBox(width: 12),
        _buildStatTile('Total Expense', CurrencyFormatter.formatPaise(summary.totalExpensePaise), Colors.red),
        const SizedBox(width: 12),
        _buildStatTile('Net Cash Flow', CurrencyFormatter.formatPaise(summary.netCashFlowPaise), Colors.indigo),
        const SizedBox(width: 12),
        _buildStatTile('Total Transfers', CurrencyFormatter.formatPaise(summary.totalTransfersPaise), Colors.grey),
      ],
    );
  }

  Widget _buildStatTile(String label, String value, Color color) {
    return Expanded(
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(color: color.withOpacity(0.3)),
        ),
        color: color.withOpacity(0.05),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(fontSize: 11, color: Colors.grey[700])),
              const SizedBox(height: 4),
              Text(value, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: color)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildReportPreview(List<TransactionModel> txns, bool isDesktop) {
    if (txns.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(40),
          child: Center(child: Text('No transactions found for the selected period.')),
        ),
      );
    }

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: SingleChildScrollView(
        child: SizedBox(
          width: double.infinity,
          child: DataTable(
            headingRowColor: MaterialStateProperty.all(Colors.grey.shade100),
            columns: const [
              DataColumn(label: Text('Date')),
              DataColumn(label: Text('Voucher')),
              DataColumn(label: Text('Particulars / Account')),
              DataColumn(label: Text('Party')),
              DataColumn(label: Text('Income (INR)')),
              DataColumn(label: Text('Expense (INR)')),
              DataColumn(label: Text('Mode')),
            ],
            rows: txns.map((t) {
              return DataRow(cells: [
                DataCell(Text(t.transactionDate, style: const TextStyle(fontSize: 12))),
                DataCell(Text(t.voucherNumber, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12))),
                DataCell(Text('${t.accountName ?? ""} ${t.categoryName != null ? "• " + t.categoryName! : ""}', style: const TextStyle(fontSize: 12))),
                DataCell(Text(t.partyName ?? '-', style: const TextStyle(fontSize: 12))),
                DataCell(Text(
                  t.isIncome ? CurrencyFormatter.formatPaise(t.amountPaise) : '-',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green, fontSize: 12),
                )),
                DataCell(Text(
                  t.isExpense ? CurrencyFormatter.formatPaise(t.amountPaise) : '-',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red, fontSize: 12),
                )),
                DataCell(Text(t.paymentMode, style: const TextStyle(fontSize: 11))),
              ]);
            }).toList(),
          ),
        ),
      ),
    );
  }

  List<TransactionModel> _getFilteredTransactions(List<TransactionModel> all) {
    final now = DateTime.now();
    final todayStr = DateFormatter.formatDate(now);
    final monthStr = '-${now.month.toString().padLeft(2, '0')}-${now.year}';

    return all.where((t) {
      if (_selectedAccountId != null && t.accountId != _selectedAccountId && t.targetAccountId != _selectedAccountId) {
        return false;
      }

      if (_selectedReportType == 'Income Report' && !t.isIncome) return false;
      if (_selectedReportType == 'Expense Report' && !t.isExpense) return false;

      if (_dateFilterPreset == 'Today') {
        return t.transactionDate == todayStr;
      } else if (_dateFilterPreset == 'This Month') {
        return t.transactionDate.endsWith(monthStr);
      }

      return true;
    }).toList();
  }

  Future<void> _exportExcel(
    BuildContext context,
    AuthService auth,
    List<TransactionModel> txns,
  ) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    setState(() => _isGenerating = true);
    try {
      final bytes = await ReportService.generateExcelReport(
        business: biz,
        reportTitle: _selectedReportType,
        dateRange: _dateFilterPreset,
        transactions: txns,
      );

      // Save locally
      final dir = await getApplicationDocumentsDirectory();
      final fileName = '${biz.name.replaceAll(" ", "_")}_Report_${DateTime.now().millisecondsSinceEpoch}.xlsx';
      final file = File('${dir.path}/$fileName');
      await file.writeAsBytes(bytes);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: Colors.green[800],
            content: Text('Excel report exported successfully!\nSaved to: ${file.path}'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: Colors.red, content: Text('Error generating Excel: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  Future<void> _printOrExportPdf(
    BuildContext context,
    AuthService auth,
    List<TransactionModel> txns,
  ) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    setState(() => _isGenerating = true);
    try {
      final pdfBytes = await ReportService.generatePdfReport(
        business: biz,
        reportTitle: _selectedReportType,
        dateRange: _dateFilterPreset,
        transactions: txns,
      );

      await Printing.layoutPdf(
        onLayout: (format) async => pdfBytes,
        name: '${_selectedReportType.replaceAll(" ", "_")}.pdf',
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: Colors.red, content: Text('Error printing PDF: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }
}

