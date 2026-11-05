import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../accounting/financial_engine.dart';
import '../core/constants/enums.dart';
import '../core/utils/currency_formatter.dart';
import '../core/utils/date_formatter.dart';
import '../services/accounting_service.dart';
import '../services/auth_service.dart';
import '../sync/sync_engine.dart';

class DashboardScreen extends StatelessWidget {
  final VoidCallback onNavigateToTransactions;
  final VoidCallback onOpenQuickEntry;

  const DashboardScreen({
    super.key,
    required this.onNavigateToTransactions,
    required this.onOpenQuickEntry,
  });

  @override
  Widget build(BuildContext context) {
    final accounting = context.watch<AccountingService>();
    final syncEngine = context.watch<SyncEngine>();
    final auth = context.watch<AuthService>();
    final isDesktop = MediaQuery.of(context).size.width >= 900;

    final todaySummary = accounting.todaySummary;
    final monthlySummary = accounting.monthlySummary;

    return Scaffold(
      body: accounting.isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () async {
                await syncEngine.triggerSync();
              },
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: EdgeInsets.all(isDesktop ? 24 : 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Header Bar with Business Info & Sync Pill
                    _buildTopHeader(context, auth, syncEngine, isDesktop),
                    const SizedBox(height: 20),

                    // Primary Financial Cards
                    _buildBalanceOverviewGrid(context, accounting, isDesktop),
                    const SizedBox(height: 20),

                    // Today vs Monthly Metrics
                    _buildPeriodMetricsRow(context, todaySummary, monthlySummary, isDesktop),
                    const SizedBox(height: 24),

                    // Financial Visual Charts
                    _buildChartsSection(context, accounting, isDesktop),
                    const SizedBox(height: 24),

                    // Recent Transactions Table / List
                    _buildRecentTransactionsSection(context, accounting, isDesktop),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildTopHeader(
    BuildContext context,
    AuthService auth,
    SyncEngine syncEngine,
    bool isDesktop,
  ) {
    final biz = auth.currentBusiness;
    Color syncColor;
    String syncText;
    IconData syncIcon;

    switch (syncEngine.state) {
      case SyncConnectionState.onlineSynced:
        syncColor = Colors.green;
        syncText = 'Cloud Synced';
        syncIcon = Icons.cloud_done;
        break;
      case SyncConnectionState.syncing:
        syncColor = Colors.blue;
        syncText = 'Syncing...';
        syncIcon = Icons.sync;
        break;
      case SyncConnectionState.offline:
        syncColor = Colors.orange;
        syncText = 'Offline Mode (${syncEngine.pendingCount} queued)';
        syncIcon = Icons.cloud_off;
        break;
      case SyncConnectionState.syncError:
        syncColor = Colors.red;
        syncText = 'Sync Alert';
        syncIcon = Icons.warning_amber;
        break;
    }

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              biz?.name ?? 'Cash Book Management',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 4),
            Text(
              '${DateFormatter.formatDate(DateTime.now())} | ${DateFormatter.getFinancialYearLabel(DateTime.now())}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey[600]),
            ),
          ],
        ),
        InkWell(
          onTap: () => syncEngine.triggerSync(),
          borderRadius: BorderRadius.circular(20),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            decoration: BoxDecoration(
              color: syncColor.withOpacity(0.12),
              border: Border.all(color: syncColor.withOpacity(0.5)),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(syncIcon, size: 16, color: syncColor),
                const SizedBox(width: 8),
                Text(
                  syncText,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: syncColor,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildBalanceOverviewGrid(
    BuildContext context,
    AccountingService accounting,
    bool isDesktop,
  ) {
    final available = CurrencyFormatter.formatPaise(accounting.totalAvailableBalancePaise);
    final cash = CurrencyFormatter.formatPaise(accounting.totalCashBalancePaise);
    final bank = CurrencyFormatter.formatPaise(accounting.totalBankBalancePaise);
    final receivables = CurrencyFormatter.formatPaise(accounting.totalReceivablesPaise);
    final payables = CurrencyFormatter.formatPaise(accounting.totalPayablesPaise);

    final cards = [
      _StatCard(
        title: 'Total Available Balance',
        value: available,
        icon: Icons.account_balance_wallet,
        color: Colors.indigo,
        subtitle: 'Cash + Bank accounts',
      ),
      _StatCard(
        title: 'Total Cash in Hand',
        value: cash,
        icon: Icons.payments,
        color: Colors.teal,
        subtitle: '${accounting.accounts.where((a) => a.accountType == AccountType.cash).length} Cash Accounts',
      ),
      _StatCard(
        title: 'Total Bank Balance',
        value: bank,
        icon: Icons.account_balance,
        color: Colors.blueAccent,
        subtitle: '${accounting.accounts.where((a) => a.accountType == AccountType.bank).length} Bank Accounts',
      ),
      _StatCard(
        title: 'Total Receivables',
        value: receivables,
        icon: Icons.call_received,
        color: Colors.green[700]!,
        subtitle: 'From customers',
      ),
      _StatCard(
        title: 'Total Payables',
        value: payables,
        icon: Icons.call_made,
        color: Colors.deepOrange,
        subtitle: 'To suppliers',
      ),
    ];

    if (isDesktop) {
      return GridView.count(
        crossAxisCount: 5,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        crossAxisSpacing: 16,
        mainAxisSpacing: 16,
        childAspectRatio: 1.6,
        children: cards,
      );
    } else {
      return SizedBox(
        height: 140,
        child: ListView.separated(
          scrollDirection: Axis.horizontal,
          itemCount: cards.length,
          separatorBuilder: (_, __) => const SizedBox(width: 12),
          itemBuilder: (ctx, idx) => SizedBox(width: 240, child: cards[idx]),
        ),
      );
    }
  }

  Widget _buildPeriodMetricsRow(
    BuildContext context,
    FinancialSummary today,
    FinancialSummary monthly,
    bool isDesktop,
  ) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Operating Cash Flow Snapshot',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _buildMetricTile(
                    'Today\'s Income',
                    CurrencyFormatter.formatPaise(today.totalIncomePaise),
                    Colors.green[700]!,
                    Icons.arrow_downward,
                  ),
                ),
                const VerticalDivider(width: 24),
                Expanded(
                  child: _buildMetricTile(
                    'Today\'s Expenses',
                    CurrencyFormatter.formatPaise(today.totalExpensePaise),
                    Colors.red[700]!,
                    Icons.arrow_upward,
                  ),
                ),
                if (isDesktop) ...[
                  const VerticalDivider(width: 24),
                  Expanded(
                    child: _buildMetricTile(
                      'Monthly Income',
                      CurrencyFormatter.formatPaise(monthly.totalIncomePaise),
                      Colors.green[800]!,
                      Icons.trending_up,
                    ),
                  ),
                  const VerticalDivider(width: 24),
                  Expanded(
                    child: _buildMetricTile(
                      'Monthly Expenses',
                      CurrencyFormatter.formatPaise(monthly.totalExpensePaise),
                      Colors.red[800]!,
                      Icons.trending_down,
                    ),
                  ),
                  const VerticalDivider(width: 24),
                  Expanded(
                    child: _buildMetricTile(
                      'Monthly Net Flow',
                      CurrencyFormatter.formatPaise(monthly.netCashFlowPaise),
                      monthly.netCashFlowPaise >= 0 ? Colors.indigo : Colors.deepOrange,
                      Icons.balance,
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricTile(String title, String value, Color color, IconData icon) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 4),
            Text(title, style: TextStyle(fontSize: 11, color: Colors.grey[600])),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  Widget _buildChartsSection(
    BuildContext context,
    AccountingService accounting,
    bool isDesktop,
  ) {
    final overall = accounting.overallSummary;

    final incomeAmt = (overall.totalIncomePaise / 100.0);
    final expenseAmt = (overall.totalExpensePaise / 100.0);

    return LayoutBuilder(
      builder: (context, constraints) {
        if (isDesktop) {
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                flex: 3,
                child: _buildBarChartCard(context, incomeAmt, expenseAmt),
              ),
              const SizedBox(width: 16),
              Expanded(
                flex: 2,
                child: _buildPieChartCard(context, accounting),
              ),
            ],
          );
        } else {
          return Column(
            children: [
              _buildBarChartCard(context, incomeAmt, expenseAmt),
              const SizedBox(height: 16),
              _buildPieChartCard(context, accounting),
            ],
          );
        }
      },
    );
  }

  Widget _buildBarChartCard(BuildContext context, double income, double expense) {
    final maxVal = (income > expense ? income : expense) * 1.25;
    final safeMax = maxVal > 100 ? maxVal : 1000.0;

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Income vs Expense Ratio',
              style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 200,
              child: BarChart(
                BarChartData(
                  maxY: safeMax,
                  barGroups: [
                    BarChartGroupData(
                      x: 0,
                      barRods: [
                        BarChartRodData(
                          toY: income > 0 ? income : 10,
                          color: Colors.green[600],
                          width: 32,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ],
                    ),
                    BarChartGroupData(
                      x: 1,
                      barRods: [
                        BarChartRodData(
                          toY: expense > 0 ? expense : 10,
                          color: Colors.red[600],
                          width: 32,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ],
                    ),
                  ],
                  titlesData: FlTitlesData(
                    leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    bottomTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        getTitlesWidget: (value, meta) {
                          if (value == 0) {
                            return const Padding(
                              padding: EdgeInsets.only(top: 8.0),
                              child: Text('Total Income', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.green)),
                            );
                          } else if (value == 1) {
                            return const Padding(
                              padding: EdgeInsets.only(top: 8.0),
                              child: Text('Total Expense', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.red)),
                            );
                          }
                          return const Text('');
                        },
                      ),
                    ),
                  ),
                  gridData: const FlGridData(show: false),
                  borderData: FlBorderData(show: false),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPieChartCard(BuildContext context, AccountingService accounting) {
    final cashPaise = accounting.totalCashBalancePaise;
    final bankPaise = accounting.totalBankBalancePaise;
    final total = (cashPaise + bankPaise) > 0 ? (cashPaise + bankPaise) : 1;

    final cashPercent = (cashPaise / total) * 100;
    final bankPercent = (bankPaise / total) * 100;

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Cash vs Bank Allocation',
              style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 200,
              child: PieChart(
                PieChartData(
                  sectionsSpace: 4,
                  centerSpaceRadius: 40,
                  sections: [
                    PieChartSectionData(
                      value: cashPaise.toDouble(),
                      title: '${cashPercent.toStringAsFixed(0)}%',
                      color: Colors.teal,
                      radius: 50,
                      titleStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                    PieChartSectionData(
                      value: bankPaise.toDouble(),
                      title: '${bankPercent.toStringAsFixed(0)}%',
                      color: Colors.blueAccent,
                      radius: 50,
                      titleStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentTransactionsSection(
    BuildContext context,
    AccountingService accounting,
    bool isDesktop,
  ) {
    final recent = accounting.transactions.take(8).toList();

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Recent Transactions',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
                TextButton.icon(
                  onPressed: onNavigateToTransactions,
                  icon: const Icon(Icons.arrow_forward, size: 16),
                  label: const Text('View All'),
                ),
              ],
            ),
            const Divider(),
            if (recent.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 32),
                child: Center(
                  child: Text('No transactions recorded yet. Tap "+ Entry" to record your first transaction.'),
                ),
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: recent.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (ctx, index) {
                  final txn = recent[index];
                  final isIncome = txn.isIncome;
                  final isTransfer = txn.isTransfer;
                  final isReversed = txn.isReversed;

                  Color amountColor = isIncome ? Colors.green[700]! : Colors.red[700]!;
                  if (isTransfer) amountColor = Colors.blue[700]!;
                  if (isReversed) amountColor = Colors.grey;

                  return ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: CircleAvatar(
                      backgroundColor: amountColor.withOpacity(0.12),
                      child: Icon(
                        isTransfer
                            ? Icons.swap_horiz
                            : (isIncome ? Icons.arrow_downward : Icons.arrow_upward),
                        color: amountColor,
                        size: 18,
                      ),
                    ),
                    title: Row(
                      children: [
                        Text(
                          txn.voucherNumber,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                        ),
                        const SizedBox(width: 8),
                        if (txn.partyName != null)
                          Expanded(
                            child: Text(
                              '• ${txn.partyName}',
                              style: TextStyle(fontSize: 12, color: Colors.grey[700]),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                      ],
                    ),
                    subtitle: Text(
                      '${txn.transactionDate} | ${txn.accountName ?? ''} ${txn.categoryName != null ? "• " + txn.categoryName! : ""}',
                      style: const TextStyle(fontSize: 11),
                    ),
                    trailing: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          '${isIncome ? "+" : (isTransfer ? "⇄ " : "-")}${CurrencyFormatter.formatPaise(txn.amountPaise)}',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            color: amountColor,
                            decoration: isReversed ? TextDecoration.lineThrough : null,
                          ),
                        ),
                        if (isReversed)
                          const Text('REVERSED', style: TextStyle(fontSize: 9, color: Colors.red, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;
  final String subtitle;

  const _StatCard({
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: TextStyle(fontSize: 11, color: Colors.grey[600], fontWeight: FontWeight.w600),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                CircleAvatar(
                  radius: 14,
                  backgroundColor: color.withOpacity(0.12),
                  child: Icon(icon, size: 16, color: color),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              value,
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color),
              overflow: TextOverflow.ellipsis,
            ),
            Text(
              subtitle,
              style: TextStyle(fontSize: 10, color: Colors.grey[500]),
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

