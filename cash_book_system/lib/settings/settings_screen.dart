import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';

import '../core/animations/bounce_button.dart';
import '../core/constants/enums.dart';
import '../core/theme/theme_provider.dart';
import '../models/business_model.dart';
import '../services/accounting_service.dart';
import '../services/auth_service.dart';
import '../services/backup_service.dart';
import '../sync/sync_engine.dart';

class SettingsScreen extends StatefulWidget {
  final VoidCallback onToggleTheme;
  final bool isDarkMode;

  const SettingsScreen({
    super.key,
    required this.onToggleTheme,
    required this.isDarkMode,
  });

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _isBackingUp = false;
  bool _isCloudBackingUp = false;

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final syncEngine = context.watch<SyncEngine>();
    final themeProvider = context.watch<ThemeProvider>();
    final biz = auth.currentBusiness;
    final isDesktop = MediaQuery.of(context).size.width >= 900;

    return Scaffold(
      body: SingleChildScrollView(
        padding: EdgeInsets.all(isDesktop ? 24 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'System Settings & Business Profile',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),

            // Active Account & Google Sign-In Status Card
            _buildUserAccountCard(context, auth),
            const SizedBox(height: 20),

            // Color Themes & Appearance Settings Card
            _buildThemeSettingsCard(context, themeProvider),
            const SizedBox(height: 20),

            // Business Profile Card
            _buildBusinessProfileCard(context, auth, biz),
            const SizedBox(height: 20),

            // User Role & Permissions Management Card
            _buildUserRolesCard(context, auth),
            const SizedBox(height: 20),

            // Cloud Synchronization & Device Card
            _buildSyncManagementCard(context, syncEngine),
            const SizedBox(height: 20),

            // Long-Term Cloud Backup Vault & Disaster Recovery Card
            _buildBackupRestoreCard(context, auth),
          ],
        ),
      ),
    );
  }

  Widget _buildThemeSettingsCard(BuildContext context, ThemeProvider themeProvider) {
    final theme = Theme.of(context);

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.palette_outlined, color: theme.colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  'Color Themes & Display Appearance',
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            const SizedBox(height: 8),

            // Appearance Mode Selector (Light, Dark, Black & White)
            const Text('Appearance Mode:', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 10),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: AppAppearanceMode.values.map((mode) {
                final isSelected = themeProvider.appearanceMode == mode;
                return TactileBounceButton(
                  child: ChoiceChip(
                    avatar: Icon(mode.icon, size: 18, color: isSelected ? Colors.white : theme.colorScheme.primary),
                    label: Text(mode.displayName),
                    selected: isSelected,
                    selectedColor: theme.colorScheme.primary,
                    labelStyle: TextStyle(
                      color: isSelected ? Colors.white : theme.colorScheme.onSurface,
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                    ),
                    onSelected: (_) => themeProvider.setAppearanceMode(mode),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),

            // Color Themes Palette
            if (!themeProvider.isBlackAndWhite) ...[
              const Text('Color Accent Palette:', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
              const SizedBox(height: 10),
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: AppColorTheme.values.map((colTheme) {
                  final isSelected = themeProvider.colorTheme == colTheme;
                  return TactileBounceButton(
                    onTap: () => themeProvider.setColorTheme(colTheme),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: isSelected ? colTheme.primaryColor.withValues(alpha: 0.12) : theme.colorScheme.surface,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: isSelected ? colTheme.primaryColor : Colors.grey.withValues(alpha: 0.3),
                          width: isSelected ? 2 : 1,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 20,
                            height: 20,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: colTheme.primaryColor,
                            ),
                            child: isSelected ? const Icon(Icons.check, size: 14, color: Colors.white) : null,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            colTheme.displayName,
                            style: TextStyle(
                              fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                              fontSize: 12,
                              color: isSelected ? colTheme.primaryColor : theme.colorScheme.onSurface,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ),
            ] else ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.black, width: 1.5),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.info_outline, color: Colors.black, size: 20),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Black & White High-Contrast mode is currently active for maximum clarity, print readiness, and battery efficiency.',
                        style: TextStyle(fontSize: 12, color: Colors.black, fontWeight: FontWeight.w500),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildBusinessProfileCard(BuildContext context, AuthService auth, BusinessModel? biz) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Icon(Icons.business, color: Colors.indigo),
                    const SizedBox(width: 8),
                    Text(
                      'Business Profile',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                TactileBounceButton(
                  onTap: () => _editBusinessDialog(context, auth, biz),
                  child: TextButton.icon(
                    onPressed: () => _editBusinessDialog(context, auth, biz),
                    icon: const Icon(Icons.edit, size: 16),
                    label: const Text('Edit Details'),
                  ),
                ),
              ],
            ),
            const Divider(),
            const SizedBox(height: 8),
            _buildProfileRow('Company / Firm Name', biz?.name ?? '-'),
            _buildProfileRow('Owner / Authorized Person', biz?.ownerName ?? '-'),
            _buildProfileRow('Registered Address', biz?.address ?? '-'),
            _buildProfileRow('Contact Phone', biz?.phone ?? '-'),
            _buildProfileRow('Email ID', biz?.email ?? '-'),
            _buildProfileRow('GSTIN Number', biz?.gstin ?? 'Not Specified'),
            _buildProfileRow('PAN Card', biz?.pan ?? 'Not Specified'),
            _buildProfileRow('Default Currency', '${biz?.currencyCode} (${biz?.currencySymbol})'),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 180,
            child: Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.w500)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }

  Widget _buildUserRolesCard(BuildContext context, AuthService auth) {
    final user = auth.currentUser;
    final role = auth.currentRole;

    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.admin_panel_settings, color: Colors.teal),
                const SizedBox(width: 8),
                Text(
                  'Multi-User Roles & Security Access',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            const SizedBox(height: 8),
            Text(
              'Logged in as: ${user?.displayName} (${user?.email})',
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Text('Active Role: ', style: TextStyle(fontWeight: FontWeight.w600)),
                DropdownButton<UserRole>(
                  value: role,
                  items: UserRole.values.map((r) {
                    return DropdownMenuItem(value: r, child: Text(r.displayName));
                  }).toList(),
                  onChanged: (newRole) {
                    if (newRole != null) {
                      auth.switchUserRole(newRole);
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Switched role to ${newRole.displayName}')),
                      );
                    }
                  },
                ),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildPermissionChip('Manage Users', role.canManageUsers),
                _buildPermissionChip('Manage Accounts', role.canManageAccounts),
                _buildPermissionChip('Post Transactions', role.canPostTransactions),
                _buildPermissionChip('Reverse Transactions', role.canReverseTransactions),
                _buildPermissionChip('Export Reports', role.canExportReports),
                _buildPermissionChip('Manage Backups', role.canManageBackups),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPermissionChip(String title, bool granted) {
    return Chip(
      avatar: Icon(
        granted ? Icons.check_circle : Icons.cancel,
        size: 16,
        color: granted ? Colors.green[800] : Colors.grey,
      ),
      label: Text(title, style: TextStyle(fontSize: 11, color: granted ? Colors.black87 : Colors.grey)),
      backgroundColor: granted ? Colors.green.shade50 : Colors.grey.shade100,
      side: BorderSide(color: granted ? Colors.green.shade300 : Colors.grey.shade300),
    );
  }

  Widget _buildSyncManagementCard(BuildContext context, SyncEngine syncEngine) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.cloud_sync, color: Colors.blueAccent),
                    const SizedBox(width: 8),
                    Text(
                      'Real-Time Cloud Synchronization',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  'Pending sync queue: ${syncEngine.pendingCount} entities',
                  style: TextStyle(fontSize: 12, color: Colors.grey[700]),
                ),
                Text(
                  'Last synced: ${syncEngine.lastSyncTime != null ? syncEngine.lastSyncTime.toString().substring(0, 19) : "Never"}',
                  style: const TextStyle(fontSize: 11, color: Colors.grey),
                ),
              ],
            ),
            TactileBounceButton(
              onTap: () => syncEngine.triggerSync(),
              child: ElevatedButton.icon(
                onPressed: () => syncEngine.triggerSync(),
                icon: const Icon(Icons.refresh),
                label: const Text('Sync Now'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBackupRestoreCard(BuildContext context, AuthService auth) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.cloud_done, color: Colors.deepOrange),
                const SizedBox(width: 8),
                Text(
                  'Long-Term Cloud Vault & Disaster Recovery',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            const SizedBox(height: 8),
            const Text(
              'Your financial data is protected with SHA-256 cryptographic verification. Save immutable snapshots directly to Firebase Cloud Vault so you can recover your ledger anytime, across years, devices, and format changes.',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                // Cloud Vault Backup
                TactileBounceButton(
                  onTap: _isCloudBackingUp ? null : () => _uploadCloudBackup(context, auth),
                  child: ElevatedButton.icon(
                    onPressed: _isCloudBackingUp ? null : () => _uploadCloudBackup(context, auth),
                    icon: _isCloudBackingUp
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.cloud_upload),
                    label: const Text('Save to Cloud Vault (Long-Term)'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.teal[800],
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),

                // Local Download
                TactileBounceButton(
                  onTap: _isBackingUp ? null : () => _createBackup(context, auth),
                  child: OutlinedButton.icon(
                    onPressed: _isBackingUp ? null : () => _createBackup(context, auth),
                    icon: _isBackingUp
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.download),
                    label: const Text('Export JSON File'),
                  ),
                ),

                // Restore from Cloud Vault
                TactileBounceButton(
                  onTap: () => _showCloudSnapshotsDialog(context, auth),
                  child: OutlinedButton.icon(
                    onPressed: () => _showCloudSnapshotsDialog(context, auth),
                    icon: const Icon(Icons.history),
                    label: const Text('Cloud Vault History & Restore'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _uploadCloudBackup(BuildContext context, AuthService auth) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    setState(() => _isCloudBackingUp = true);
    try {
      final res = await BackupService.uploadBackupToCloudVault(biz);
      if (mounted) {
        if (res.success) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: Colors.green[800],
              content: Text('Saved to Cloud Vault!\nSnapshot ID: ${res.backupId}\nEncrypted with SHA-256 integrity.'),
            ),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: Colors.red[800],
              content: Text('Cloud backup note: ${res.error} (Ensure Firebase is connected).'),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: Colors.red, content: Text('Error: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isCloudBackingUp = false);
    }
  }

  void _showCloudSnapshotsDialog(BuildContext context, AuthService auth) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    final backups = await BackupService.getCloudBackups(biz.id);

    if (!context.mounted) return;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.cloud_done, color: Colors.teal),
            SizedBox(width: 8),
            Text('Cloud Vault Snapshots'),
          ],
        ),
        content: SizedBox(
          width: 500,
          child: backups.isEmpty
              ? const Padding(
                  padding: EdgeInsets.all(20),
                  child: Text('No cloud snapshots found yet. Click "Save to Cloud Vault" to create your first permanent snapshot.'),
                )
              : ListView.separated(
                  shrinkWrap: true,
                  itemCount: backups.length,
                  separatorBuilder: (_, __) => const Divider(),
                  itemBuilder: (_, idx) {
                    final b = backups[idx];
                    return ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const CircleAvatar(
                        backgroundColor: Colors.teal,
                        child: Icon(Icons.lock, color: Colors.white, size: 18),
                      ),
                      title: Text(b.backupId, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                      subtitle: Text(
                        'Date: ${b.createdAt.toString().substring(0, 19)}\nTxns: ${b.transactionCount} • Parties: ${b.partyCount} • Size: ${(b.sizeBytes / 1024).toStringAsFixed(1)} KB',
                        style: const TextStyle(fontSize: 11),
                      ),
                      trailing: ElevatedButton(
                        onPressed: () async {
                          final confirm = await showDialog<bool>(
                            context: ctx,
                            builder: (c) => AlertDialog(
                              title: const Text('Restore Cloud Snapshot?'),
                              content: const Text('Restoring will replace local data with this verified snapshot. A safety local snapshot will be saved automatically.'),
                              actions: [
                                TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Cancel')),
                                ElevatedButton(onPressed: () => Navigator.pop(c, true), child: const Text('Confirm Restore')),
                              ],
                            ),
                          );
                          if (confirm == true && context.mounted) {
                            final res = await BackupService.restoreFromCloudVault(business: biz, backupId: b.backupId);
                            if (context.mounted) {
                              Navigator.pop(ctx);
                              if (res.success) {
                                await context.read<AccountingService>().loadBusinessData(biz.id);
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(backgroundColor: Colors.green, content: Text('Database successfully restored from Cloud Vault!')),
                                );
                              } else {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(backgroundColor: Colors.red, content: Text('Restore failed: ${res.error}')),
                                );
                              }
                            }
                          }
                        },
                        child: const Text('Restore'),
                      ),
                    );
                  },
                ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
        ],
      ),
    );
  }

  Future<void> _createBackup(BuildContext context, AuthService auth) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    setState(() => _isBackingUp = true);
    try {
      final snapshot = await BackupService.createBackupSnapshot(biz);
      final jsonStr = jsonEncode(snapshot);

      final dir = await getApplicationDocumentsDirectory();
      final fileName = 'CashBook_Backup_${DateTime.now().millisecondsSinceEpoch}.json';
      final file = File('${dir.path}/$fileName');
      await file.writeAsString(jsonStr);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: Colors.green[800],
            content: Text('Backup created with SHA-256 checksum!\nSaved to: ${file.path}'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(backgroundColor: Colors.red, content: Text('Error generating backup: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isBackingUp = false);
    }
  }

  void _editBusinessDialog(BuildContext context, AuthService auth, BusinessModel? biz) {
    final nameCtrl = TextEditingController(text: biz?.name);
    final ownerCtrl = TextEditingController(text: biz?.ownerName);
    final addressCtrl = TextEditingController(text: biz?.address);
    final phoneCtrl = TextEditingController(text: biz?.phone);
    final emailCtrl = TextEditingController(text: biz?.email);
    final gstinCtrl = TextEditingController(text: biz?.gstin);
    final panCtrl = TextEditingController(text: biz?.pan);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Edit Business Profile'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nameCtrl, decoration: const InputDecoration(labelText: 'Business Name')),
              TextField(controller: ownerCtrl, decoration: const InputDecoration(labelText: 'Owner / Contact Person')),
              TextField(controller: addressCtrl, decoration: const InputDecoration(labelText: 'Address')),
              TextField(controller: phoneCtrl, decoration: const InputDecoration(labelText: 'Phone')),
              TextField(controller: emailCtrl, decoration: const InputDecoration(labelText: 'Email')),
              TextField(controller: gstinCtrl, decoration: const InputDecoration(labelText: 'GSTIN')),
              TextField(controller: panCtrl, decoration: const InputDecoration(labelText: 'PAN')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () async {
              if (biz != null) {
                final updated = biz.copyWith(
                  name: nameCtrl.text.trim(),
                  ownerName: ownerCtrl.text.trim(),
                  address: addressCtrl.text.trim(),
                  phone: phoneCtrl.text.trim(),
                  email: emailCtrl.text.trim(),
                  gstin: gstinCtrl.text.trim(),
                  pan: panCtrl.text.trim(),
                  updatedAt: DateTime.now(),
                );
                await auth.updateBusinessProfile(updated);
              }
              if (ctx.mounted) Navigator.pop(ctx);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  Widget _buildUserAccountCard(BuildContext context, AuthService auth) {
    final user = auth.currentUser;
    final theme = Theme.of(context);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: theme.colorScheme.outlineVariant.withOpacity(0.5)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              radius: 24,
              backgroundColor: const Color(0xFF1E3A8A),
              child: Text(
                (user?.displayName.isNotEmpty == true) ? user!.displayName[0].toUpperCase() : 'U',
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Flexible(
                        child: Text(
                          user?.displayName ?? 'Cloud User',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.green.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.check_circle, size: 12, color: Colors.green),
                            SizedBox(width: 4),
                            Text(
                              'Synced',
                              style: TextStyle(fontSize: 10, color: Colors.green, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    user?.email ?? 'Google Account Connected',
                    style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.6)),
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            TactileBounceButton(
              child: OutlinedButton.icon(
                onPressed: () async {
                  final confirm = await showDialog<bool>(
                    context: context,
                    builder: (ctx) => AlertDialog(
                      title: const Text('Sign Out'),
                      content: const Text('Are you sure you want to sign out and return to the login screen?'),
                      actions: [
                        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
                        ElevatedButton(
                          onPressed: () => Navigator.pop(ctx, true),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
                          child: const Text('Sign Out'),
                        ),
                      ],
                    ),
                  );
                  if (confirm == true) {
                    await auth.signOut();
                  }
                },
                icon: const Icon(Icons.logout, size: 16, color: Colors.red),
                label: const Text('Sign Out', style: TextStyle(color: Colors.red, fontSize: 12, fontWeight: FontWeight.bold)),
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: Colors.red),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
