import 'dart:async';
import 'dart:convert';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart';

import '../database/local_database.dart';
import '../models/sync_queue_model.dart';
import '../models/transaction_model.dart';

enum SyncConnectionState {
  onlineSynced,
  syncing,
  offline,
  syncError,
}

/// Centralized Real-time Synchronization and Offline Queue Engine
class SyncEngine extends ChangeNotifier {
  static final SyncEngine instance = SyncEngine._init();

  SyncConnectionState _state = SyncConnectionState.offline;
  DateTime? _lastSyncTime;
  int _pendingCount = 0;
  String? _lastError;
  StreamSubscription? _remoteTxnSubscription;
  Timer? _periodicSyncTimer;
  String? _currentBusinessId;

  SyncEngine._init();

  SyncConnectionState get state => _state;
  DateTime? get lastSyncTime => _lastSyncTime;
  int get pendingCount => _pendingCount;
  String? get lastError => _lastError;
  bool get isOnline => _state == SyncConnectionState.onlineSynced || _state == SyncConnectionState.syncing;

  /// Initializes real-time listener and background sync worker for a business
  void initialize(String businessId) {
    _currentBusinessId = businessId;
    _startPeriodicWorker();
    _subscribeToCloudChanges(businessId);
    triggerSync();
  }

  void disposeEngine() {
    _remoteTxnSubscription?.cancel();
    _periodicSyncTimer?.cancel();
  }

  void _startPeriodicWorker() {
    _periodicSyncTimer?.cancel();
    // Check and drain sync queue every 15 seconds or upon reconnect
    _periodicSyncTimer = Timer.periodic(const Duration(seconds: 15), (_) {
      triggerSync();
    });
  }

  /// Triggers an immediate sync cycle
  Future<void> triggerSync() async {
    if (_currentBusinessId == null) return;
    final businessId = _currentBusinessId!;

    try {
      final queue = await LocalDatabase.instance.getPendingSyncQueue(businessId);
      _pendingCount = queue.length;

      if (queue.isEmpty) {
        _state = SyncConnectionState.onlineSynced;
        _lastSyncTime = DateTime.now();
        notifyListeners();
        return;
      }

      _state = SyncConnectionState.syncing;
      notifyListeners();

      final firestore = FirebaseFirestore.instance;

      for (final item in queue) {
        try {
          await _processQueueItem(firestore, item);
          await LocalDatabase.instance.removeSyncItem(item.queueId);
        } catch (e) {
          debugPrint('Error syncing queue item ${item.queueId}: $e');
          await LocalDatabase.instance.updateSyncItemStatus(
            item.queueId,
            'FAILED',
            error: e.toString(),
          );
        }
      }

      final remaining = await LocalDatabase.instance.getPendingSyncQueue(businessId);
      _pendingCount = remaining.length;
      _lastSyncTime = DateTime.now();
      _state = remaining.isEmpty ? SyncConnectionState.onlineSynced : SyncConnectionState.syncError;
      notifyListeners();
    } catch (e) {
      debugPrint('Sync engine offline or unconfigured: $e');
      _state = SyncConnectionState.offline;
      _lastError = e.toString();
      notifyListeners();
    }
  }

  /// Process an individual queue item to Cloud Firestore with idempotency check
  Future<void> _processQueueItem(FirebaseFirestore firestore, SyncQueueModel item) async {
    final payload = jsonDecode(item.payloadJson) as Map<String, dynamic>;

    if (item.entityType == 'transaction') {
      final docRef = firestore
          .collection('businesses')
          .doc(item.businessId)
          .collection('transactions')
          .doc(item.entityId);

      // Verify idempotency
      final existing = await docRef.get();
      if (!existing.exists || item.operationType == 'UPDATE') {
        await docRef.set(payload, SetOptions(merge: true));
      }
    } else if (item.entityType == 'account') {
      await firestore
          .collection('businesses')
          .doc(item.businessId)
          .collection('accounts')
          .doc(item.entityId)
          .set(payload, SetOptions(merge: true));
    } else if (item.entityType == 'party') {
      await firestore
          .collection('businesses')
          .doc(item.businessId)
          .collection('parties')
          .doc(item.entityId)
          .set(payload, SetOptions(merge: true));
    } else if (item.entityType == 'category') {
      await firestore
          .collection('businesses')
          .doc(item.businessId)
          .collection('categories')
          .doc(item.entityId)
          .set(payload, SetOptions(merge: true));
    }
  }

  /// Real-time subscription to Cloud Firestore changes from other devices
  void _subscribeToCloudChanges(String businessId) {
    _remoteTxnSubscription?.cancel();

    try {
      final firestore = FirebaseFirestore.instance;
      _remoteTxnSubscription = firestore
          .collection('businesses')
          .doc(businessId)
          .collection('transactions')
          .snapshots()
          .listen(
        (snapshot) async {
          for (final change in snapshot.docChanges) {
            if (change.type == DocumentChangeType.added ||
                change.type == DocumentChangeType.modified) {
              final data = change.doc.data();
              if (data != null) {
                try {
                  final remoteTxn = TransactionModel.fromFirestore(data);
                  // Upsert to local SQLite database so offline cache is updated
                  await LocalDatabase.instance.upsertTransaction(remoteTxn);
                } catch (e) {
                  debugPrint('Failed to parse remote transaction: $e');
                }
              }
            }
          }
          _state = SyncConnectionState.onlineSynced;
          _lastSyncTime = DateTime.now();
          notifyListeners();
        },
        onError: (err) {
          debugPrint('Firestore real-time subscription error: $err');
          _state = SyncConnectionState.offline;
          notifyListeners();
        },
      );
    } catch (e) {
      debugPrint('Cloud Firestore not initialized or offline: $e');
      _state = SyncConnectionState.offline;
      notifyListeners();
    }
  }
}

