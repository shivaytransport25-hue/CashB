import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/animations/bounce_button.dart';
import '../core/constants/enums.dart';
import '../core/utils/currency_formatter.dart';
import '../models/party_model.dart';
import '../services/accounting_service.dart';

class PartiesScreen extends StatefulWidget {
  const PartiesScreen({super.key});

  @override
  State<PartiesScreen> createState() => _PartiesScreenState();
}

class _PartiesScreenState extends State<PartiesScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  List<PartyModel> _filterParties(List<PartyModel> list) {
    final q = _searchController.text.trim().toLowerCase();
    if (q.isEmpty) return list;
    return list.where((p) {
      final matchName = p.name.toLowerCase().contains(q);
      final matchPhone = p.phone?.toLowerCase().contains(q) ?? false;
      final matchAddress = p.address?.toLowerCase().contains(q) ?? false;
      return matchName || matchPhone || matchAddress;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final accounting = context.watch<AccountingService>();
    final isDesktop = MediaQuery.of(context).size.width >= 900;
    final theme = Theme.of(context);
    final isBW = theme.brightness == Brightness.light && theme.colorScheme.primary == const Color(0xFF000000);

    final allCustomers = accounting.parties.where((p) => p.partyType == PartyType.customer).toList();
    final allSuppliers = accounting.parties.where((p) => p.partyType == PartyType.supplier).toList();
    final allEmployees = accounting.parties.where((p) => p.partyType == PartyType.employee).toList();

    final customers = _filterParties(allCustomers);
    final suppliers = _filterParties(allSuppliers);
    final employees = _filterParties(allEmployees);

    return Scaffold(
      body: Padding(
        padding: EdgeInsets.all(isDesktop ? 24 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Toolbar
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    'Party & Ledger Management',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                  ),
                ),
                TactileBounceButton(
                  child: ElevatedButton.icon(
                    onPressed: () => _showAddPartyDialog(context, initialName: _searchController.text.trim()),
                    icon: const Icon(Icons.person_add),
                    label: const Text('Add Party'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: isBW ? Colors.black : Theme.of(context).colorScheme.primary,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Real-Time Party Search Bar
            TextField(
              controller: _searchController,
              onChanged: (_) => setState(() {}),
              decoration: InputDecoration(
                hintText: 'Search party ledger by name, phone, or address...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear, size: 18),
                        onPressed: () {
                          _searchController.clear();
                          setState(() {});
                        },
                      )
                    : null,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              ),
            ),
            const SizedBox(height: 16),

            // Tab Bar: Customers, Suppliers, Employees
            TabBar(
              controller: _tabController,
              indicatorColor: isBW ? Colors.black : theme.colorScheme.primary,
              labelColor: isBW ? Colors.black : theme.colorScheme.primary,
              tabs: [
                Tab(icon: const Icon(Icons.people), text: 'Customers (${customers.length})'),
                Tab(icon: const Icon(Icons.local_shipping), text: 'Suppliers (${suppliers.length})'),
                Tab(icon: const Icon(Icons.badge), text: 'Employees (${employees.length})'),
              ],
            ),
            const SizedBox(height: 16),

            // Tab Views
            Expanded(
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildPartyList(context, customers, PartyType.customer, isDesktop),
                  _buildPartyList(context, suppliers, PartyType.supplier, isDesktop),
                  _buildPartyList(context, employees, PartyType.employee, isDesktop),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPartyList(BuildContext context, List<PartyModel> parties, PartyType sectionType, bool isDesktop) {
    final query = _searchController.text.trim();

    if (parties.isEmpty) {
      if (query.isNotEmpty) {
        return Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.search_off, size: 48, color: Colors.grey),
              const SizedBox(height: 12),
              Text(
                'No parties found matching "$query"',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              const SizedBox(height: 16),
              TactileBounceButton(
                child: ElevatedButton.icon(
                  onPressed: () => _showAddPartyDialog(context, initialName: query, initialType: sectionType),
                  icon: const Icon(Icons.person_add),
                  label: Text('➕ Naya Party Banayein: "$query"'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    foregroundColor: Colors.white,
                  ),
                ),
              ),
            ],
          ),
        );
      }
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.people_outline, size: 48, color: Colors.grey),
            const SizedBox(height: 12),
            const Text('No parties added in this section.'),
            const SizedBox(height: 12),
            TactileBounceButton(
              child: OutlinedButton.icon(
                onPressed: () => _showAddPartyDialog(context, initialType: sectionType),
                icon: const Icon(Icons.add),
                label: const Text('Add First Party'),
              ),
            ),
          ],
        ),
      );
    }

    return ListView.separated(
      itemCount: parties.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (ctx, index) {
        final party = parties[index];
        final balance = party.currentBalancePaise;
        final isReceivable = balance > 0;
        final isPayable = balance < 0;

        Color balanceColor = Colors.grey;
        String balanceLabel = 'Settled (₹0)';
        if (isReceivable) {
          balanceColor = Colors.green[800]!;
          balanceLabel = 'To Receive: ${CurrencyFormatter.formatPaise(balance)}';
        } else if (isPayable) {
          balanceColor = Colors.deepOrange[800]!;
          balanceLabel = 'To Pay: ${CurrencyFormatter.formatPaise(balance.abs())}';
        }

        return Card(
          elevation: 1,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: Theme.of(context).colorScheme.primary.withValues(alpha: 0.1),
              child: Text(
                party.name.isNotEmpty ? party.name[0].toUpperCase() : 'P',
                style: TextStyle(fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.primary),
              ),
            ),
            title: Text(party.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (party.phone != null)
                  Text('Phone: ${party.phone}', style: const TextStyle(fontSize: 11)),
                if (party.address != null)
                  Text(party.address!, style: TextStyle(fontSize: 10, color: Colors.grey[600])),
              ],
            ),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  balanceLabel,
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: balanceColor),
                ),
                TactileBounceButton(
                  child: TextButton(
                    onPressed: () => _showPartyStatementModal(context, party),
                    child: const Text('View Ledger >', style: TextStyle(fontSize: 11)),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showAddPartyDialog(BuildContext context, {String initialName = '', PartyType initialType = PartyType.customer}) {
    final nameController = TextEditingController(text: initialName);
    final phoneController = TextEditingController();
    final addressController = TextEditingController();
    final openingBalanceController = TextEditingController(text: '0');

    PartyType selectedType = initialType;
    OpeningBalanceType balanceType = selectedType == PartyType.supplier
        ? OpeningBalanceType.payable
        : OpeningBalanceType.receivable;

    showPopInDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Row(
            children: [
              const Icon(Icons.person_add, color: Colors.teal),
              const SizedBox(width: 8),
              const Text('Add New Party Ledger', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
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
                  controller: nameController,
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
                  controller: phoneController,
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
                  controller: addressController,
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
                        controller: openingBalanceController,
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
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
            TactileBounceButton(
              child: ElevatedButton.icon(
                onPressed: () async {
                  final name = nameController.text.trim();
                  if (name.isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Please enter party name.')),
                    );
                    return;
                  }

                  final rawPaise = CurrencyFormatter.parseToPaise(openingBalanceController.text);
                  final initialBalancePaise = balanceType == OpeningBalanceType.receivable ? rawPaise : -rawPaise;
                  final now = DateTime.now();
                  final accounting = context.read<AccountingService>();

                  final newParty = PartyModel(
                    id: 'pty_${now.millisecondsSinceEpoch}',
                    businessId: accounting.currentBusinessId,
                    name: name,
                    partyType: selectedType,
                    phone: phoneController.text.trim().isEmpty ? null : phoneController.text.trim(),
                    address: addressController.text.trim().isEmpty ? null : addressController.text.trim(),
                    openingBalancePaise: rawPaise,
                    openingBalanceType: balanceType,
                    currentBalancePaise: initialBalancePaise,
                    createdAt: now,
                    updatedAt: now,
                  );

                  await accounting.addParty(newParty);
                  if (context.mounted) {
                    Navigator.pop(ctx);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        backgroundColor: Colors.teal[800],
                        content: Text('Party "${newParty.name}" created successfully!'),
                      ),
                    );
                  }
                },
                icon: const Icon(Icons.check),
                label: const Text('Save Party'),
                style: ElevatedButton.styleFrom(backgroundColor: Colors.teal, foregroundColor: Colors.white),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showPartyStatementModal(BuildContext context, PartyModel party) {
    final accounting = context.read<AccountingService>();
    final txns = accounting.transactions.where((t) => t.partyId == party.id).toList();

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
                      Text(party.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                      Text(
                        'Outstanding: ${CurrencyFormatter.formatPaise(party.currentBalancePaise)} (${party.partyType.displayName})',
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
                child: txns.isEmpty
                    ? const Center(child: Text('No transactions recorded for this party.'))
                    : ListView.separated(
                        controller: scrollController,
                        itemCount: txns.length,
                        separatorBuilder: (_, __) => const Divider(height: 1),
                        itemBuilder: (_, index) {
                          final t = txns[index];
                          final isIncome = t.isIncome;

                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text('${t.transactionDate} • ${t.voucherNumber}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                            subtitle: Text('${t.paymentMode} • ${t.description ?? "-"}', style: const TextStyle(fontSize: 11)),
                            trailing: Text(
                              '${isIncome ? "-" : "+"}${CurrencyFormatter.formatPaise(t.amountPaise)}',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                color: isIncome ? Colors.green : Colors.red,
                              ),
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
