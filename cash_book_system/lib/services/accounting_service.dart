import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';

import '../accounting/financial_engine.dart';
import '../core/constants/enums.dart';
import '../core/utils/date_formatter.dart';
import '../database/local_database.dart';
import '../models/account_model.dart';
import '../models/category_model.dart';
import '../models/party_model.dart';
import '../models/sync_queue_model.dart';
import '../models/transaction_model.dart';
import '../sync/sync_engine.dart';

class AccountingService extends ChangeNotifier {
  final _uuid = const Uuid();
  final LocalDatabase _db = LocalDatabase.instance;
  final SyncEngine _syncEngine = SyncEngine.instance;

  List<AccountModel> _accounts = [];
  List<CategoryModel> _categories = [];
  List<PartyModel> _parties = [];
  List<TransactionModel> _transactions = [];
  bool _isLoading = false;
  String _currentBusinessId = 'biz_shivay_001';

  String get currentBusinessId => _currentBusinessId;

  List<AccountModel> get accounts => _accounts;
  List<CategoryModel> get categories => _categories;
  List<PartyModel> get parties => _parties;
  List<TransactionModel> get transactions => _transactions;
  bool get isLoading => _isLoading;

  int get totalCashBalancePaise {
    return _accounts
        .where((a) => a.accountType == AccountType.cash && a.isActive)
        .fold(0, (sum, a) => sum + a.currentBalancePaise);
  }

  int get totalBankBalancePaise {
    return _accounts
        .where((a) => a.accountType == AccountType.bank && a.isActive)
        .fold(0, (sum, a) => sum + a.currentBalancePaise);
  }

  int get totalAvailableBalancePaise => totalCashBalancePaise + totalBankBalancePaise;

  int get totalReceivablesPaise {
    return _parties
        .where((p) => p.currentBalancePaise > 0)
        .fold(0, (sum, p) => sum + p.currentBalancePaise);
  }

  int get totalPayablesPaise {
    return _parties
        .where((p) => p.currentBalancePaise < 0)
        .fold(0, (sum, p) => sum + p.currentBalancePaise.abs());
  }

  FinancialSummary get todaySummary {
    final today = DateFormatter.formatDate(DateTime.now());
    final todayTxns = _transactions.where((t) => t.transactionDate == today).toList();
    return FinancialEngine.calculateSummary(todayTxns);
  }

  FinancialSummary get monthlySummary {
    final now = DateTime.now();
    final monthPrefix = '-${now.month.toString().padLeft(2, '0')}-${now.year}';
    final monthTxns = _transactions.where((t) => t.transactionDate.endsWith(monthPrefix)).toList();
    return FinancialEngine.calculateSummary(monthTxns);
  }

  FinancialSummary get overallSummary {
    return FinancialEngine.calculateSummary(_transactions);
  }

  Future<void> loadBusinessData(String businessId) async {
    _currentBusinessId = businessId;
    _isLoading = true;
    notifyListeners();

    try {
      _accounts = await _db.getAccounts(businessId);
      _categories = await _db.getCategories(businessId);
      _parties = await _db.getParties(businessId);
      _transactions = await _db.getTransactions(businessId);

      // If database is brand new, seed standard initial accounts and categories
      if (_accounts.isEmpty && _categories.isEmpty) {
        await _seedInitialData(businessId);
        _accounts = await _db.getAccounts(businessId);
        _categories = await _db.getCategories(businessId);
        _parties = await _db.getParties(businessId);
        _transactions = await _db.getTransactions(businessId);
      }

      _syncEngine.initialize(businessId);
    } catch (e) {
      debugPrint('Error loading accounting data: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> _seedInitialData(String businessId) async {
    final now = DateTime.now();
    final todayStr = DateFormatter.formatDate(now);

    // 1. Initial Accounts
    final mainCash = AccountModel(
      id: 'acc_main_cash',
      businessId: businessId,
      accountName: 'Main Cash Counter',
      accountType: AccountType.cash,
      openingBalancePaise: 5000000, // ₹50,000.00
      openingDate: todayStr,
      currentBalancePaise: 5000000,
      createdAt: now,
      updatedAt: now,
    );

    final pettyCash = AccountModel(
      id: 'acc_petty_cash',
      businessId: businessId,
      accountName: 'Office Petty Cash',
      accountType: AccountType.cash,
      openingBalancePaise: 1000000, // ₹10,000.00
      openingDate: todayStr,
      currentBalancePaise: 1000000,
      createdAt: now,
      updatedAt: now,
    );

    final sbiBank = AccountModel(
      id: 'acc_sbi_bank',
      businessId: businessId,
      accountName: 'State Bank of India (Current)',
      accountType: AccountType.bank,
      openingBalancePaise: 15000000, // ₹1,50,000.00
      openingDate: todayStr,
      currentBalancePaise: 15000000,
      bankName: 'State Bank of India',
      accountNumber: 'XXXX-XXXX-9821',
      ifscCode: 'SBIN0001234',
      branch: 'Ring Road Branch',
      createdAt: now,
      updatedAt: now,
    );

    final hdfcBank = AccountModel(
      id: 'acc_hdfc_bank',
      businessId: businessId,
      accountName: 'HDFC Bank (Operations)',
      accountType: AccountType.bank,
      openingBalancePaise: 7500000, // ₹75,000.00
      openingDate: todayStr,
      currentBalancePaise: 7500000,
      bankName: 'HDFC Bank Ltd',
      accountNumber: 'XXXX-XXXX-4512',
      ifscCode: 'HDFC0005678',
      branch: 'Textile Market Branch',
      createdAt: now,
      updatedAt: now,
    );

    await _db.upsertAccount(mainCash);
    await _db.upsertAccount(pettyCash);
    await _db.upsertAccount(sbiBank);
    await _db.upsertAccount(hdfcBank);

    // 2. Initial Categories
    final categoriesToSeed = [
      ('cat_sales', 'Sales & Revenue', 'income', 'payments', '#2E7D32'),
      ('cat_freight', 'Freight & Transport Income', 'income', 'local_shipping', '#1B5E20'),
      ('cat_commission', 'Commission Income', 'income', 'monetization_on', '#388E3C'),
      ('cat_other_inc', 'Other Income', 'income', 'account_balance_wallet', '#4CAF50'),
      ('cat_fuel', 'Diesel & Fuel Expenses', 'expense', 'local_gas_station', '#C62828'),
      ('cat_maintenance', 'Vehicle Maintenance & Repairs', 'expense', 'build', '#D32F2F'),
      ('cat_salary', 'Staff & Driver Salary', 'expense', 'badge', '#E53935'),
      ('cat_toll', 'Toll Tax & Border Charges', 'expense', 'toll', '#F44336'),
      ('cat_rent', 'Office Rent & Rates', 'expense', 'apartment', '#D84315'),
      ('cat_tea_food', 'Tea, Refreshment & Food', 'expense', 'restaurant', '#BF360C'),
      ('cat_office_exp', 'General Office Expenses', 'expense', 'store', '#4E342E'),
    ];

    for (final c in categoriesToSeed) {
      await _db.upsertCategory(CategoryModel(
        id: c.$1,
        businessId: businessId,
        name: c.$2,
        type: c.$3,
        iconName: c.$4,
        colorHex: c.$5,
        createdAt: now,
        updatedAt: now,
      ));
    }

    // 3. Initial Parties
    final partiesToSeed = [
      PartyModel(
        id: 'pty_reliance',
        businessId: businessId,
        name: 'Reliance Logistics Client',
        partyType: PartyType.customer,
        phone: '+91 98980 11223',
        address: 'Hazira Complex, Surat',
        openingBalancePaise: 4500000, // Receivable ₹45,000
        currentBalancePaise: 4500000,
        createdAt: now,
        updatedAt: now,
      ),
      PartyModel(
        id: 'pty_iocl',
        businessId: businessId,
        name: 'Indian Oil Depot (Supplier)',
        partyType: PartyType.supplier,
        phone: '+91 94260 44556',
        address: 'Magdalla Port Road',
        openingBalancePaise: 3000000, // Payable ₹30,000
        currentBalancePaise: -3000000,
        createdAt: now,
        updatedAt: now,
      ),
      PartyModel(
        id: 'pty_mukesh_driver',
        businessId: businessId,
        name: 'Mukeshbhai (Truck Driver GJ-05-XX-1234)',
        partyType: PartyType.employee,
        phone: '+91 97250 88990',
        currentBalancePaise: 0,
        createdAt: now,
        updatedAt: now,
      ),
    ];

    for (final p in partiesToSeed) {
      await _db.upsertParty(p);
    }
  }

  /// Generates the next sequential voucher number (e.g. VCH-2026-0001)
  String generateNextVoucherNumber() {
    final year = DateTime.now().year;
    final count = _transactions.length + 1;
    final seqStr = count.toString().padLeft(4, '0');
    return 'VCH-$year-$seqStr';
  }

  /// Records an Income transaction (Cash Received or Bank Received)
  Future<({bool success, String? error})> recordIncome({
    required String accountId,
    required int amountPaise,
    required String paymentMode,
    String? categoryId,
    String? partyId,
    String? narration,
    String? referenceNumber,
    List<String> attachments = const [],
    required String createdBy,
    required String createdByName,
    required String deviceId,
    DateTime? customDate,
  }) async {
    try {
      final now = customDate ?? DateTime.now();
      final account = _accounts.firstWhere((a) => a.id == accountId);
      final category = categoryId != null
          ? _categories.firstWhere((c) => c.id == categoryId, orElse: () => _categories.first)
          : null;
      final party = partyId != null
          ? _parties.firstWhere((p) => p.id == partyId, orElse: () => _parties.first)
          : null;

      final txnId = 'txn_${_uuid.v4()}';
      final voucher = generateNextVoucherNumber();
      final txnType = account.accountType == AccountType.cash
          ? TransactionType.cashReceived
          : TransactionType.bankReceived;

      final transaction = TransactionModel(
        id: txnId,
        businessId: _currentBusinessId,
        voucherNumber: voucher,
        transactionDate: DateFormatter.formatDate(now),
        transactionTime: DateFormatter.formatTime(now),
        transactionType: txnType,
        accountId: account.id,
        accountName: account.accountName,
        categoryId: category?.id,
        categoryName: category?.name,
        partyId: party?.id,
        partyName: party?.name,
        description: narration,
        paymentMode: paymentMode,
        amountPaise: amountPaise,
        referenceNumber: referenceNumber,
        attachmentUrls: attachments,
        createdBy: createdBy,
        createdByName: createdByName,
        deviceId: deviceId,
        idempotencyKey: 'idem_$txnId',
        createdAt: now,
        updatedAt: now,
      );

      // Save to local database
      await _db.upsertTransaction(transaction);

      // Update account balance
      final newAccBalance = account.currentBalancePaise + amountPaise;
      await _db.updateAccountBalance(account.id, newAccBalance);

      // Update party balance if linked (Receiving money from a party reduces what they owe or increases what we owe)
      if (party != null) {
        final newPartyBalance = party.currentBalancePaise - amountPaise;
        await _db.updatePartyBalance(party.id, newPartyBalance);
      }

      // Enqueue to offline sync queue
      await _db.enqueueSyncItem(SyncQueueModel(
        queueId: 'q_${_uuid.v4()}',
        businessId: _currentBusinessId,
        entityType: 'transaction',
        entityId: transaction.id,
        operationType: 'INSERT',
        payloadJson: jsonEncode(transaction.toFirestore()),
        idempotencyKey: transaction.idempotencyKey,
        createdAt: now,
        updatedAt: now,
      ));

      // Audit Log
      await _db.insertAuditLog(AuditLogModel(
        id: 'aud_${_uuid.v4()}',
        businessId: _currentBusinessId,
        action: 'CREATE_INCOME',
        entityType: 'transaction',
        entityId: transaction.id,
        performedBy: createdBy,
        performedByName: createdByName,
        detailsJson: jsonEncode({
          'voucher': voucher,
          'amountPaise': amountPaise,
          'account': account.accountName,
        }),
        createdAt: now,
      ));

      await loadBusinessData(_currentBusinessId);
      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Records an Expense transaction (Cash Paid or Bank Payment)
  Future<({bool success, String? error})> recordExpense({
    required String accountId,
    required int amountPaise,
    required String paymentMode,
    String? categoryId,
    String? partyId,
    String? narration,
    String? referenceNumber,
    List<String> attachments = const [],
    required String createdBy,
    required String createdByName,
    required String deviceId,
    DateTime? customDate,
  }) async {
    try {
      final now = customDate ?? DateTime.now();
      final account = _accounts.firstWhere((a) => a.id == accountId);

      // Validate balance constraints
      final validation = FinancialEngine.validateBalanceConstraints(
        account: account,
        prospectiveDeductionPaise: amountPaise,
      );
      if (!validation.isValid) {
        return (success: false, error: validation.errorMessage);
      }

      final category = categoryId != null
          ? _categories.firstWhere((c) => c.id == categoryId, orElse: () => _categories.first)
          : null;
      final party = partyId != null
          ? _parties.firstWhere((p) => p.id == partyId, orElse: () => _parties.first)
          : null;

      final txnId = 'txn_${_uuid.v4()}';
      final voucher = generateNextVoucherNumber();
      final txnType = account.accountType == AccountType.cash
          ? TransactionType.cashPaid
          : TransactionType.bankPayment;

      final transaction = TransactionModel(
        id: txnId,
        businessId: _currentBusinessId,
        voucherNumber: voucher,
        transactionDate: DateFormatter.formatDate(now),
        transactionTime: DateFormatter.formatTime(now),
        transactionType: txnType,
        accountId: account.id,
        accountName: account.accountName,
        categoryId: category?.id,
        categoryName: category?.name,
        partyId: party?.id,
        partyName: party?.name,
        description: narration,
        paymentMode: paymentMode,
        amountPaise: amountPaise,
        referenceNumber: referenceNumber,
        attachmentUrls: attachments,
        createdBy: createdBy,
        createdByName: createdByName,
        deviceId: deviceId,
        idempotencyKey: 'idem_$txnId',
        createdAt: now,
        updatedAt: now,
      );

      await _db.upsertTransaction(transaction);

      // Decrease account balance
      final newAccBalance = account.currentBalancePaise - amountPaise;
      await _db.updateAccountBalance(account.id, newAccBalance);

      // Update party balance if linked (Paying money to a party reduces what we owe or increases what they owe)
      if (party != null) {
        final newPartyBalance = party.currentBalancePaise + amountPaise;
        await _db.updatePartyBalance(party.id, newPartyBalance);
      }

      // Enqueue to offline sync queue
      await _db.enqueueSyncItem(SyncQueueModel(
        queueId: 'q_${_uuid.v4()}',
        businessId: _currentBusinessId,
        entityType: 'transaction',
        entityId: transaction.id,
        operationType: 'INSERT',
        payloadJson: jsonEncode(transaction.toFirestore()),
        idempotencyKey: transaction.idempotencyKey,
        createdAt: now,
        updatedAt: now,
      ));

      // Audit Log
      await _db.insertAuditLog(AuditLogModel(
        id: 'aud_${_uuid.v4()}',
        businessId: _currentBusinessId,
        action: 'CREATE_EXPENSE',
        entityType: 'transaction',
        entityId: transaction.id,
        performedBy: createdBy,
        performedByName: createdByName,
        detailsJson: jsonEncode({
          'voucher': voucher,
          'amountPaise': amountPaise,
          'account': account.accountName,
        }),
        createdAt: now,
      ));

      await loadBusinessData(_currentBusinessId);
      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Records an Atomic Transfer between two Accounts
  /// Both legs are recorded atomically with a single shared `transferId`.
  /// Transfers do NOT count towards Income or Expense totals.
  Future<({bool success, String? error})> recordTransfer({
    required String sourceAccountId,
    required String targetAccountId,
    required int amountPaise,
    required String paymentMode,
    String? narration,
    String? referenceNumber,
    required String createdBy,
    required String createdByName,
    required String deviceId,
    DateTime? customDate,
  }) async {
    try {
      if (sourceAccountId == targetAccountId) {
        return (success: false, error: 'Source and Destination accounts cannot be the same.');
      }

      final sourceAcc = _accounts.firstWhere((a) => a.id == sourceAccountId);
      final targetAcc = _accounts.firstWhere((a) => a.id == targetAccountId);

      // Validate source balance
      final validation = FinancialEngine.validateBalanceConstraints(
        account: sourceAcc,
        prospectiveDeductionPaise: amountPaise,
      );
      if (!validation.isValid) {
        return (success: false, error: validation.errorMessage);
      }

      final now = customDate ?? DateTime.now();
      final transferId = 'xfer_${_uuid.v4()}';
      final sourceVoucher = generateNextVoucherNumber();
      final targetVoucher = '$sourceVoucher-B';

      final pair = FinancialEngine.createAtomicTransfer(
        transferId: transferId,
        businessId: _currentBusinessId,
        sourceVoucher: sourceVoucher,
        targetVoucher: targetVoucher,
        transactionDate: DateFormatter.formatDate(now),
        transactionTime: DateFormatter.formatTime(now),
        sourceAccount: sourceAcc,
        targetAccount: targetAcc,
        amountPaise: amountPaise,
        paymentMode: paymentMode,
        referenceNumber: referenceNumber,
        narration: narration,
        createdBy: createdBy,
        createdByName: createdByName,
        deviceId: deviceId,
        idempotencyKeyPrefix: 'idem_$transferId',
      );

      // Save both transactions locally
      await _db.upsertTransaction(pair.transferDebit);
      await _db.upsertTransaction(pair.transferCredit);

      // Update source and target account balances
      await _db.updateAccountBalance(sourceAcc.id, sourceAcc.currentBalancePaise - amountPaise);
      await _db.updateAccountBalance(targetAcc.id, targetAcc.currentBalancePaise + amountPaise);

      // Enqueue both legs into sync queue
      await _db.enqueueSyncItem(SyncQueueModel(
        queueId: 'q_${_uuid.v4()}',
        businessId: _currentBusinessId,
        entityType: 'transaction',
        entityId: pair.transferDebit.id,
        operationType: 'INSERT',
        payloadJson: jsonEncode(pair.transferDebit.toFirestore()),
        idempotencyKey: pair.transferDebit.idempotencyKey,
        createdAt: now,
        updatedAt: now,
      ));

      await _db.enqueueSyncItem(SyncQueueModel(
        queueId: 'q_${_uuid.v4()}',
        businessId: _currentBusinessId,
        entityType: 'transaction',
        entityId: pair.transferCredit.id,
        operationType: 'INSERT',
        payloadJson: jsonEncode(pair.transferCredit.toFirestore()),
        idempotencyKey: pair.transferCredit.idempotencyKey,
        createdAt: now,
        updatedAt: now,
      ));

      // Audit Log
      await _db.insertAuditLog(AuditLogModel(
        id: 'aud_${_uuid.v4()}',
        businessId: _currentBusinessId,
        action: 'CREATE_TRANSFER',
        entityType: 'transfer',
        entityId: transferId,
        performedBy: createdBy,
        performedByName: createdByName,
        detailsJson: jsonEncode({
          'transferId': transferId,
          'amountPaise': amountPaise,
          'from': sourceAcc.accountName,
          'to': targetAcc.accountName,
        }),
        createdAt: now,
      ));

      await loadBusinessData(_currentBusinessId);
      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Reverses an existing posted transaction with reason and audit trail
  Future<({bool success, String? error})> reverseTransaction({
    required TransactionModel originalTransaction,
    required String reason,
    required String reversedBy,
    required String reversedByName,
    required String deviceId,
  }) async {
    try {
      if (originalTransaction.status == TransactionStatus.reversed) {
        return (success: false, error: 'Transaction is already reversed.');
      }

      final now = DateTime.now();
      final reversalVoucher = '${originalTransaction.voucherNumber}-REV';
      final newTxnId = 'txn_rev_${_uuid.v4()}';

      final reversalTxn = FinancialEngine.createReversalTransaction(
        originalTransaction: originalTransaction,
        reversalVoucherNumber: reversalVoucher,
        reversalReason: reason,
        reversedBy: reversedBy,
        reversedByName: reversedByName,
        deviceId: deviceId,
        newTransactionId: newTxnId,
      );

      // Mark original transaction as reversed
      final updatedOriginal = originalTransaction.copyWith(
        status: TransactionStatus.reversed,
        reversalReason: reason,
        reversedTransactionId: newTxnId,
        updatedAt: now,
      );

      await _db.upsertTransaction(updatedOriginal);
      await _db.upsertTransaction(reversalTxn);

      // Restore account balance
      final account = _accounts.firstWhere((a) => a.id == originalTransaction.accountId);
      int restoredBalance = account.currentBalancePaise;

      if (originalTransaction.isIncome) {
        restoredBalance -= originalTransaction.amountPaise;
      } else if (originalTransaction.isExpense) {
        restoredBalance += originalTransaction.amountPaise;
      } else if (originalTransaction.isTransfer) {
        restoredBalance += originalTransaction.amountPaise;
      }
      await _db.updateAccountBalance(account.id, restoredBalance);

      // Restore party balance if linked
      if (originalTransaction.partyId != null) {
        final party = _parties.firstWhere((p) => p.id == originalTransaction.partyId);
        int restoredPartyBalance = party.currentBalancePaise;
        if (originalTransaction.isIncome) {
          restoredPartyBalance += originalTransaction.amountPaise;
        } else if (originalTransaction.isExpense) {
          restoredPartyBalance -= originalTransaction.amountPaise;
        }
        await _db.updatePartyBalance(party.id, restoredPartyBalance);
      }

      // Enqueue reversal to sync queue
      await _db.enqueueSyncItem(SyncQueueModel(
        queueId: 'q_${_uuid.v4()}',
        businessId: _currentBusinessId,
        entityType: 'transaction',
        entityId: reversalTxn.id,
        operationType: 'INSERT',
        payloadJson: jsonEncode(reversalTxn.toFirestore()),
        idempotencyKey: reversalTxn.idempotencyKey,
        createdAt: now,
        updatedAt: now,
      ));

      // Audit Log
      await _db.insertAuditLog(AuditLogModel(
        id: 'aud_${_uuid.v4()}',
        businessId: _currentBusinessId,
        action: 'REVERSE_TRANSACTION',
        entityType: 'transaction',
        entityId: originalTransaction.id,
        performedBy: reversedBy,
        performedByName: reversedByName,
        detailsJson: jsonEncode({
          'originalVoucher': originalTransaction.voucherNumber,
          'reversalVoucher': reversalVoucher,
          'reason': reason,
        }),
        createdAt: now,
      ));

      await loadBusinessData(_currentBusinessId);
      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Permanently deletes a transaction and restores account and party balances
  Future<({bool success, String? error})> deleteTransaction({
    required String transactionId,
    String? performedBy,
    String? performedByName,
  }) async {
    try {
      final txn = _transactions.firstWhere((t) => t.id == transactionId);

      // If transaction was posted (not voided or reversed), restore balances
      if (txn.status == TransactionStatus.posted) {
        // 1. Restore account balance
        final accIdx = _accounts.indexWhere((a) => a.id == txn.accountId);
        if (accIdx != -1) {
          final account = _accounts[accIdx];
          int restored = account.currentBalancePaise;
          if (txn.isIncome) {
            restored -= txn.amountPaise;
          } else if (txn.isExpense) {
            restored += txn.amountPaise;
          } else if (txn.isTransfer) {
            restored += txn.amountPaise;
          }
          await _db.updateAccountBalance(account.id, restored);
        }

        // 2. Restore party balance if linked
        if (txn.partyId != null) {
          final pIdx = _parties.indexWhere((p) => p.id == txn.partyId);
          if (pIdx != -1) {
            final party = _parties[pIdx];
            int restoredParty = party.currentBalancePaise;
            if (txn.isIncome) {
              restoredParty += txn.amountPaise;
            } else if (txn.isExpense) {
              restoredParty -= txn.amountPaise;
            }
            await _db.updatePartyBalance(party.id, restoredParty);
          }
        }
      }

      await _db.deleteTransaction(transactionId);
      await loadBusinessData(_currentBusinessId);
      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Updates an existing transaction and adjusts differences in balances
  Future<({bool success, String? error})> updateTransaction({
    required TransactionModel updatedTransaction,
  }) async {
    try {
      final oldTxn = _transactions.firstWhere((t) => t.id == updatedTransaction.id);

      // Revert old effect
      if (oldTxn.status == TransactionStatus.posted) {
        final accIdx = _accounts.indexWhere((a) => a.id == oldTxn.accountId);
        if (accIdx != -1) {
          final oldAcc = _accounts[accIdx];
          int revertedAcc = oldAcc.currentBalancePaise;
          if (oldTxn.isIncome) revertedAcc -= oldTxn.amountPaise;
          else if (oldTxn.isExpense) revertedAcc += oldTxn.amountPaise;
          await _db.updateAccountBalance(oldAcc.id, revertedAcc);
        }

        if (oldTxn.partyId != null) {
          final pIdx = _parties.indexWhere((p) => p.id == oldTxn.partyId);
          if (pIdx != -1) {
            int revertedParty = _parties[pIdx].currentBalancePaise;
            if (oldTxn.isIncome) revertedParty += oldTxn.amountPaise;
            else if (oldTxn.isExpense) revertedParty -= oldTxn.amountPaise;
            await _db.updatePartyBalance(_parties[pIdx].id, revertedParty);
          }
        }
      }

      // Apply new effect
      final newAccIdx = _accounts.indexWhere((a) => a.id == updatedTransaction.accountId);
      if (newAccIdx != -1) {
        final newAcc = _accounts[newAccIdx];
        int newAccBal = newAcc.currentBalancePaise;
        if (updatedTransaction.isIncome) newAccBal += updatedTransaction.amountPaise;
        else if (updatedTransaction.isExpense) newAccBal -= updatedTransaction.amountPaise;
        await _db.updateAccountBalance(newAcc.id, newAccBal);
      }

      if (updatedTransaction.partyId != null) {
        final pIdx = _parties.indexWhere((p) => p.id == updatedTransaction.partyId);
        if (pIdx != -1) {
          int newPartyBal = _parties[pIdx].currentBalancePaise;
          if (updatedTransaction.isIncome) newPartyBal -= updatedTransaction.amountPaise;
          else if (updatedTransaction.isExpense) newPartyBal += updatedTransaction.amountPaise;
          await _db.updatePartyBalance(_parties[pIdx].id, newPartyBal);
        }
      }

      await _db.upsertTransaction(updatedTransaction);
      await loadBusinessData(_currentBusinessId);
      return (success: true, error: null);
    } catch (e) {
      return (success: false, error: e.toString());
    }
  }

  /// Adds a new Account (Cash or Bank)
  Future<void> addAccount(AccountModel account) async {
    await _db.upsertAccount(account);
    await loadBusinessData(_currentBusinessId);
  }

  /// Adds a new Category
  Future<void> addCategory(CategoryModel category) async {
    await _db.upsertCategory(category);
    await loadBusinessData(_currentBusinessId);
  }

  /// Adds a new Party (Customer, Supplier, Employee) and queues cloud sync
  Future<void> addParty(PartyModel party, {String? createdBy, String? createdByName}) async {
    final now = DateTime.now();
    await _db.upsertParty(party);

    // Enqueue to offline sync queue for cloud persistence
    await _db.enqueueSyncItem(SyncQueueModel(
      queueId: 'q_${_uuid.v4()}',
      businessId: _currentBusinessId,
      entityType: 'party',
      entityId: party.id,
      operationType: 'INSERT',
      payloadJson: jsonEncode(party.toFirestore()),
      idempotencyKey: 'idem_pty_${party.id}',
      createdAt: now,
      updatedAt: now,
    ));

    // Audit Log
    await _db.insertAuditLog(AuditLogModel(
      id: 'aud_${_uuid.v4()}',
      businessId: _currentBusinessId,
      action: 'CREATE_PARTY',
      entityType: 'party',
      entityId: party.id,
      performedBy: createdBy ?? 'usr_current',
      performedByName: createdByName ?? 'Operator',
      detailsJson: jsonEncode({
        'name': party.name,
        'type': party.partyType.name,
        'phone': party.phone,
        'openingBalance': party.openingBalancePaise,
      }),
      createdAt: now,
    ));

    await loadBusinessData(_currentBusinessId);
  }
}

