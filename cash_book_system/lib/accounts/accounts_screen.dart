import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../accounting/financial_engine.dart';
import '../core/constants/enums.dart';
import '../core/utils/currency_formatter.dart';
import '../core/utils/date_formatter.dart';
import '../models/account_model.dart';
import '../services/accounting_service.dart';

class AccountsScreen extends StatelessWidget {
  final VoidCallback onOpenTransfer;

  const AccountsScreen({super.key, required this.onOpenTransfer});

  @override
  Widget build(BuildContext context) {
    final accounting = context.watch<AccountingService>();
    final isDesktop = MediaQuery.of(context).size.width >= 900;

    final cashAccounts = accounting.accounts.where((a) => a.accountType == AccountType.cash).toList();
    final bankAccounts = accounting.accounts.where((a) => a.accountType == AccountType.bank).toList();

    return Scaffold(
      body: SingleChildScrollView(
        padding: EdgeInsets.all(isDesktop ? 24 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Toolbar with Add Account and Transfer buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Accounts & Ledgers',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                Row(
                  children: [
                    OutlinedButton.icon(
                      onPressed: onOpenTransfer,
                      icon: const Icon(Icons.swap_horiz),
                      label: const Text('Transfer (Cash<->Bank)'),
                    ),
                    const SizedBox(width: 12),
                    ElevatedButton.icon(
                      onPressed: () => _showAddAccountDialog(context),
                      icon: const Icon(Icons.add),
                      label: const Text('Add Account'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Theme.of(context).colorScheme.primary,
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Cash Accounts Section
            _buildAccountSection(context, 'Cash in Hand Accounts', cashAccounts, Colors.teal, isDesktop),
            const SizedBox(height: 28),

            // Bank Accounts Section
            _buildAccountSection(context, 'Bank Accounts', bankAccounts, Colors.blueAccent, isDesktop),
          ],
        ),
      ),
    );
  }

  Widget _buildAccountSection(
    BuildContext context,
    String title,
    List<AccountModel> accounts,
    Color themeColor,
    bool isDesktop,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            CircleAvatar(radius: 6, backgroundColor: themeColor),
            const SizedBox(width: 8),
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(width: 8),
            Text('(${accounts.length})', style: const TextStyle(color: Colors.grey, fontSize: 13)),
          ],
        ),
        const SizedBox(height: 12),
        if (accounts.isEmpty)
          const Card(
            child: Padding(
              padding: EdgeInsets.all(20),
              child: Center(child: Text('No accounts in this category.')),
            ),
          )
        else if (isDesktop)
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 2.1,
            ),
            itemCount: accounts.length,
            itemBuilder: (ctx, idx) => _buildAccountCard(context, accounts[idx], themeColor),
          )
        else
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: accounts.length,
            separatorBuilder: (_, __) => const SizedBox(height: 12),
            itemBuilder: (ctx, idx) => _buildAccountCard(context, accounts[idx], themeColor),
          ),
      ],
    );
  }

  Widget _buildAccountCard(BuildContext context, AccountModel account, Color color) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () => _showAccountStatementModal(context, account),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      account.accountName,
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  Icon(
                    account.accountType == AccountType.cash ? Icons.payments : Icons.account_balance,
                    color: color,
                    size: 20,
                  ),
                ],
              ),
              if (account.bankName != null)
                Text(
                  '${account.bankName} • ${account.accountNumber ?? ""}',
                  style: TextStyle(fontSize: 11, color: Colors.grey[600]),
                  overflow: TextOverflow.ellipsis,
                ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Current Balance', style: TextStyle(fontSize: 10, color: Colors.grey)),
                      Text(
                        CurrencyFormatter.formatPaise(account.currentBalancePaise),
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: account.currentBalancePaise >= 0 ? color : Colors.red,
                        ),
                      ),
                    ],
                  ),
                  TextButton(
                    onPressed: () => _showAccountStatementModal(context, account),
                    child: const Text('Statement >', style: TextStyle(fontSize: 11)),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showAddAccountDialog(BuildContext context) {
    final nameController = TextEditingController();
    final balanceController = TextEditingController(text: '0');
    final bankNameController = TextEditingController();
    final accNoController = TextEditingController();
    final ifscController = TextEditingController();

    AccountType selectedType = AccountType.cash;
    bool allowNegative = false;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Add New Account'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<AccountType>(
                  value: selectedType,
                  decoration: const InputDecoration(labelText: 'Account Type', border: OutlineInputBorder()),
                  items: const [
                    DropdownMenuItem(value: AccountType.cash, child: Text('Cash Account')),
                    DropdownMenuItem(value: AccountType.bank, child: Text('Bank Account')),
                  ],
                  onChanged: (val) {
                    if (val != null) setDialogState(() => selectedType = val);
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nameController,
                  decoration: const InputDecoration(labelText: 'Account Name *', border: OutlineInputBorder()),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: balanceController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Opening Balance (₹)', prefixText: '₹ ', border: OutlineInputBorder()),
                ),
                if (selectedType == AccountType.bank) ...[
                  const SizedBox(height: 12),
                  TextField(controller: bankNameController, decoration: const InputDecoration(labelText: 'Bank Name', border: OutlineInputBorder())),
                  const SizedBox(height: 12),
                  TextField(controller: accNoController, decoration: const InputDecoration(labelText: 'Account Number', border: OutlineInputBorder())),
                  const SizedBox(height: 12),
                  TextField(controller: ifscController, decoration: const InputDecoration(labelText: 'IFSC Code', border: OutlineInputBorder())),
                ],
                const SizedBox(height: 8),
                CheckboxListTile(
                  title: const Text('Allow Negative Balance', style: TextStyle(fontSize: 13)),
                  value: allowNegative,
                  onChanged: (val) => setDialogState(() => allowNegative = val ?? false),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: () async {
                final name = nameController.text.trim();
                if (name.isEmpty) return;

                final openingPaise = CurrencyFormatter.parseToPaise(balanceController.text);
                final now = DateTime.now();
                final accounting = context.read<AccountingService>();

                final newAccount = AccountModel(
                  id: 'acc_${DateTime.now().millisecondsSinceEpoch}',
                  businessId: 'biz_shivay_001',
                  accountName: name,
                  accountType: selectedType,
                  openingBalancePaise: openingPaise,
                  openingDate: DateFormatter.formatDate(now),
                  currentBalancePaise: openingPaise,
                  bankName: selectedType == AccountType.bank ? bankNameController.text.trim() : null,
                  accountNumber: selectedType == AccountType.bank ? accNoController.text.trim() : null,
                  ifscCode: selectedType == AccountType.bank ? ifscController.text.trim() : null,
                  allowNegativeBalance: allowNegative,
                  createdAt: now,
                  updatedAt: now,
                );

                await accounting.addAccount(newAccount);
                if (context.mounted) Navigator.pop(ctx);
              },
              child: const Text('Create Account'),
            ),
          ],
        ),
      ),
    );
  }

  void _showAccountStatementModal(BuildContext context, AccountModel account) {
    final accounting = context.read<AccountingService>();
    final txns = accounting.transactions
        .where((t) => t.accountId == account.id || t.targetAccountId == account.id)
        .toList();

    final lines = FinancialEngine.computeLedgerWithRunningBalances(
      initialOpeningBalancePaise: account.openingBalancePaise,
      transactions: txns.reversed.toList(),
    );

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.75,
        maxChildSize: 0.95,
        minChildSize: 0.5,
        expand: false,
        builder: (_, scrollController) => Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        account.accountName,
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      Text(
                        'Opening: ${CurrencyFormatter.formatPaise(account.openingBalancePaise)}  |  Current: ${CurrencyFormatter.formatPaise(account.currentBalancePaise)}',
                        style: TextStyle(fontSize: 12, color: Colors.grey[700]),
                      ),
                    ],
                  ),
                  IconButton(onPressed: () => Navigator.pop(ctx), icon: const Icon(Icons.close)),
                ],
              ),
              const Divider(),
              const SizedBox(height: 8),
              Expanded(
                child: lines.isEmpty
                  ? const Center(child: Text('No transactions recorded for this account.'))
                  : ListView.separated(
                      controller: scrollController,
                      itemCount: lines.length,
                      separatorBuilder: (_, __) => const Divider(height: 1),
                      itemBuilder: (_, index) {
                        final line = lines[index];
                        final t = line.transaction;

                        return ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('${t.transactionDate} • ${t.voucherNumber}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                              Text(
                                CurrencyFormatter.formatPaise(line.runningBalancePaise),
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.indigo),
                              ),
                            ],
                          ),
                          subtitle: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(t.partyName ?? t.description ?? t.transactionType.displayName, style: const TextStyle(fontSize: 11)),
                              Text(
                                line.creditPaise > 0
                                    ? '+${CurrencyFormatter.formatPaise(line.creditPaise)}'
                                    : '-${CurrencyFormatter.formatPaise(line.debitPaise)}',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w600,
                                  color: line.creditPaise > 0 ? Colors.green : Colors.red,
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

