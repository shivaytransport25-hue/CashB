import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';

import '../core/animations/bounce_button.dart';
import '../services/auth_service.dart';
import '../services/backup_service.dart';

class BackupScreen extends StatefulWidget {
  const BackupScreen({super.key});

  @override
  State<BackupScreen> createState() => _BackupScreenState();
}

class _BackupScreenState extends State<BackupScreen> {
  bool _isProcessing = false;
  bool _isCloudProcessing = false;

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final biz = auth.currentBusiness;

    return Scaffold(
      appBar: AppBar(title: const Text('Backup & Disaster Recovery')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Card(
              elevation: 1,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.shield, color: Colors.green, size: 28),
                        const SizedBox(width: 12),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Cryptographic Backup Engine', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                            Text('SHA-256 Tamper Verification Active', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                          ],
                        ),
                      ],
                    ),
                    const Divider(height: 24),
                    const Text(
                      'All accounts, historical transactions, categories, parties, and audit trail are encrypted and packed into a single portable backup file with permanent Cloud Vault storage.',
                      style: TextStyle(fontSize: 13, height: 1.5),
                    ),
                    const SizedBox(height: 20),
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        TactileBounceButton(
                          child: ElevatedButton.icon(
                            onPressed: _isCloudProcessing ? null : () => _createCloudBackup(context, auth),
                            icon: _isCloudProcessing
                                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                : const Icon(Icons.cloud_upload),
                            label: const Text('Save to Cloud Vault (Long-Term)'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.teal[800],
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                            ),
                          ),
                        ),
                        TactileBounceButton(
                          child: OutlinedButton.icon(
                            onPressed: _isProcessing ? null : () => _createBackup(context, auth),
                            icon: _isProcessing
                                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                                : const Icon(Icons.download),
                            label: const Text('Export JSON File'),
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                            ),
                          ),
                        ),
                      ],
                    ),
                    if (biz != null) ...[
                      const SizedBox(height: 16),
                      Text(
                        'Active Business: ${biz.name} • ID: ${biz.id}',
                        style: const TextStyle(fontSize: 11, color: Colors.grey),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _createCloudBackup(BuildContext context, AuthService auth) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    setState(() => _isCloudProcessing = true);
    try {
      final res = await BackupService.uploadBackupToCloudVault(biz);
      if (mounted) {
        if (res.success) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: Colors.green[800],
              content: Text('Successfully archived in Cloud Vault!\nSnapshot ID: ${res.backupId}'),
            ),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(backgroundColor: Colors.red[800], content: Text('Cloud backup note: ${res.error}')),
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
      if (mounted) setState(() => _isCloudProcessing = false);
    }
  }

  Future<void> _createBackup(BuildContext context, AuthService auth) async {
    final biz = auth.currentBusiness;
    if (biz == null) return;

    setState(() => _isProcessing = true);
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
          SnackBar(backgroundColor: Colors.red, content: Text('Error: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }
}
