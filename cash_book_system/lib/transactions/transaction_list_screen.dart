import 'package:flutter/material.dart';
import 'package:printing/printing.dart';
import 'package:provider/provider.dart';

import '../core/constants/enums.dart';
import '../core/utils/currency_formatter.dart';
import '../models/transaction_model.dart';
import '../services/accounting_service.dart';
import '../services/auth_service.dart';
import '../services/report_service.dart';
import 'transaction_entry_screen.dart';

class TransactionListScreen extends StatefulWidget {
  final VoidCallback onOpenNewEntry;

  const TransactionListScreen({super.key, required this.onOpenNewEntry});

  @override
  State<TransactionListScreen> createState() => _TransactionListScreenState();
}

class _TransactionListScreenState extends State<TransactionListScreen> {
  final _searchController = TextEditingController();
  String? _selectedAccountId;
  String? _selectedTypeFilter; // 'ALL', 'INCOME', 'EXPENSE', 'TRANSFER'
  String _searchQuery = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final accounting = context.watch<AccountingService>();
    final auth = context.watch<AuthService>();
    final isDesktop = MediaQuery.of(context).size.width >= 900;

    // Filter transactions
    var filtered = accounting.transactions.where((txn) {
      if (_selectedAccountId != null &&
          txn.accountId != _selectedAccountId &&
          txn.targetAccountId != _selectedAccountId) {
        return false;
      }

      if (_selectedTypeFilter != null && _selectedTypeFilter != 'ALL') {
        if (_selectedTypeFilter == 'INCOME' && !txn.isIncome) return false;
        if (_selectedTypeFilter == 'EXPENSE' && !txn.isExpense) return false;
        if (_selectedTypeFilter == 'TRANSFER' && !txn.isTransfer) return false;
      }

      if (_searchQuery.isNotEmpty) {
        final q = _searchQuery.toLowerCase();
        final matchVoucher = txn.voucherNumber.toLowerCase().contains(q);
        final matchParty = (txn.partyName ?? '').toLowerCase().contains(q);
        final matchDesc = (txn.description ?? '').toLowerCase().contains(q);
        final matchAmount = CurrencyFormatter.formatPaise(txn.amountPaise).contains(q);
        final matchDate = txn.transactionDate.contains(q);

        if (!matchVoucher && !matchParty && !matchDesc && !matchAmount && !matchDate) {
          return false;
        }
      }

      return true;
    }).toList();

    return Scaffold(
      body: Padding(
        padding: EdgeInsets.all(isDesktop ? 24 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Toolbar & Filters
            _buildFilterToolbar(context, accounting, isDesktop),
            const SizedBox(height: 16),

            // Results Count & Summary Strip
            _buildResultsSummaryStrip(filtered),
            const SizedBox(height: 12),

            // Transactions View
            Expanded(
              child: filtered.isEmpty
                  ? const Center(
                      child: Text('No transactions match the selected filters.'),
                    )
                  : (isDesktop
                      ? _buildDesktopDataTable(context, filtered, auth)
                      : _buildMobileListView(context, filtered, auth)),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: widget.onOpenNewEntry,
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('New Entry (Ctrl+N)', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
    );
  }

  Widget _buildFilterToolbar(
    BuildContext context,
    AccountingService accounting,
    bool isDesktop,
  ) {
    return Row(
      children: [
        // Global Search Box
        Expanded(
          flex: 2,
          child: TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Search by voucher, party, amount, date...',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _searchQuery.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear),
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _searchQuery = '');
                      },
                    )
                  : null,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
            ),
            onChanged: (val) {
              setState(() => _searchQuery = val.trim());
            },
          ),
        ),
        const SizedBox(width: 12),

        // Type Filter Dropdown
        DropdownButton<String>(
          value: _selectedTypeFilter ?? 'ALL',
          items: const [
            DropdownMenuItem(value: 'ALL', child: Text('All Types')),
            DropdownMenuItem(value: 'INCOME', child: Text('Income Only')),
            DropdownMenuItem(value: 'EXPENSE', child: Text('Expenses Only')),
            DropdownMenuItem(value: 'TRANSFER', child: Text('Transfers Only')),
          ],
          onChanged: (val) {
            setState(() => _selectedTypeFilter = val);
          },
        ),
        const SizedBox(width: 12),

        // Account Filter Dropdown
        if (isDesktop)
          DropdownButton<String?>(
            value: _selectedAccountId,
            hint: const Text('All Accounts'),
            items: [
              const DropdownMenuItem(value: null, child: Text('All Accounts')),
              ...accounting.accounts.map((a) => DropdownMenuItem(value: a.id, child: Text(a.accountName))),
            ],
            onChanged: (val) {
              setState(() => _selectedAccountId = val);
            },
          ),
      ],
    );
  }

  Widget _buildResultsSummaryStrip(List<TransactionModel> filtered) {
    int inc = 0;
    int exp = 0;
    for (final t in filtered) {
      if (t.status == TransactionStatus.posted) {
        if (t.isIncome) inc += t.amountPaise;
        if (t.isExpense) exp += t.amountPaise;
      }
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceVariant.withOpacity(0.4),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            'Showing ${filtered.length} transactions',
            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
          ),
          Row(
            children: [
              Text(
                'Income: ${CurrencyFormatter.formatPaise(inc)}',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.green),
              ),
              const SizedBox(width: 16),
              Text(
                'Expense: ${CurrencyFormatter.formatPaise(exp)}',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.red),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDesktopDataTable(
    BuildContext context,
    List<TransactionModel> txns,
    AuthService auth,
  ) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: Colors.grey.shade300),
      ),
      child: SingleChildScrollView(
        child: SizedBox(
          width: double.infinity,
          child: DataTable(
            headingRowColor: MaterialStateProperty.all(Colors.grey.shade100),
            columns: const [
              DataColumn(label: Text('Date & Time')),
              DataColumn(label: Text('Voucher No.')),
              DataColumn(label: Text('Account')),
              DataColumn(label: Text('Party / Details')),
              DataColumn(label: Text('Mode')),
              DataColumn(label: Text('Amount (INR)')),
              DataColumn(label: Text('Status')),
              DataColumn(label: Text('Actions')),
            ],
            rows: txns.map((txn) {
              final isIncome = txn.isIncome;
              final isTransfer = txn.isTransfer;
              final isReversed = txn.isReversed;

              Color color = isIncome ? Colors.green[800]! : Colors.red[800]!;
              if (isTransfer) color = Colors.blue[800]!;
              if (isReversed) color = Colors.grey;

              return DataRow(
                cells: [
                  DataCell(Text('${txn.transactionDate}\n${txn.transactionTime}', style: const TextStyle(fontSize: 11))),
                  DataCell(Text(txn.voucherNumber, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12))),
                  DataCell(Text(txn.accountName ?? '', style: const TextStyle(fontSize: 12))),
                  DataCell(Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(txn.partyName ?? '-', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12)),
                      if (txn.categoryName != null)
                        Text(txn.categoryName!, style: TextStyle(fontSize: 10, color: Colors.grey[600])),
                    ],
                  )),
                  DataCell(Text(txn.paymentMode, style: const TextStyle(fontSize: 11))),
                  DataCell(Text(
                    '${isIncome ? "+" : (isTransfer ? "⇄ " : "-")}${CurrencyFormatter.formatPaise(txn.amountPaise)}',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: color,
                      decoration: isReversed ? TextDecoration.lineThrough : null,
                    ),
                  )),
                  DataCell(_buildStatusBadge(txn.status)),
                  DataCell(Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.print, size: 18),
                        tooltip: 'Print Voucher Receipt',
                        onPressed: () => _printVoucher(context, auth, txn),
                      ),
                      if (!isReversed && auth.currentRole.canReverseTransactions)
                        IconButton(
                          icon: const Icon(Icons.undo, size: 18, color: Colors.orange),
                          tooltip: 'Reverse Transaction',
                          onPressed: () => _promptReverseTransaction(context, txn),
                        ),
                    ],
                  )),
                ],
              );
            }).toList(),
          ),
        ),
      ),
    );
  }

  Widget _buildMobileListView(
    BuildContext context,
    List<TransactionModel> txns,
    AuthService auth,
  ) {
    return ListView.separated(
      itemCount: txns.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (ctx, index) {
        final txn = txns[index];
        final isIncome = txn.isIncome;
        final isTransfer = txn.isTransfer;
        final isReversed = txn.isReversed;

        Color color = isIncome ? Colors.green[800]! : Colors.red[800]!;
        if (isTransfer) color = Colors.blue[800]!;
        if (isReversed) color = Colors.grey;

        return Card(
          elevation: 1,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: color.withOpacity(0.12),
              child: Icon(
                isTransfer ? Icons.swap_horiz : (isIncome ? Icons.arrow_downward : Icons.arrow_upward),
                color: color,
              ),
            ),
            title: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(txn.voucherNumber, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                Text(
                  '${isIncome ? "+" : (isTransfer ? "⇄ " : "-")}${CurrencyFormatter.formatPaise(txn.amountPaise)}',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: color,
                    decoration: isReversed ? TextDecoration.lineThrough : null,
                  ),
                ),
              ],
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Text(
                  '${txn.partyName ?? "General"} • ${txn.accountName ?? ""}',
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                ),
                Text(
                  '${txn.transactionDate} ${txn.transactionTime} | ${txn.paymentMode}',
                  style: TextStyle(fontSize: 11, color: Colors.grey[600]),
                ),
              ],
            ),
            trailing: IconButton(
              icon: const Icon(Icons.more_vert),
              onPressed: () => _showTransactionActionsModal(context, auth, txn),
            ),
          ),
        );
      },
    );
  }

  Widget _buildStatusBadge(TransactionStatus status) {
    Color bg = Colors.green.shade50;
    Color fg = Colors.green.shade800;
    String label = 'POSTED';

    if (status == TransactionStatus.reversed) {
      bg = Colors.red.shade50;
      fg = Colors.red.shade800;
      label = 'REVERSED';
    } else if (status == TransactionStatus.draft) {
      bg = Colors.amber.shade50;
      fg = Colors.amber.shade900;
      label = 'DRAFT';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: fg.withOpacity(0.3)),
      ),
      child: Text(
        label,
        style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: fg),
      ),
    );
  }

  void _showTransactionActionsModal(BuildContext context, AuthService auth, TransactionModel txn) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Voucher: ${txn.voucherNumber}',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const Divider(),
              ListTile(
                leading: const Icon(Icons.edit_note, color: Colors.amber),
                title: const Text('Edit Transaction Entry'),
                subtitle: const Text('Modify amount, account, party or remarks'),
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => TransactionEntryScreen(existingTransaction: txn),
                    ),
                  );
                },
              ),
              ListTile(
                leading: const Icon(Icons.delete_outline, color: Colors.redAccent),
                title: const Text('Delete Transaction Entry'),
                subtitle: const Text('Permanently remove voucher and rollback balances'),
                onTap: () {
                  Navigator.pop(ctx);
                  _promptDeleteTransaction(context, txn);
                },
              ),
              ListTile(
                leading: const Icon(Icons.print, color: Colors.indigo),
                title: const Text('Print Voucher / Receipt'),
                onTap: () {
                  Navigator.pop(ctx);
                  _printVoucher(context, auth, txn);
                },
              ),
              if (!txn.isReversed && auth.currentRole.canReverseTransactions)
                ListTile(
                  leading: const Icon(Icons.undo, color: Colors.deepOrange),
                  title: const Text('Reverse This Transaction'),
                  subtitle: const Text('Creates an offsetting reversal entry with audit history'),
                  onTap: () {
                    Navigator.pop(ctx);
                    _promptReverseTransaction(context, txn);
                  },
                ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _printVoucher(BuildContext context, AuthService auth, TransactionModel txn) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    final pdfBytes = await ReportService.generateVoucherPdf(business: biz, transaction: txn);
    await Printing.layoutPdf(
      onLayout: (format) async => pdfBytes,
      name: 'Voucher_${txn.voucherNumber}.pdf',
    );
  }

  void _promptReverseTransaction(BuildContext context, TransactionModel txn) {
    final reasonController = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Reverse Voucher ${txn.voucherNumber}?'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Financial records cannot be deleted. Reversing will create a compensating offset entry and restore account balances.',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: reasonController,
              decoration: const InputDecoration(
                labelText: 'Reason for correction / reversal *',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red[700], foregroundColor: Colors.white),
            onPressed: () async {
              final reason = reasonController.text.trim();
              if (reason.isEmpty) return;

              Navigator.pop(ctx);

              final accounting = context.read<AccountingService>();
              final auth = context.read<AuthService>();
              final user = auth.currentUser;

              final res = await accounting.reverseTransaction(
                originalTransaction: txn,
                reason: reason,
                reversedBy: user?.id ?? 'usr_current',
                reversedByName: user?.displayName ?? 'Operator',
                deviceId: 'dev_win_client',
              );

              if (mounted) {
                if (res.success) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Transaction reversed successfully and balances restored!')),
                  );
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(backgroundColor: Colors.red, content: Text(res.error ?? 'Error reversing transaction')),
                  );
                }
              }
            },
            child: const Text('Confirm Reversal'),
          ),
        ],
      ),
    );
  }

  void _promptDeleteTransaction(BuildContext context, TransactionModel txn) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Delete Voucher ${txn.voucherNumber}?'),
        content: Text(
          'Are you sure you want to permanently delete this voucher for ₹${(txn.amountPaise / 100.0).toStringAsFixed(2)}?\n\n'
          'All account and party balances will be automatically recalculated.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red[700], foregroundColor: Colors.white),
            onPressed: () async {
              Navigator.pop(ctx);
              final accounting = context.read<AccountingService>();
              final result = await accounting.deleteTransaction(transactionId: txn.id);
              if (mounted) {
                if (result.success) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Voucher ${txn.voucherNumber} permanently deleted.'),
                      backgroundColor: Colors.red[700],
                    ),
                  );
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Delete failed: ${result.error}'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              }
            },
            child: const Text('Delete Permanently'),
          ),
        ],
      ),
    );
  }
}

