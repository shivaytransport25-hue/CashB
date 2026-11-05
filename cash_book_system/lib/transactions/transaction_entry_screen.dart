import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/animations/bounce_button.dart';
import '../core/constants/enums.dart';
import '../core/utils/currency_formatter.dart';
import '../core/utils/date_formatter.dart';
import '../models/party_model.dart';
import '../models/transaction_model.dart';
import '../services/accounting_service.dart';
import '../services/auth_service.dart';

class TransactionEntryScreen extends StatefulWidget {
  final int initialTabIndex; // 0: Income, 1: Expense, 2: Transfer
  final TransactionModel? existingTransaction;

  const TransactionEntryScreen({
    super.key,
    this.initialTabIndex = 0,
    this.existingTransaction,
  });

  @override
  State<TransactionEntryScreen> createState() => _TransactionEntryScreenState();
}

class _TransactionEntryScreenState extends State<TransactionEntryScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  // Form Controllers
  final _amountController = TextEditingController();
  final _narrationController = TextEditingController();
  final _refController = TextEditingController();
  final _partyTextController = TextEditingController();
  final _partyFocusNode = FocusNode();

  String? _selectedAccountId;
  String? _selectedTargetAccountId; // For transfer
  String? _selectedCategoryId;
  String? _selectedPartyId;
  String _paymentMode = 'Cash';
  DateTime _selectedDate = DateTime.now();
  bool _isSubmitting = false;

  final List<String> _paymentModes = ['Cash', 'UPI', 'NEFT/RTGS', 'Cheque', 'Card'];

  @override
  void initState() {
    super.initState();
    final initialIdx = widget.existingTransaction != null
        ? (widget.existingTransaction!.isIncome ? 0 : (widget.existingTransaction!.isExpense ? 1 : 2))
        : widget.initialTabIndex;

    _tabController = TabController(
      length: 3,
      vsync: this,
      initialIndex: initialIdx,
    );
    _tabController.addListener(_onTabChanged);

    if (widget.existingTransaction != null) {
      final t = widget.existingTransaction!;
      _amountController.text = (t.amountPaise / 100.0).toStringAsFixed(2);
      _narrationController.text = t.narration;
      _refController.text = t.referenceNumber ?? '';
      _selectedAccountId = t.accountId;
      _selectedPartyId = t.partyId;
      _selectedCategoryId = t.categoryId;
      _paymentMode = t.paymentMode ?? 'Cash';
      _partyTextController.text = t.partyName ?? '';
    }

    WidgetsBinding.instance.addPostFrameCallback((_) {
      final accounting = context.read<AccountingService>();
      if (accounting.accounts.isNotEmpty && _selectedAccountId == null) {
        setState(() {
          _selectedAccountId = accounting.accounts.first.id;
          if (accounting.accounts.length > 1) {
            _selectedTargetAccountId = accounting.accounts[1].id;
          }
        });
      }
      if (accounting.categories.isNotEmpty) {
        setState(() {
          _selectedCategoryId = accounting.categories.first.id;
        });
      }
    });
  }

  void _onTabChanged() {
    if (_tabController.indexIsChanging) {
      _amountController.clear();
      _narrationController.clear();
      _refController.clear();
      _partyTextController.clear();
      _selectedPartyId = null;
      setState(() {});
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    _amountController.dispose();
    _narrationController.dispose();
    _refController.dispose();
    _partyTextController.dispose();
    _partyFocusNode.dispose();
    super.dispose();
  }

  Future<void> _submitTransaction() async {
    final amountPaise = CurrencyFormatter.parseToPaise(_amountController.text);
    if (amountPaise <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a valid amount greater than zero.')),
      );
      return;
    }

    if (_selectedAccountId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please select an account.')),
      );
      return;
    }

    final accounting = context.read<AccountingService>();
    final tabIndex = _tabController.index;

    // Handle party auto-detection or inline creation if user typed a name but did not tap suggestion
    if (tabIndex != 2) {
      final typedParty = _partyTextController.text.trim();
      if (typedParty.isNotEmpty && _selectedPartyId == null) {
        PartyModel? matchedParty;
        for (final p in accounting.parties) {
          if (p.name.trim().toLowerCase() == typedParty.toLowerCase()) {
            matchedParty = p;
            break;
          }
        }

        if (matchedParty != null) {
          _selectedPartyId = matchedParty.id;
        } else {
          final userChoice = await showPopInDialog<String>(
            context: context,
            builder: (dialogCtx) => AlertDialog(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              title: Row(
                children: [
                  const Icon(Icons.person_search, color: Colors.teal),
                  const SizedBox(width: 8),
                  const Text('Party Ledger Nahi Mili', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ],
              ),
              content: Text(
                'Party "$typedParty" aapke records me pehle se nahi bani hui hai.\n\n'
                '• Kya aap iski nayi party ledger banana chahte hain?\n'
                '• Ya bina party ke (General Cash Counter) save karna chahte hain?',
                style: const TextStyle(height: 1.4),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(dialogCtx, 'CANCEL'),
                  child: const Text('Cancel'),
                ),
                OutlinedButton(
                  onPressed: () => Navigator.pop(dialogCtx, 'GENERAL_CASH'),
                  child: const Text('Bina Party Ke Save Karein (Cash)'),
                ),
                ElevatedButton.icon(
                  onPressed: () => Navigator.pop(dialogCtx, 'CREATE_PARTY'),
                  icon: const Icon(Icons.person_add),
                  label: const Text('Nayi Party Banayein'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    foregroundColor: Colors.white,
                  ),
                ),
              ],
            ),
          );

          if (!mounted) return;

          if (userChoice == 'CREATE_PARTY') {
            await _showInlinePartyDialog(context, initialName: typedParty);
            if (_selectedPartyId == null) {
              return;
            }
          } else if (userChoice == 'GENERAL_CASH') {
            _partyTextController.clear();
            _selectedPartyId = null;
          } else {
            return;
          }
        }
      }
    }

    setState(() => _isSubmitting = true);

    final auth = context.read<AuthService>();
    final user = auth.currentUser;

    final createdBy = user?.id ?? 'usr_current';
    final createdByName = user?.displayName ?? 'Operator';
    const deviceId = 'dev_win_client';

    final selectedAcc = accounting.accounts.firstWhere((a) => a.id == _selectedAccountId);
    PartyModel? linkedParty;
    if (_selectedPartyId != null) {
      try {
        linkedParty = accounting.parties.firstWhere((p) => p.id == _selectedPartyId);
      } catch (_) {}
    }

    ({bool success, String? error}) result;

    if (widget.existingTransaction != null) {
      final updated = widget.existingTransaction!.copyWith(
        amountPaise: amountPaise,
        accountId: _selectedAccountId,
        partyId: _selectedPartyId,
        categoryId: _selectedCategoryId,
        paymentMode: _paymentMode,
        narration: _narrationController.text.trim(),
        referenceNumber: _refController.text.trim().isEmpty ? null : _refController.text.trim(),
        transactionDate: DateFormatter.formatDate(_selectedDate),
        updatedAt: DateTime.now(),
      );
      result = await accounting.updateTransaction(updatedTransaction: updated);
    } else if (tabIndex == 0) {
      // Income
      result = await accounting.recordIncome(
        accountId: _selectedAccountId!,
        amountPaise: amountPaise,
        paymentMode: _paymentMode,
        categoryId: _selectedCategoryId,
        partyId: _selectedPartyId,
        narration: _narrationController.text.trim().isEmpty ? null : _narrationController.text.trim(),
        referenceNumber: _refController.text.trim().isEmpty ? null : _refController.text.trim(),
        createdBy: createdBy,
        createdByName: createdByName,
        deviceId: deviceId,
        customDate: _selectedDate,
      );
    } else if (tabIndex == 1) {
      // Expense
      result = await accounting.recordExpense(
        accountId: _selectedAccountId!,
        amountPaise: amountPaise,
        paymentMode: _paymentMode,
        categoryId: _selectedCategoryId,
        partyId: _selectedPartyId,
        narration: _narrationController.text.trim().isEmpty ? null : _narrationController.text.trim(),
        referenceNumber: _refController.text.trim().isEmpty ? null : _refController.text.trim(),
        createdBy: createdBy,
        createdByName: createdByName,
        deviceId: deviceId,
        customDate: _selectedDate,
      );
    } else {
      // Transfer
      if (_selectedTargetAccountId == null) {
        setState(() => _isSubmitting = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please select a destination account.')),
        );
        return;
      }

      result = await accounting.recordTransfer(
        sourceAccountId: _selectedAccountId!,
        targetAccountId: _selectedTargetAccountId!,
        amountPaise: amountPaise,
        paymentMode: _paymentMode,
        narration: _narrationController.text.trim().isEmpty ? null : _narrationController.text.trim(),
        referenceNumber: _refController.text.trim().isEmpty ? null : _refController.text.trim(),
        createdBy: createdBy,
        createdByName: createdByName,
        deviceId: deviceId,
        customDate: _selectedDate,
      );
    }

    setState(() => _isSubmitting = false);

    if (result.success) {
      if (mounted) {
        final latestTxn = accounting.transactions.isNotEmpty ? accounting.transactions.first : null;
        final voucherNo = latestTxn?.voucherNumber ?? 'VCH-NEW';

        // Pop celebration dialog with spring bounce
        await showPopInDialog(
          context: context,
          builder: (dialogCtx) => TransactionCelebrationDialog(
            title: tabIndex == 0
                ? 'Cash In Recorded!'
                : (tabIndex == 1 ? 'Cash Out Recorded!' : 'Transfer Completed!'),
            voucherNumber: voucherNo,
            formattedAmount: CurrencyFormatter.formatPaise(amountPaise),
            accountName: selectedAcc.accountName,
            partyName: linkedParty?.name,
            isIncome: tabIndex == 0,
            onDone: () {
              Navigator.pop(dialogCtx);
              if (mounted) Navigator.pop(context);
            },
            onAddAnother: () {
              Navigator.pop(dialogCtx);
              _amountController.clear();
              _narrationController.clear();
              _refController.clear();
              _partyTextController.clear();
              setState(() {
                _selectedPartyId = null;
              });
            },
          ),
        );
      }
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: Colors.red[800], content: Text(result.error ?? 'Error saving transaction.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final accounting = context.watch<AccountingService>();
    final isDesktop = MediaQuery.of(context).size.width >= 700;
    final theme = Theme.of(context);
    final isBW = theme.brightness == Brightness.light && theme.colorScheme.primary == const Color(0xFF000000);

    return Scaffold(
      appBar: AppBar(
        title: const Text('New Financial Entry'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: isBW
              ? Colors.black
              : (_tabController.index == 0
                  ? Colors.green
                  : (_tabController.index == 1 ? Colors.red : Colors.blue)),
          labelColor: Theme.of(context).colorScheme.primary,
          tabs: [
            Tab(icon: Icon(Icons.arrow_downward, color: isBW ? Colors.black87 : Colors.green), text: 'Money In (Income)'),
            Tab(icon: Icon(Icons.arrow_upward, color: isBW ? Colors.black87 : Colors.red), text: 'Money Out (Expense)'),
            Tab(icon: Icon(Icons.swap_horiz, color: isBW ? Colors.black87 : Colors.blue), text: 'Transfer (Cash<->Bank)'),
          ],
        ),
      ),
      body: Center(
        child: Container(
          constraints: BoxConstraints(maxWidth: isDesktop ? 650 : double.infinity),
          padding: const EdgeInsets.all(20),
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Amount Input Field with Large Rupees Text
                _buildAmountField(),
                const SizedBox(height: 20),

                // Date Picker Row
                _buildDatePickerTile(),
                const SizedBox(height: 16),

                // Account Selection
                _buildAccountDropdown(accounting),
                const SizedBox(height: 16),

                // If Transfer: Target Account Dropdown
                if (_tabController.index == 2) ...[
                  _buildTargetAccountDropdown(accounting),
                  const SizedBox(height: 16),
                ],

                // If Income or Expense: Category & Party Autocomplete
                if (_tabController.index != 2) ...[
                  _buildCategoryDropdown(accounting),
                  const SizedBox(height: 16),
                  _buildPartyAutocompleteField(accounting),
                  const SizedBox(height: 16),
                ],

                // Payment Mode Selection
                _buildPaymentModeSelector(),
                const SizedBox(height: 16),

                // Reference Number
                TextField(
                  controller: _refController,
                  decoration: const InputDecoration(
                    labelText: 'Reference / Invoice / UTR / Cheque No. (Optional)',
                    prefixIcon: Icon(Icons.tag),
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),

                // Description / Narration
                TextField(
                  controller: _narrationController,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Description / Narration',
                    prefixIcon: Icon(Icons.notes),
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 24),

                // Submit Button with Tactile Bounce Micro-Animation
                TactileBounceButton(
                  child: ElevatedButton.icon(
                    onPressed: _isSubmitting ? null : _submitTransaction,
                    icon: _isSubmitting
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.check_circle),
                    label: Text(
                      _tabController.index == 0
                          ? 'SAVE INCOME (₹)'
                          : (_tabController.index == 1 ? 'SAVE EXPENSE (₹)' : 'EXECUTE TRANSFER (₹)'),
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: isBW
                          ? Colors.black
                          : (_tabController.index == 0
                              ? Colors.green[700]
                              : (_tabController.index == 1 ? Colors.red[700] : Colors.blue[700])),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildAmountField() {
    final theme = Theme.of(context);
    final isBW = theme.brightness == Brightness.light && theme.colorScheme.primary == const Color(0xFF000000);

    return Card(
      elevation: 0,
      color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isBW
              ? Colors.black
              : (_tabController.index == 0
                  ? Colors.green
                  : (_tabController.index == 1 ? Colors.red : Colors.blue)),
          width: 2,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: TextField(
          controller: _amountController,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          autofocus: true,
          style: TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.bold,
            color: isBW
                ? Colors.black
                : (_tabController.index == 0
                    ? Colors.green[800]
                    : (_tabController.index == 1 ? Colors.red[800] : Colors.blue[800])),
          ),
          decoration: const InputDecoration(
            prefixText: '₹ ',
            prefixStyle: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
            labelText: 'Enter Amount (₹)',
            border: InputBorder.none,
          ),
        ),
      ),
    );
  }

  Widget _buildDatePickerTile() {
    return InkWell(
      onTap: () async {
        final picked = await showDatePicker(
          context: context,
          initialDate: _selectedDate,
          firstDate: DateTime(2020),
          lastDate: DateTime(2035),
        );
        if (picked != null) {
          setState(() => _selectedDate = picked);
        }
      },
      child: InputDecorator(
        decoration: const InputDecoration(
          labelText: 'Transaction Date (DD-MM-YYYY)',
          prefixIcon: Icon(Icons.calendar_today),
          border: OutlineInputBorder(),
        ),
        child: Text(
          DateFormatter.formatDate(_selectedDate),
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  Widget _buildAccountDropdown(AccountingService accounting) {
    return DropdownButtonFormField<String>(
      value: _selectedAccountId,
      decoration: InputDecoration(
        labelText: _tabController.index == 2 ? 'Source Account (From)' : 'Account (Cash / Bank)',
        prefixIcon: const Icon(Icons.account_balance_wallet),
        border: const OutlineInputBorder(),
      ),
      items: accounting.accounts.map((acc) {
        return DropdownMenuItem<String>(
          value: acc.id,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(acc.accountName),
              Text(
                ' [Bal: ${CurrencyFormatter.formatPaise(acc.currentBalancePaise)}]',
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ],
          ),
        );
      }).toList(),
      onChanged: (val) {
        setState(() => _selectedAccountId = val);
      },
    );
  }

  Widget _buildTargetAccountDropdown(AccountingService accounting) {
    final available = accounting.accounts.where((a) => a.id != _selectedAccountId).toList();

    return DropdownButtonFormField<String>(
      value: _selectedTargetAccountId,
      decoration: const InputDecoration(
        labelText: 'Destination Account (To)',
        prefixIcon: Icon(Icons.login),
        border: OutlineInputBorder(),
      ),
      items: available.map((acc) {
        return DropdownMenuItem<String>(
          value: acc.id,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(acc.accountName),
              Text(
                ' [Bal: ${CurrencyFormatter.formatPaise(acc.currentBalancePaise)}]',
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ],
          ),
        );
      }).toList(),
      onChanged: (val) {
        setState(() => _selectedTargetAccountId = val);
      },
    );
  }

  Widget _buildCategoryDropdown(AccountingService accounting) {
    final isIncome = _tabController.index == 0;
    final filteredCategories = accounting.categories
        .where((c) => isIncome ? c.isIncome : c.isExpense)
        .toList();

    return DropdownButtonFormField<String>(
      value: _selectedCategoryId,
      decoration: const InputDecoration(
        labelText: 'Category',
        prefixIcon: Icon(Icons.category),
        border: OutlineInputBorder(),
      ),
      items: filteredCategories.map((cat) {
        return DropdownMenuItem<String>(
          value: cat.id,
          child: Text(cat.name),
        );
      }).toList(),
      onChanged: (val) {
        setState(() => _selectedCategoryId = val);
      },
    );
  }

  /// Autocomplete Party Search with live filtering and instant inline party creation
  Widget _buildPartyAutocompleteField(AccountingService accounting) {
    final theme = Theme.of(context);
    PartyModel? selectedParty;
    if (_selectedPartyId != null) {
      try {
        selectedParty = accounting.parties.firstWhere((p) => p.id == _selectedPartyId);
      } catch (_) {}
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        return RawAutocomplete<PartyModel>(
          textEditingController: _partyTextController,
          focusNode: _partyFocusNode,
          displayStringForOption: (PartyModel p) => p.name,
          optionsBuilder: (TextEditingValue textEditingValue) {
            final query = textEditingValue.text.trim().toLowerCase();
            if (query.isEmpty) {
              return accounting.parties;
            }
            return accounting.parties.where((p) {
              final nameMatch = p.name.toLowerCase().contains(query);
              final phoneMatch = p.phone?.toLowerCase().contains(query) ?? false;
              final addrMatch = p.address?.toLowerCase().contains(query) ?? false;
              return nameMatch || phoneMatch || addrMatch;
            });
          },
          onSelected: (PartyModel party) {
            setState(() {
              _selectedPartyId = party.id;
              _partyTextController.text = party.name;
            });
          },
          fieldViewBuilder: (context, textController, focusNode, onFieldSubmitted) {
            return TextField(
              controller: textController,
              focusNode: focusNode,
              decoration: InputDecoration(
                labelText: 'Party (Type name to search / Add new)',
                hintText: 'e.g. Ramesh Bhai, Reliance, Mukesh...',
                prefixIcon: const Icon(Icons.person_search),
                border: const OutlineInputBorder(),
                suffixIcon: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (_selectedPartyId != null || textController.text.isNotEmpty)
                      IconButton(
                        icon: const Icon(Icons.clear, size: 20),
                        tooltip: 'Clear selection (General Cash)',
                        onPressed: () {
                          textController.clear();
                          setState(() {
                            _selectedPartyId = null;
                          });
                        },
                      ),
                    TactileBounceButton(
                      child: IconButton(
                        icon: const Icon(Icons.person_add_alt_1, color: Colors.teal),
                        tooltip: 'Create New Party Ledger',
                        onPressed: () => _showInlinePartyDialog(context, initialName: textController.text.trim()),
                      ),
                    ),
                  ],
                ),
                helperText: selectedParty != null
                    ? 'Selected: ${selectedParty.name} (${selectedParty.partyType.displayName}) • Outstanding: ${CurrencyFormatter.formatPaise(selectedParty.currentBalancePaise)}'
                    : 'Leave blank for General Cash Counter sale/purchase',
                helperStyle: TextStyle(
                  color: selectedParty != null ? theme.colorScheme.primary : Colors.grey[600],
                  fontWeight: selectedParty != null ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            );
          },
          optionsViewBuilder: (context, onSelected, options) {
            final query = _partyTextController.text.trim();
            final optionsList = options.toList();

            return Align(
              alignment: Alignment.topLeft,
              child: Material(
                elevation: 4,
                borderRadius: BorderRadius.circular(10),
                child: Container(
                  width: constraints.maxWidth,
                  constraints: const BoxConstraints(maxHeight: 280),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.surface,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: theme.dividerTheme.color ?? Colors.grey.shade300),
                  ),
                  child: ListView(
                    padding: EdgeInsets.zero,
                    shrinkWrap: true,
                    children: [
                      // Prominent "Add New Party" button at top if user typed something new
                      if (query.isNotEmpty)
                        ListTile(
                          dense: true,
                          tileColor: theme.colorScheme.primary.withValues(alpha: 0.08),
                          leading: CircleAvatar(
                            radius: 14,
                            backgroundColor: theme.colorScheme.primary,
                            child: const Icon(Icons.add, size: 16, color: Colors.white),
                          ),
                          title: Text(
                            '➕ Naya Party Banayein: "$query"',
                            style: TextStyle(
                              color: theme.colorScheme.primary,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          subtitle: const Text('Click to create and link instantly', style: TextStyle(fontSize: 11)),
                          onTap: () {
                            _showInlinePartyDialog(context, initialName: query);
                          },
                        ),
                      if (optionsList.isEmpty && query.isEmpty)
                        const Padding(
                          padding: EdgeInsets.all(12),
                          child: Text('No parties available. Tap + to create one.', style: TextStyle(color: Colors.grey)),
                        ),
                      ...optionsList.map((party) {
                        final balance = party.currentBalancePaise;
                        final isReceivable = balance > 0;
                        final isPayable = balance < 0;

                        Color balColor = Colors.grey;
                        String balText = '₹0';
                        if (isReceivable) {
                          balColor = Colors.green[800]!;
                          balText = 'Rec: ${CurrencyFormatter.formatPaise(balance)}';
                        } else if (isPayable) {
                          balColor = Colors.deepOrange[800]!;
                          balText = 'Pay: ${CurrencyFormatter.formatPaise(balance.abs())}';
                        }

                        return ListTile(
                          dense: true,
                          leading: CircleAvatar(
                            radius: 14,
                            backgroundColor: theme.colorScheme.primary.withValues(alpha: 0.12),
                            child: Text(
                              party.name.isNotEmpty ? party.name[0].toUpperCase() : 'P',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: theme.colorScheme.primary),
                            ),
                          ),
                          title: Text(
                            party.name,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                          ),
                          subtitle: Text(
                            '${party.partyType.displayName}${party.phone != null ? " • ${party.phone}" : ""}',
                            style: const TextStyle(fontSize: 11),
                          ),
                          trailing: Text(
                            balText,
                            style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: balColor),
                          ),
                          onTap: () => onSelected(party),
                        );
                      }),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );
  }

  /// Inline Party Creation Dialog: Opens via smooth pop-in animation
  Future<void> _showInlinePartyDialog(BuildContext context, {String initialName = ''}) async {
    final nameCtrl = TextEditingController(text: initialName);
    final phoneCtrl = TextEditingController();
    final addressCtrl = TextEditingController();
    final openingBalCtrl = TextEditingController(text: '0');

    // Smart default: Income -> Customer; Expense -> Supplier
    PartyType selectedType = _tabController.index == 0 ? PartyType.customer : PartyType.supplier;
    OpeningBalanceType balanceType = selectedType == PartyType.supplier
        ? OpeningBalanceType.payable
        : OpeningBalanceType.receivable;

    await showPopInDialog(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Row(
            children: [
              const Icon(Icons.person_add, color: Colors.teal),
              const SizedBox(width: 8),
              const Text('Quick Add Party Ledger', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<PartyType>(
                  value: selectedType,
                  decoration: const InputDecoration(labelText: 'Party Type', border: OutlineInputBorder()),
                  items: const [
                    DropdownMenuItem(value: PartyType.customer, child: Text('Customer (Grahak)')),
                    DropdownMenuItem(value: PartyType.supplier, child: Text('Supplier (Vendor)')),
                    DropdownMenuItem(value: PartyType.employee, child: Text('Employee / Staff / Driver')),
                  ],
                  onChanged: (val) {
                    if (val != null) {
                      setDialogState(() {
                        selectedType = val;
                        if (val == PartyType.supplier) balanceType = OpeningBalanceType.payable;
                        if (val == PartyType.customer) balanceType = OpeningBalanceType.receivable;
                      });
                    }
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nameCtrl,
                  autofocus: initialName.isEmpty,
                  decoration: const InputDecoration(
                    labelText: 'Party / Business Name *',
                    hintText: 'e.g. Ramesh Bhai',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.business),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: phoneCtrl,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Phone Number (Optional)',
                    hintText: '+91 98765 43210',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.phone),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: addressCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Address / City (Optional)',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.location_on),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: TextField(
                        controller: openingBalCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(
                          labelText: 'Opening Balance (₹)',
                          prefixText: '₹ ',
                          border: OutlineInputBorder(),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: DropdownButtonFormField<OpeningBalanceType>(
                        value: balanceType,
                        decoration: const InputDecoration(border: OutlineInputBorder()),
                        items: const [
                          DropdownMenuItem(value: OpeningBalanceType.receivable, child: Text('Lena (Rec)')),
                          DropdownMenuItem(value: OpeningBalanceType.payable, child: Text('Dena (Pay)')),
                        ],
                        onChanged: (val) {
                          if (val != null) setDialogState(() => balanceType = val);
                        },
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogCtx),
              child: const Text('Cancel'),
            ),
            TactileBounceButton(
              child: ElevatedButton.icon(
                onPressed: () async {
                  final name = nameCtrl.text.trim();
                  if (name.isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Please enter party name.')),
                    );
                    return;
                  }

                  final rawPaise = CurrencyFormatter.parseToPaise(openingBalCtrl.text);
                  final initialBalancePaise = balanceType == OpeningBalanceType.receivable ? rawPaise : -rawPaise;
                  final now = DateTime.now();
                  final accounting = context.read<AccountingService>();
                  final auth = context.read<AuthService>();
                  final user = auth.currentUser;

                  final newParty = PartyModel(
                    id: 'pty_${now.millisecondsSinceEpoch}',
                    businessId: accounting.currentBusinessId,
                    name: name,
                    partyType: selectedType,
                    phone: phoneCtrl.text.trim().isEmpty ? null : phoneCtrl.text.trim(),
                    address: addressCtrl.text.trim().isEmpty ? null : addressCtrl.text.trim(),
                    openingBalancePaise: rawPaise,
                    openingBalanceType: balanceType,
                    currentBalancePaise: initialBalancePaise,
                    createdAt: now,
                    updatedAt: now,
                  );

                  await accounting.addParty(
                    newParty,
                    createdBy: user?.id ?? 'usr_current',
                    createdByName: user?.displayName ?? 'Operator',
                  );

                  if (mounted) {
                    setState(() {
                      _selectedPartyId = newParty.id;
                      _partyTextController.text = newParty.name;
                    });
                    Navigator.pop(dialogCtx);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        backgroundColor: Colors.teal[800],
                        content: Text('Party "${newParty.name}" created and selected! You can now save your entry.'),
                      ),
                    );
                  }
                },
                icon: const Icon(Icons.check),
                label: const Text('Save & Select Party'),
                style: ElevatedButton.styleFrom(backgroundColor: Colors.teal, foregroundColor: Colors.white),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPaymentModeSelector() {
    return DropdownButtonFormField<String>(
      value: _paymentMode,
      decoration: const InputDecoration(
        labelText: 'Payment Mode',
        prefixIcon: Icon(Icons.payment),
        border: OutlineInputBorder(),
      ),
      items: _paymentModes.map((mode) {
        return DropdownMenuItem<String>(
          value: mode,
          child: Text(mode),
        );
      }).toList(),
      onChanged: (val) {
        if (val != null) setState(() => _paymentMode = val);
      },
    );
  }
}
