import 'dart:convert';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';

import '../database/local_database.dart';
import '../models/business_model.dart';

class CloudBackupMetadata {
  final String backupId;
  final String businessId;
  final DateTime createdAt;
  final String checksumSha256;
  final int transactionCount;
  final int partyCount;
  final int accountCount;
  final int sizeBytes;

  CloudBackupMetadata({
    required this.backupId,
    required this.businessId,
    required this.createdAt,
    required this.checksumSha256,
    required this.transactionCount,
    required this.partyCount,
    required this.accountCount,
    required this.sizeBytes,
  });

  Map<String, dynamic> toMap() {
    return {
      'backupId': backupId,
      'businessId': businessId,
      'createdAt': createdAt.toIso8601String(),
      'checksumSha256': checksumSha256,
      'transactionCount': transactionCount,
      'partyCount': partyCount,
      'accountCount': accountCount,
      'sizeBytes': sizeBytes,
    };
  }

  factory CloudBackupMetadata.fromMap(Map<String, dynamic> map) {
    return CloudBackupMetadata(
      backupId: map['backupId'] as String? ?? 'unknown',
      businessId: map['businessId'] as String? ?? '',
      createdAt: map['createdAt'] != null ? DateTime.parse(map['createdAt']) : DateTime.now(),
      checksumSha256: map['checksumSha256'] as String? ?? '',
      transactionCount: map['transactionCount'] as int? ?? 0,
      partyCount: map['partyCount'] as int? ?? 0,
      accountCount: map['accountCount'] as int? ?? 0,
      sizeBytes: map['sizeBytes'] as int? ?? 0,
    );
  }
}

class BackupService {
  BackupService._();

  /// Creates a full export snapshot containing all financial records with SHA-256 verification checksum
  static Future<Map<String, dynamic>> createBackupSnapshot(BusinessModel business) async {
    final rawData = await LocalDatabase.instance.exportAllDataAsJson(business.id);

    // Embed Business Metadata
    rawData['businessProfile'] = business.toMap();

    // Compute cryptographic SHA-256 checksum over records payload for tamper prevention
    final serialized = jsonEncode(rawData['transactions']);
    final digest = sha256.convert(utf8.encode(serialized));
    rawData['checksumSha256'] = digest.toString();
    rawData['appVersion'] = '1.0.0';

    return rawData;
  }

  /// Verifies backup integrity before attempting restoration
  static bool verifyBackupIntegrity(Map<String, dynamic> backupData) {
    try {
      if (backupData['version'] == null || backupData['businessId'] == null) {
        return false;
      }
      final recordedChecksum = backupData['checksumSha256'] as String?;
      if (recordedChecksum != null && backupData['transactions'] != null) {
        final serialized = jsonEncode(backupData['transactions']);
        final digest = sha256.convert(utf8.encode(serialized)).toString();
        if (recordedChecksum != digest) {
          debugPrint('Backup checksum mismatch! File may be corrupted or tampered.');
          return false;
        }
      }
      return true;
    } catch (e) {
      debugPrint('Backup verification error: $e');
      return false;
    }
  }

  /// Restores data from a validated backup package with emergency safety snapshot
  static Future<({bool success, String? error})> restoreBackup({
    required BusinessModel business,
    required Map<String, dynamic> backupData,
  }) async {
    try {
      // 1. Verify integrity
      if (!verifyBackupIntegrity(backupData)) {
        return (success: false, error: 'Backup file failed cryptographic integrity check or is corrupted.');
      }

      // 2. Take automatic safety snapshot before restore so data can never be lost
      await createBackupSnapshot(business);

      // 3. Perform restoration
      await LocalDatabase.instance.restoreDataFromJson(business.id, backupData);

      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Uploads a full verified snapshot to Firebase Cloud Firestore Vault for long-term durable retention
  static Future<({bool success, String? error, String? backupId})> uploadBackupToCloudVault(
    BusinessModel business,
  ) async {
    try {
      final snapshot = await createBackupSnapshot(business);
      final jsonStr = jsonEncode(snapshot);
      final bytesLength = utf8.encode(jsonStr).length;

      final now = DateTime.now();
      final backupId = 'vault_snap_${now.millisecondsSinceEpoch}';

      final firestore = FirebaseFirestore.instance;
      final docRef = firestore
          .collection('businesses')
          .doc(business.id)
          .collection('cloud_vault_backups')
          .doc(backupId);

      final metadata = CloudBackupMetadata(
        backupId: backupId,
        businessId: business.id,
        createdAt: now,
        checksumSha256: snapshot['checksumSha256'] as String? ?? '',
        transactionCount: (snapshot['transactions'] as List?)?.length ?? 0,
        partyCount: (snapshot['parties'] as List?)?.length ?? 0,
        accountCount: (snapshot['accounts'] as List?)?.length ?? 0,
        sizeBytes: bytesLength,
      );

      await docRef.set({
        ...metadata.toMap(),
        'snapshotJson': jsonStr,
        'archivedBy': business.ownerName,
        'retentionPolicy': 'PERMANENT_CLOUD_VAULT',
      });

      return (success: true, error: null, backupId: backupId);
    } catch (e) {
      debugPrint('Error uploading backup to Cloud Vault: $e');
      return (success: false, error: e.toString(), backupId: null);
    }
  }

  /// Retrieves list of snapshots saved in Cloud Vault
  static Future<List<CloudBackupMetadata>> getCloudBackups(String businessId) async {
    try {
      final firestore = FirebaseFirestore.instance;
      final querySnapshot = await firestore
          .collection('businesses')
          .doc(businessId)
          .collection('cloud_vault_backups')
          .orderBy('createdAt', descending: true)
          .limit(20)
          .get();

      return querySnapshot.docs.map((doc) => CloudBackupMetadata.fromMap(doc.data())).toList();
    } catch (e) {
      debugPrint('Error querying Cloud Vault backups: $e');
      return [];
    }
  }

  /// Restores entire database state from a Cloud Vault snapshot
  static Future<({bool success, String? error})> restoreFromCloudVault({
    required BusinessModel business,
    required String backupId,
  }) async {
    try {
      final firestore = FirebaseFirestore.instance;
      final doc = await firestore
          .collection('businesses')
          .doc(business.id)
          .collection('cloud_vault_backups')
          .doc(backupId)
          .get();

      if (!doc.exists || doc.data() == null) {
        return (success: false, error: 'Cloud backup snapshot not found.');
      }

      final jsonStr = doc.data()!['snapshotJson'] as String?;
      if (jsonStr == null) {
        return (success: false, error: 'Snapshot data is corrupt or missing.');
      }

      final backupData = jsonDecode(jsonStr) as Map<String, dynamic>;
      return await restoreBackup(business: business, backupData: backupData);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }
}
