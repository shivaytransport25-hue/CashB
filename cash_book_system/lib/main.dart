import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import 'accounts/accounts_screen.dart';
import 'authentication/login_screen.dart';
import 'core/animations/bounce_button.dart';
import 'core/theme/theme_provider.dart';
import 'dashboard/dashboard_screen.dart';
import 'parties/parties_screen.dart';
import 'reports/reports_screen.dart';
import 'services/accounting_service.dart';
import 'services/auth_service.dart';
import 'settings/settings_screen.dart';
import 'sync/sync_engine.dart';
import 'transactions/transaction_entry_screen.dart';
import 'transactions/transaction_list_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CashBookApp());
}

class CashBookApp extends StatelessWidget {
  const CashBookApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthService()..initializeAuth()),
        ChangeNotifierProvider(create: (_) => AccountingService()..loadBusinessData('biz_shivay_001')),
        ChangeNotifierProvider.value(value: SyncEngine.instance),
        ChangeNotifierProvider(create: (_) => ThemeProvider()..loadPreferences()),
      ],
      child: Consumer2<ThemeProvider, AuthService>(
        builder: (context, themeProvider, auth, _) {
          return MaterialApp(
            title: 'Cash Book Management System',
            debugShowCheckedModeBanner: false,
            theme: themeProvider.themeData,
            home: auth.isLoading
                ? const CashBookStartupLoadingScreen()
                : (auth.isAuthenticated
                    ? const MainShell()
                    : LoginScreen(onLoginSuccess: () {})),
          );
        },
      ),
    );
  }
}

class CashBookStartupLoadingScreen extends StatefulWidget {
  const CashBookStartupLoadingScreen({super.key});

  @override
  State<CashBookStartupLoadingScreen> createState() => _CashBookStartupLoadingScreenState();
}

class _CashBookStartupLoadingScreenState extends State<CashBookStartupLoadingScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _animController;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat(reverse: true);
    _scaleAnimation = Tween<double>(begin: 0.95, end: 1.05).animate(
      CurvedAnimation(parent: _animController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ScaleTransition(
              scale: _scaleAnimation,
              child: Container(
                width: 90,
                height: 90,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF1E3A8A)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(22),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFFF59E0B).withOpacity(0.35),
                      blurRadius: 25,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.account_balance_wallet,
                  size: 46,
                  color: Colors.white,
                ),
              ),
            ),
            const SizedBox(height: 28),
            Text(
              'Shivay CashBook Pro',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: theme.colorScheme.onSurface,
                letterSpacing: 0.5,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Secure Cloud & Offline Financial Ledger',
              style: TextStyle(
                fontSize: 13,
                color: theme.colorScheme.onSurface.withOpacity(0.6),
              ),
            ),
            const SizedBox(height: 36),
            SizedBox(
              width: 32,
              height: 32,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                valueColor: AlwaysStoppedAnimation<Color>(theme.colorScheme.primary),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Initializing Secure Session...',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: theme.colorScheme.primary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _selectedIndex = 0;

  void _openQuickEntry({int tabIndex = 0}) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => TransactionEntryScreen(initialTabIndex: tabIndex),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final themeProvider = context.watch<ThemeProvider>();
    final isDesktop = MediaQuery.of(context).size.width >= 900;

    final screens = [
      DashboardScreen(
        onNavigateToTransactions: () => setState(() => _selectedIndex = 1),
        onOpenQuickEntry: () => _openQuickEntry(),
      ),
      TransactionListScreen(
        onOpenNewEntry: () => _openQuickEntry(),
      ),
      AccountsScreen(
        onOpenTransfer: () => _openQuickEntry(tabIndex: 2),
      ),
      const PartiesScreen(),
      const ReportsScreen(),
      SettingsScreen(
        onToggleTheme: themeProvider.toggleTheme,
        isDarkMode: themeProvider.isDarkMode,
      ),
    ];

    // Global Desktop Keyboard Shortcuts
    return Shortcuts(
      shortcuts: <LogicalKeySet, Intent>{
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyN): const _NewTransactionIntent(),
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyR): const _RefreshIntent(),
      },
      child: Actions(
        actions: <Type, Action<Intent>>{
          _NewTransactionIntent: CallbackAction<_NewTransactionIntent>(
            onInvoke: (_) => _openQuickEntry(),
          ),
          _RefreshIntent: CallbackAction<_RefreshIntent>(
            onInvoke: (_) => SyncEngine.instance.triggerSync(),
          ),
        },
        child: Focus(
          autofocus: true,
          child: Scaffold(
            body: Row(
              children: [
                // Desktop Persistent Sidebar
                if (isDesktop) _buildDesktopSidebar(context, themeProvider),

                // Main Content Body
                Expanded(child: screens[_selectedIndex]),
              ],
            ),
            bottomNavigationBar: isDesktop
                ? null
                : NavigationBar(
                    selectedIndex: _selectedIndex,
                    onDestinationSelected: (idx) => setState(() => _selectedIndex = idx),
                    destinations: const [
                      NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Dashboard'),
                      NavigationDestination(icon: Icon(Icons.receipt_long_outlined), selectedIcon: Icon(Icons.receipt_long), label: 'Entries'),
                      NavigationDestination(icon: Icon(Icons.account_balance_outlined), selectedIcon: Icon(Icons.account_balance), label: 'Accounts'),
                      NavigationDestination(icon: Icon(Icons.people_outline), selectedIcon: Icon(Icons.people), label: 'Parties'),
                      NavigationDestination(icon: Icon(Icons.bar_chart_outlined), selectedIcon: Icon(Icons.bar_chart), label: 'Reports'),
                      NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: 'Settings'),
                    ],
                  ),
          ),
        ),
      ),
    );
  }

  Widget _buildDesktopSidebar(BuildContext context, ThemeProvider themeProvider) {
    final theme = Theme.of(context);

    return Container(
      width: 250,
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(right: BorderSide(color: theme.dividerTheme.color ?? Colors.grey.withOpacity(0.2))),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // App Header
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                CircleAvatar(
                  backgroundColor: theme.colorScheme.primary,
                  child: const Icon(Icons.menu_book, color: Colors.white, size: 20),
                ),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Cash Book Pro', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: theme.colorScheme.onSurface)),
                    const Text('Financial System', style: TextStyle(fontSize: 11, color: Colors.grey)),
                  ],
                ),
              ],
            ),
          ),

          // Primary "+ New Transaction" Button with Tactile Bounce
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: TactileBounceButton(
              child: ElevatedButton.icon(
                onPressed: () => _openQuickEntry(),
                icon: const Icon(Icons.add),
                label: const Text('New Entry (Ctrl+N)'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: theme.colorScheme.primary,
                  foregroundColor: theme.colorScheme.onPrimary,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Navigation Links
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              children: [
                _buildNavItem(0, Icons.dashboard_outlined, Icons.dashboard, 'Dashboard'),
                _buildNavItem(1, Icons.receipt_long_outlined, Icons.receipt_long, 'Transactions'),
                _buildNavItem(2, Icons.account_balance_outlined, Icons.account_balance, 'Accounts & Ledgers'),
                _buildNavItem(3, Icons.people_outline, Icons.people, 'Parties'),
                _buildNavItem(4, Icons.bar_chart_outlined, Icons.bar_chart, 'Reports & Export'),
                _buildNavItem(5, Icons.settings_outlined, Icons.settings, 'Settings'),
              ],
            ),
          ),

          // Theme & Version Footer
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    TactileBounceButton(
                      child: IconButton(
                        icon: Icon(
                          themeProvider.isBlackAndWhite
                              ? Icons.contrast
                              : (themeProvider.isDarkMode ? Icons.dark_mode : Icons.light_mode),
                          size: 20,
                        ),
                        onPressed: themeProvider.toggleTheme,
                        tooltip: 'Theme: ${themeProvider.appearanceMode.displayName}',
                      ),
                    ),
                    Text(
                      themeProvider.isBlackAndWhite
                          ? 'B&W Mode'
                          : themeProvider.colorTheme.displayName.split(' ').first,
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600),
                    ),
                  ],
                ),
                const Text('v1.0.0 (Pro)', style: TextStyle(fontSize: 10, color: Colors.grey)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem(int index, IconData unselectedIcon, IconData selectedIcon, String label) {
    final isSelected = _selectedIndex == index;
    final color = isSelected ? Theme.of(context).colorScheme.primary : Colors.grey[700];

    return ListTile(
      dense: true,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      selected: isSelected,
      selectedTileColor: Theme.of(context).colorScheme.primary.withOpacity(0.12),
      leading: Icon(isSelected ? selectedIcon : unselectedIcon, color: color, size: 20),
      title: Text(
        label,
        style: TextStyle(
          fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
          color: color,
        ),
      ),
      onTap: () => setState(() => _selectedIndex = index),
    );
  }
}

class _NewTransactionIntent extends Intent {
  const _NewTransactionIntent();
}

class _RefreshIntent extends Intent {
  const _RefreshIntent();
}
