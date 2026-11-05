import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

import '../core/constants/enums.dart';
import '../models/account_model.dart';
import '../models/category_model.dart';
import '../models/party_model.dart';
import '../models/sync_queue_model.dart';
import '../models/transaction_model.dart';

/// Cross-platform local SQLite database for offline-first accounting
class LocalDatabase {
  static final LocalDatabase instance = LocalDatabase._init();
  static Database? _database;

  LocalDatabase._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('cash_book_master.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    // Enable FFI for Desktop (Windows, macOS, Linux)
    if (!kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS)) {
      sqfliteFfiInit();
      databaseFactory = databaseFactoryFfi;
    }

    String path;
    if (!kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS)) {
      final appSupportDir = await getApplicationSupportDirectory();
      path = join(appSupportDir.path, filePath);
    } else {
      final dbPath = await getDatabasesPath();
      path = join(dbPath, filePath);
    }

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
    // 1. Accounts Table
    await db.execute('''
      CREATE TABLE local_accounts (
        id TEXT PRIMARY KEY,
        business_id TEXT NOT NULL,
        account_name TEXT NOT NULL,
        account_type TEXT NOT NULL,
        opening_balance_paise INTEGER NOT NULL DEFAULT 0,
        opening_date TEXT NOT NULL,
        current_balance_paise INTEGER NOT NULL DEFAULT 0,
        bank_name TEXT,
        account_number TEXT,
        ifsc_code TEXT,
        branch TEXT,
        allow_negative_balance INTEGER NOT NULL DEFAULT 0,
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        sync_status TEXT NOT NULL DEFAULT 'SYNCED'
      )
    ''');
    await db.execute('CREATE INDEX idx_accounts_biz ON local_accounts(business_id)');

    // 2. Categories Table
    await db.execute('''
      CREATE TABLE local_categories (
        id TEXT PRIMARY KEY,
        business_id TEXT NOT NULL,
        name TEXT NOT NULL,
        type TEXT NOT NULL,
        parent_category_id TEXT,
        icon_name TEXT,
        color_hex TEXT,
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        sync_status TEXT NOT NULL DEFAULT 'SYNCED'
      )
    ''');
    await db.execute('CREATE INDEX idx_categories_biz ON local_categories(business_id)');

    // 3. Parties Table
    await db.execute('''
      CREATE TABLE local_parties (
        id TEXT PRIMARY KEY,
        business_id TEXT NOT NULL,
        name TEXT NOT NULL,
        party_type TEXT NOT NULL,
        phone TEXT,
        email TEXT,
        address TEXT,
        opening_balance_paise INTEGER NOT NULL DEFAULT 0,
        opening_balance_type TEXT NOT NULL DEFAULT 'receivable',
        current_balance_paise INTEGER NOT NULL DEFAULT 0,
        gstin TEXT,
        is_active INTEGER NOT NULL DEFAULT 1,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        sync_status TEXT NOT NULL DEFAULT 'SYNCED'
      )
    ''');
    await db.execute('CREATE INDEX idx_parties_biz ON local_parties(business_id)');

    // 4. Transactions Table
    await db.execute('''
      CREATE TABLE local_transactions (
        id TEXT PRIMARY KEY,
        business_id TEXT NOT NULL,
        voucher_number TEXT NOT NULL,
        transaction_date TEXT NOT NULL,
        transaction_time TEXT NOT NULL,
        transaction_type TEXT NOT NULL,
        account_id TEXT NOT NULL,
        target_account_id TEXT,
        transfer_id TEXT,
        category_id TEXT,
        party_id TEXT,
        party_name TEXT,
        description TEXT,
        payment_mode TEXT NOT NULL,
        amount_paise INTEGER NOT NULL,
        reference_number TEXT,
        attachment_urls TEXT,
        status TEXT NOT NULL DEFAULT 'posted',
        reversal_reason TEXT,
        reversed_transaction_id TEXT,
        original_transaction_id TEXT,
        created_by TEXT NOT NULL,
        created_by_name TEXT NOT NULL,
        device_id TEXT NOT NULL,
        idempotency_key TEXT UNIQUE NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        sync_status TEXT NOT NULL DEFAULT 'SYNCED'
      )
    ''');
    await db.execute('CREATE INDEX idx_txn_biz_date ON local_transactions(business_id, transaction_date)');
    await db.execute('CREATE INDEX idx_txn_acc ON local_transactions(business_id, account_id)');
    await db.execute('CREATE INDEX idx_txn_party ON local_transactions(business_id, party_id)');

    // 5. Offline Sync Queue
    await db.execute('''
      CREATE TABLE sync_queue (
        queue_id TEXT PRIMARY KEY,
        business_id TEXT NOT NULL,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        operation_type TEXT NOT NULL,
        payload_json TEXT NOT NULL,
        idempotency_key TEXT UNIQUE NOT NULL,
        status TEXT NOT NULL DEFAULT 'PENDING',
        retry_count INTEGER NOT NULL DEFAULT 0,
        last_error TEXT,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      )
    ''');
    await db.execute('CREATE INDEX idx_sync_status ON sync_queue(status, created_at)');

    // 6. Audit Logs
    await db.execute('''
      CREATE TABLE local_audit_logs (
        id TEXT PRIMARY KEY,
        business_id TEXT NOT NULL,
        action TEXT NOT NULL,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        performed_by TEXT NOT NULL,
        performed_by_name TEXT NOT NULL,
        details_json TEXT NOT NULL,
        created_at TEXT NOT NULL
      )
    ''');
    await db.execute('CREATE INDEX idx_audit_biz ON local_audit_logs(business_id, created_at)');
  }

  // ----------------------- ACCOUNTS -----------------------

  Future<List<AccountModel>> getAccounts(String businessId) async {
    final db = await database;
    final maps = await db.query(
      'local_accounts',
      where: 'business_id = ? AND is_active = 1',
      whereArgs: [businessId],
      orderBy: 'account_name ASC',
    );
    return maps.map((e) => AccountModel.fromMap(e)).toList();
  }

  Future<void> upsertAccount(AccountModel account) async {
    final db = await database;
    await db.insert(
      'local_accounts',
      account.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> updateAccountBalance(String accountId, int newBalancePaise) async {
    final db = await database;
    await db.update(
      'local_accounts',
      {
        'current_balance_paise': newBalancePaise,
        'updated_at': DateTime.now().toIso8601String(),
      },
      where: 'id = ?',
      whereArgs: [accountId],
    );
  }

  // ----------------------- CATEGORIES -----------------------

  Future<List<CategoryModel>> getCategories(String businessId, {String? type}) async {
    final db = await database;
    String? whereClause = 'business_id = ? AND is_active = 1';
    List<dynamic> whereArgs = [businessId];

    if (type != null) {
      whereClause += ' AND type = ?';
      whereArgs.add(type);
    }

    final maps = await db.query(
      'local_categories',
      where: whereClause,
      whereArgs: whereArgs,
      orderBy: 'name ASC',
    );
    return maps.map((e) => CategoryModel.fromMap(e)).toList();
  }

  Future<void> upsertCategory(CategoryModel category) async {
    final db = await database;
    await db.insert(
      'local_categories',
      category.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  // ----------------------- PARTIES -----------------------

  Future<List<PartyModel>> getParties(String businessId) async {
    final db = await database;
    final maps = await db.query(
      'local_parties',
      where: 'business_id = ? AND is_active = 1',
      whereArgs: [businessId],
      orderBy: 'name ASC',
    );
    return maps.map((e) => PartyModel.fromMap(e)).toList();
  }

  Future<void> upsertParty(PartyModel party) async {
    final db = await database;
    await db.insert(
      'local_parties',
      party.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> updatePartyBalance(String partyId, int newBalancePaise) async {
    final db = await database;
    await db.update(
      'local_parties',
      {
        'current_balance_paise': newBalancePaise,
        'updated_at': DateTime.now().toIso8601String(),
      },
      where: 'id = ?',
      whereArgs: [partyId],
    );
  }

  // ----------------------- TRANSACTIONS -----------------------

  Future<List<TransactionModel>> getTransactions(
    String businessId, {
    String? accountId,
    String? categoryId,
    String? partyId,
    String? startDate,
    String? endDate,
    TransactionStatus? status,
    int? limit,
    int? offset,
  }) async {
    final db = await database;
    String whereClause = 'business_id = ?';
    List<dynamic> whereArgs = [businessId];

    if (accountId != null) {
      whereClause += ' AND (account_id = ? OR target_account_id = ?)';
      whereArgs.add(accountId);
      whereArgs.add(accountId);
    }

    if (categoryId != null) {
      whereClause += ' AND category_id = ?';
      whereArgs.add(categoryId);
    }

    if (partyId != null) {
      whereClause += ' AND party_id = ?';
      whereArgs.add(partyId);
    }

    if (status != null) {
      whereClause += ' AND status = ?';
      whereArgs.add(status.name);
    }

    final maps = await db.query(
      'local_transactions',
      where: whereClause,
      whereArgs: whereArgs,
      orderBy: 'created_at DESC',
      limit: limit,
      offset: offset,
    );

    return maps.map((e) => TransactionModel.fromMap(e)).toList();
  }

  Future<void> upsertTransaction(TransactionModel txn) async {
    final db = await database;
    await db.insert(
      'local_transactions',
      txn.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> deleteTransaction(String id) async {
    final db = await database;
    await db.delete(
      'local_transactions',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<TransactionModel?> getTransactionById(String id) async {
    final db = await database;
    final maps = await db.query(
      'local_transactions',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );
    if (maps.isEmpty) return null;
    return TransactionModel.fromMap(maps.first);
  }

  Future<int> getTransactionCount(String businessId) async {
    final db = await database;
    final result = await db.rawQuery(
      'SELECT COUNT(*) as count FROM local_transactions WHERE business_id = ?',
      [businessId],
    );
    return Sqflite.firstIntValue(result) ?? 0;
  }

  // ----------------------- SYNC QUEUE -----------------------

  Future<void> enqueueSyncItem(SyncQueueModel item) async {
    final db = await database;
    await db.insert(
      'sync_queue',
      item.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<SyncQueueModel>> getPendingSyncQueue(String businessId) async {
    final db = await database;
    final maps = await db.query(
      'sync_queue',
      where: 'business_id = ? AND (status = ? OR status = ?)',
      whereArgs: [businessId, 'PENDING', 'FAILED'],
      orderBy: 'created_at ASC',
    );
    return maps.map((e) => SyncQueueModel.fromMap(e)).toList();
  }

  Future<void> updateSyncItemStatus(String queueId, String status, {String? error}) async {
    final db = await database;
    final updateData = <String, dynamic>{
      'status': status,
      'updated_at': DateTime.now().toIso8601String(),
    };
    if (error != null) {
      updateData['last_error'] = error;
    }
    await db.update(
      'sync_queue',
      updateData,
      where: 'queue_id = ?',
      whereArgs: [queueId],
    );
  }

  Future<void> removeSyncItem(String queueId) async {
    final db = await database;
    await db.delete(
      'sync_queue',
      where: 'queue_id = ?',
      whereArgs: [queueId],
    );
  }

  // ----------------------- AUDIT LOGS -----------------------

  Future<void> insertAuditLog(AuditLogModel log) async {
    final db = await database;
    await db.insert('local_audit_logs', log.toMap());
  }

  Future<List<AuditLogModel>> getAuditLogs(String businessId, {int limit = 50}) async {
    final db = await database;
    final maps = await db.query(
      'local_audit_logs',
      where: 'business_id = ?',
      whereArgs: [businessId],
      orderBy: 'created_at DESC',
      limit: limit,
    );
    return maps.map((e) => AuditLogModel.fromMap(e)).toList();
  }

  // ----------------------- FULL BACKUP & RESTORE -----------------------

  Future<Map<String, dynamic>> exportAllDataAsJson(String businessId) async {
    final db = await database;

    final accounts = await db.query('local_accounts', where: 'business_id = ?', whereArgs: [businessId]);
    final categories = await db.query('local_categories', where: 'business_id = ?', whereArgs: [businessId]);
    final parties = await db.query('local_parties', where: 'business_id = ?', whereArgs: [businessId]);
    final transactions = await db.query('local_transactions', where: 'business_id = ?', whereArgs: [businessId]);
    final auditLogs = await db.query('local_audit_logs', where: 'business_id = ?', whereArgs: [businessId]);

    return {
      'version': '1.0',
      'businessId': businessId,
      'exportedAt': DateTime.now().toIso8601String(),
      'accounts': accounts,
      'categories': categories,
      'parties': parties,
      'transactions': transactions,
      'auditLogs': auditLogs,
    };
  }

  Future<void> restoreDataFromJson(String businessId, Map<String, dynamic> backupData) async {
    final db = await database;
    await db.transaction((txn) async {
      // 1. Restore Accounts
      if (backupData['accounts'] != null) {
        for (final item in backupData['accounts'] as List) {
          await txn.insert('local_accounts', Map<String, dynamic>.from(item), conflictAlgorithm: ConflictAlgorithm.replace);
        }
      }

      // 2. Restore Categories
      if (backupData['categories'] != null) {
        for (final item in backupData['categories'] as List) {
          await txn.insert('local_categories', Map<String, dynamic>.from(item), conflictAlgorithm: ConflictAlgorithm.replace);
        }
      }

      // 3. Restore Parties
      if (backupData['parties'] != null) {
        for (final item in backupData['parties'] as List) {
          await txn.insert('local_parties', Map<String, dynamic>.from(item), conflictAlgorithm: ConflictAlgorithm.replace);
        }
      }

      // 4. Restore Transactions
      if (backupData['transactions'] != null) {
        for (final item in backupData['transactions'] as List) {
          await txn.insert('local_transactions', Map<String, dynamic>.from(item), conflictAlgorithm: ConflictAlgorithm.replace);
        }
      }
    });
  }
}

