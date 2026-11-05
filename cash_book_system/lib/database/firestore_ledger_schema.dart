import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/party_model.dart';
import '../models/transaction_model.dart';

/// Centralized Cloud Firestore Collection Architecture & Type-Safe Ledger Data Model.
///
/// Hierarchy Design:
/// -----------------
/// Primary Multi-Tenant Isolation Path:
///   `businesses/{businessId}/parties/{partyId}`
///   `businesses/{businessId}/transactions/{transactionId}`
///
/// Collection Group Query Support:
///   Allows cross-business or auditor-level indexing:
///   - collectionGroup('parties')
///   - collectionGroup('transactions')
///
/// Precision & Data Integrity Rules:
/// ---------------------------------
/// 1. Zero Floating-Point Math: All currency values are strictly stored as 64-bit integers in Paise (₹1.00 = 100 paise).
/// 2. Idempotency: Every transaction document ID matches its client-generated UUID `id`, and holds a SHA-256 `idempotencyKey`.
/// 3. Immutable Audit Trails: Vouchers are never deleted; correction requires an authorized reversal voucher (`isReversal = true`).
class FirestoreLedgerSchema {
  final FirebaseFirestore _firestore;

  FirestoreLedgerSchema({FirebaseFirestore? firestore})
      : _firestore = firestore ?? FirebaseFirestore.instance;

  // Collection Names
  static const String colBusinesses = 'businesses';
  static const String colParties = 'parties';
  static const String colTransactions = 'transactions';
  static const String colAuditLogs = 'audit_logs';

  // ---------------------------------------------------------------------------
  // 1. PARTIES COLLECTION REFERENCE & CONVERTER
  // ---------------------------------------------------------------------------

  /// Returns a type-safe [CollectionReference<PartyModel>] for a business.
  CollectionReference<PartyModel> partiesRef(String businessId) {
    return _firestore
        .collection(colBusinesses)
        .doc(businessId)
        .collection(colParties)
        .withConverter<PartyModel>(
          fromFirestore: (snapshot, _) => fromPartyFirestoreDoc(snapshot),
          toFirestore: (party, _) => toPartyFirestoreMap(party),
        );
  }

  /// Single Party Document Reference
  DocumentReference<PartyModel> partyDocRef(String businessId, String partyId) {
    return partiesRef(businessId).doc(partyId);
  }

  /// Party Document Parser handling both Firestore [Timestamp] and ISO String dates.
  static PartyModel fromPartyFirestoreDoc(DocumentSnapshot snapshot) {
    final data = snapshot.data() as Map<String, dynamic>? ?? {};
    final id = snapshot.id;

    DateTime parseDate(dynamic val) {
      if (val is Timestamp) return val.toDate();
      if (val is String) return DateTime.tryParse(val) ?? DateTime.now();
      return DateTime.now();
    }

    return PartyModel(
      id: data['id'] as String? ?? id,
      businessId: data['businessId'] as String? ?? '',
      name: data['name'] as String? ?? '',
      partyType: PartyType.values.firstWhere(
        (e) => e.name == data['partyType'],
        orElse: () => PartyType.customer,
      ),
      phone: data['phone'] as String?,
      email: data['email'] as String?,
      address: data['address'] as String?,
      openingBalancePaise: (data['openingBalancePaise'] as num?)?.toInt() ?? 0,
      openingBalanceType: OpeningBalanceType.values.firstWhere(
        (e) => e.name == data['openingBalanceType'],
        orElse: () => OpeningBalanceType.receivable,
      ),
      currentBalancePaise: (data['currentBalancePaise'] as num?)?.toInt() ?? 0,
      gstin: data['gstin'] as String?,
      isActive: data['isActive'] as bool? ?? true,
      createdAt: parseDate(data['createdAt']),
      updatedAt: parseDate(data['updatedAt']),
      syncStatus: SyncStatus.synced,
    );
  }

  /// Converts a [PartyModel] into a standardized Firestore document Map.
  static Map<String, dynamic> toPartyFirestoreMap(PartyModel party) {
    return {
      'id': party.id,
      'businessId': party.businessId,
      'name': party.name,
      'partyType': party.partyType.name,
      'phone': party.phone,
      'email': party.email,
      'address': party.address,
      'openingBalancePaise': party.openingBalancePaise,
      'openingBalanceType': party.openingBalanceType.name,
      'currentBalancePaise': party.currentBalancePaise,
      'gstin': party.gstin,
      'isActive': party.isActive,
      'createdAt': Timestamp.fromDate(party.createdAt),
      'updatedAt': FieldValue.serverTimestamp(),
      'searchKeywords': _generateSearchKeywords(party.name, party.phone),
    };
  }

  // ---------------------------------------------------------------------------
  // 2. TRANSACTIONS COLLECTION REFERENCE & CONVERTER
  // ---------------------------------------------------------------------------

  /// Returns a type-safe [CollectionReference<TransactionModel>] for a business.
  CollectionReference<TransactionModel> transactionsRef(String businessId) {
    return _firestore
        .collection(colBusinesses)
        .doc(businessId)
        .collection(colTransactions)
        .withConverter<TransactionModel>(
          fromFirestore: (snapshot, _) => fromTransactionFirestoreDoc(snapshot),
          toFirestore: (txn, _) => toTransactionFirestoreMap(txn),
        );
  }

  /// Single Transaction Document Reference
  DocumentReference<TransactionModel> transactionDocRef(
      String businessId, String transactionId) {
    return transactionsRef(businessId).doc(transactionId);
  }

  /// Transaction Document Parser with timestamp reconciliation and null safety.
  static TransactionModel fromTransactionFirestoreDoc(
      DocumentSnapshot snapshot) {
    final data = snapshot.data() as Map<String, dynamic>? ?? {};
    final id = snapshot.id;

    DateTime parseDate(dynamic val) {
      if (val is Timestamp) return val.toDate();
      if (val is String) return DateTime.tryParse(val) ?? DateTime.now();
      return DateTime.now();
    }

    final rawAttachments = data['attachmentUrls'];
    List<String> attachments = [];
    if (rawAttachments is List) {
      attachments = rawAttachments.map((e) => e.toString()).toList();
    }

    return TransactionModel(
      id: data['id'] as String? ?? id,
      businessId: data['businessId'] as String? ?? '',
      voucherNumber: data['voucherNumber'] as String? ?? 'VCH-0000',
      transactionDate: data['transactionDate'] as String? ?? '',
      transactionTime: data['transactionTime'] as String? ?? '',
      transactionType: TransactionType.values.firstWhere(
        (e) => e.name == data['transactionType'],
        orElse: () => TransactionType.cashReceived,
      ),
      accountId: data['accountId'] as String? ?? '',
      accountName: data['accountName'] as String?,
      targetAccountId: data['targetAccountId'] as String?,
      targetAccountName: data['targetAccountName'] as String?,
      transferId: data['transferId'] as String?,
      categoryId: data['categoryId'] as String?,
      categoryName: data['categoryName'] as String?,
      partyId: data['partyId'] as String?,
      partyName: data['partyName'] as String?,
      description: data['description'] as String?,
      paymentMode: data['paymentMode'] as String? ?? 'Cash',
      amountPaise: (data['amountPaise'] as num?)?.toInt() ?? 0,
      referenceNumber: data['referenceNumber'] as String?,
      attachmentUrls: attachments,
      status: TransactionStatus.values.firstWhere(
        (e) => e.name == data['status'],
        orElse: () => TransactionStatus.posted,
      ),
      reversalReason: data['reversalReason'] as String?,
      reversedTransactionId: data['reversedTransactionId'] as String?,
      originalTransactionId: data['originalTransactionId'] as String?,
      createdBy: data['createdBy'] as String? ?? '',
      createdByName: data['createdByName'] as String? ?? 'User',
      deviceId: data['deviceId'] as String? ?? 'device-0',
      idempotencyKey: data['idempotencyKey'] as String? ?? id,
      createdAt: parseDate(data['createdAt']),
      updatedAt: parseDate(data['updatedAt']),
      syncStatus: SyncStatus.synced,
    );
  }

  /// Converts a [TransactionModel] into a standardized Firestore document Map.
  static Map<String, dynamic> toTransactionFirestoreMap(
      TransactionModel txn) {
    return {
      'id': txn.id,
      'businessId': txn.businessId,
      'voucherNumber': txn.voucherNumber,
      'transactionDate': txn.transactionDate,
      'transactionTime': txn.transactionTime,
      'transactionType': txn.transactionType.name,
      'accountId': txn.accountId,
      'accountName': txn.accountName,
      'targetAccountId': txn.targetAccountId,
      'targetAccountName': txn.targetAccountName,
      'transferId': txn.transferId,
      'categoryId': txn.categoryId,
      'categoryName': txn.categoryName,
      'partyId': txn.partyId,
      'partyName': txn.partyName,
      'description': txn.description,
      'paymentMode': txn.paymentMode,
      'amountPaise': txn.amountPaise,
      'referenceNumber': txn.referenceNumber,
      'attachmentUrls': txn.attachmentUrls,
      'status': txn.status.name,
      'reversalReason': txn.reversalReason,
      'reversedTransactionId': txn.reversedTransactionId,
      'originalTransactionId': txn.originalTransactionId,
      'createdBy': txn.createdBy,
      'createdByName': txn.createdByName,
      'deviceId': txn.deviceId,
      'idempotencyKey': txn.idempotencyKey,
      'createdAt': Timestamp.fromDate(txn.createdAt),
      'updatedAt': FieldValue.serverTimestamp(),
    };
  }

  // ---------------------------------------------------------------------------
  // 3. PERSISTENCE & LEDGER QUERY APIS
  // ---------------------------------------------------------------------------

  /// Post a transaction with atomic party ledger balance adjustment.
  Future<void> postTransactionAtomic({
    required String businessId,
    required TransactionModel transaction,
    PartyModel? party,
  }) async {
    final batch = _firestore.batch();

    // 1. Transaction doc
    final txnDoc = transactionDocRef(businessId, transaction.id);
    batch.set(txnDoc, transaction);

    // 2. Atomic Party balance update if associated with a party
    if (party != null && transaction.partyId != null) {
      final partyDoc = partyDocRef(businessId, transaction.partyId!);
      int balanceDelta = 0;
      if (transaction.transactionType == TransactionType.cashReceived ||
          transaction.transactionType == TransactionType.bankReceived) {
        // Receiving money from party reduces receivable / increases payable
        balanceDelta = -transaction.amountPaise;
      } else if (transaction.transactionType == TransactionType.cashPaid ||
          transaction.transactionType == TransactionType.bankPayment) {
        // Paying money to party increases receivable / reduces payable
        balanceDelta = transaction.amountPaise;
      }

      batch.update(partyDoc, {
        'currentBalancePaise': FieldValue.increment(balanceDelta),
        'updatedAt': FieldValue.serverTimestamp(),
      });
    }

    await batch.commit();
  }

  /// Query ledger entries filtered by Date Range
  Query<TransactionModel> queryLedgerByDateRange(
    String businessId, {
    required DateTime startDate,
    required DateTime endDate,
  }) {
    return transactionsRef(businessId)
        .where('createdAt', isGreaterThanOrEqualTo: Timestamp.fromDate(startDate))
        .where('createdAt', isLessThanOrEqualTo: Timestamp.fromDate(endDate))
        .orderBy('createdAt', descending: true);
  }

  /// Query all transactions for a specific Party Ledger
  Query<TransactionModel> queryPartyLedger(
    String businessId,
    String partyId,
  ) {
    return transactionsRef(businessId)
        .where('partyId', isEqualTo: partyId)
        .orderBy('createdAt', descending: true);
  }

  /// Search keyword generator for prefix search on Firestore
  static List<String> _generateSearchKeywords(String name, String? phone) {
    final Set<String> keywords = {};
    final lowerName = name.toLowerCase();
    for (int i = 1; i <= lowerName.length; i++) {
      keywords.add(lowerName.substring(0, i));
    }
    if (phone != null && phone.isNotEmpty) {
      for (int i = 1; i <= phone.length; i++) {
        keywords.add(phone.substring(0, i));
      }
    }
    return keywords.toList();
  }
}

