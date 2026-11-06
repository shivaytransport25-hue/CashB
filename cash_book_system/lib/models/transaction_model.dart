import '../core/constants/enums.dart';

class TransactionModel {
  final String id;
  final String businessId;
  final String voucherNumber;
  final String transactionDate; // DD-MM-YYYY
  final String transactionTime; // hh:mm a
  final TransactionType transactionType;
  final String accountId;
  final String? accountName;
  final String? targetAccountId; // Populated for transfers
  final String? targetAccountName;
  final String? transferId; // Shared ID linking both legs of a transfer
  final String? categoryId;
  final String? categoryName;
  final String? partyId;
  final String? partyName;
  final String? description;
  final String paymentMode; // Cash, Cheque, NEFT/RTGS, UPI, Card
  final int amountPaise;
  final String? referenceNumber;
  final List<String> attachmentUrls;
  final TransactionStatus status;
  final String? reversalReason;
  final String? reversedTransactionId;
  final String? originalTransactionId;
  final String createdBy;
  final String createdByName;
  final String deviceId;
  final String idempotencyKey;
  final DateTime createdAt;
  final DateTime updatedAt;
  final SyncStatus syncStatus;

  TransactionModel({
    required this.id,
    required this.businessId,
    required this.voucherNumber,
    required this.transactionDate,
    required this.transactionTime,
    required this.transactionType,
    required this.accountId,
    this.accountName,
    this.targetAccountId,
    this.targetAccountName,
    this.transferId,
    this.categoryId,
    this.categoryName,
    this.partyId,
    this.partyName,
    this.description,
    required this.paymentMode,
    required this.amountPaise,
    this.referenceNumber,
    this.attachmentUrls = const [],
    this.status = TransactionStatus.posted,
    this.reversalReason,
    this.reversedTransactionId,
    this.originalTransactionId,
    required this.createdBy,
    required this.createdByName,
    required this.deviceId,
    required this.idempotencyKey,
    required this.createdAt,
    required this.updatedAt,
    this.syncStatus = SyncStatus.synced,
  });

  bool get isIncome => transactionType.isIncome;
  bool get isExpense => transactionType.isExpense;
  bool get isTransfer => transactionType.isTransfer;
  bool get isReversed => status == TransactionStatus.reversed;
  bool get isPosted => status == TransactionStatus.posted;

  TransactionModel copyWith({
    String? id,
    String? businessId,
    String? voucherNumber,
    String? transactionDate,
    String? transactionTime,
    TransactionType? transactionType,
    String? accountId,
    String? accountName,
    String? targetAccountId,
    String? targetAccountName,
    String? transferId,
    String? categoryId,
    String? categoryName,
    String? partyId,
    String? partyName,
    String? description,
    String? paymentMode,
    int? amountPaise,
    String? referenceNumber,
    List<String>? attachmentUrls,
    TransactionStatus? status,
    String? reversalReason,
    String? reversedTransactionId,
    String? originalTransactionId,
    String? createdBy,
    String? createdByName,
    String? deviceId,
    String? idempotencyKey,
    DateTime? createdAt,
    DateTime? updatedAt,
    SyncStatus? syncStatus,
  }) {
    return TransactionModel(
      id: id ?? this.id,
      businessId: businessId ?? this.businessId,
      voucherNumber: voucherNumber ?? this.voucherNumber,
      transactionDate: transactionDate ?? this.transactionDate,
      transactionTime: transactionTime ?? this.transactionTime,
      transactionType: transactionType ?? this.transactionType,
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
      targetAccountId: targetAccountId ?? this.targetAccountId,
      targetAccountName: targetAccountName ?? this.targetAccountName,
      transferId: transferId ?? this.transferId,
      categoryId: categoryId ?? this.categoryId,
      categoryName: categoryName ?? this.categoryName,
      partyId: partyId ?? this.partyId,
      partyName: partyName ?? this.partyName,
      description: description ?? this.description,
      paymentMode: paymentMode ?? this.paymentMode,
      amountPaise: amountPaise ?? this.amountPaise,
      referenceNumber: referenceNumber ?? this.referenceNumber,
      attachmentUrls: attachmentUrls ?? this.attachmentUrls,
      status: status ?? this.status,
      reversalReason: reversalReason ?? this.reversalReason,
      reversedTransactionId: reversedTransactionId ?? this.reversedTransactionId,
      originalTransactionId: originalTransactionId ?? this.originalTransactionId,
      createdBy: createdBy ?? this.createdBy,
      createdByName: createdByName ?? this.createdByName,
      deviceId: deviceId ?? this.deviceId,
      idempotencyKey: idempotencyKey ?? this.idempotencyKey,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      syncStatus: syncStatus ?? this.syncStatus,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'business_id': businessId,
      'voucher_number': voucherNumber,
      'transaction_date': transactionDate,
      'transaction_time': transactionTime,
      'transaction_type': transactionType.name,
      'account_id': accountId,
      'target_account_id': targetAccountId,
      'transfer_id': transferId,
      'category_id': categoryId,
      'party_id': partyId,
      'party_name': partyName,
      'description': description,
      'payment_mode': paymentMode,
      'amount_paise': amountPaise,
      'reference_number': referenceNumber,
      'attachment_urls': attachmentUrls.join(';'),
      'status': status.name,
      'reversal_reason': reversalReason,
      'reversed_transaction_id': reversedTransactionId,
      'original_transaction_id': originalTransactionId,
      'created_by': createdBy,
      'created_by_name': createdByName,
      'device_id': deviceId,
      'idempotency_key': idempotencyKey,
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
      'sync_status': syncStatus.name,
    };
  }

  factory TransactionModel.fromMap(Map<String, dynamic> map) {
    final rawAttachments = map['attachment_urls'] as String?;
    final List<String> attachments = (rawAttachments != null && rawAttachments.isNotEmpty)
        ? rawAttachments.split(';')
        : [];

    return TransactionModel(
      id: map['id'] as String,
      businessId: map['business_id'] as String,
      voucherNumber: map['voucher_number'] as String,
      transactionDate: map['transaction_date'] as String,
      transactionTime: map['transaction_time'] as String,
      transactionType: TransactionType.values.firstWhere(
        (e) => e.name == map['transaction_type'],
        orElse: () => TransactionType.cashReceived,
      ),
      accountId: map['account_id'] as String,
      targetAccountId: map['target_account_id'] as String?,
      transferId: map['transfer_id'] as String?,
      categoryId: map['category_id'] as String?,
      partyId: map['party_id'] as String?,
      partyName: map['party_name'] as String?,
      description: map['description'] as String?,
      paymentMode: map['payment_mode'] as String,
      amountPaise: map['amount_paise'] as int,
      referenceNumber: map['reference_number'] as String?,
      attachmentUrls: attachments,
      status: TransactionStatus.values.firstWhere(
        (e) => e.name == map['status'],
        orElse: () => TransactionStatus.posted,
      ),
      reversalReason: map['reversal_reason'] as String?,
      reversedTransactionId: map['reversed_transaction_id'] as String?,
      originalTransactionId: map['original_transaction_id'] as String?,
      createdBy: map['created_by'] as String,
      createdByName: map['created_by_name'] as String,
      deviceId: map['device_id'] as String,
      idempotencyKey: map['idempotency_key'] as String,
      createdAt: DateTime.parse(map['created_at'] as String),
      updatedAt: DateTime.parse(map['updated_at'] as String),
      syncStatus: SyncStatus.values.firstWhere(
        (e) => e.name == map['sync_status'],
        orElse: () => SyncStatus.synced,
      ),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'id': id,
      'businessId': businessId,
      'userId': businessId,
      'bookId': 'main_book',
      'voucherNumber': voucherNumber,
      'transactionDate': transactionDate,
      'transactionTime': transactionTime,
      'transactionType': transactionType.name,
      'type': transactionType.isExpense ? 'CASH_OUT' : (transactionType.isTransfer ? 'TRANSFER' : 'CASH_IN'),
      'accountId': accountId,
      'targetAccountId': targetAccountId,
      'transferId': transferId,
      'categoryId': categoryId,
      'category': categoryName ?? (transactionType.isExpense ? 'Fuel & Diesel' : 'Freight Revenue'),
      'partyId': partyId,
      'partyName': partyName ?? '',
      'description': description,
      'remark': description ?? '',
      'paymentMode': paymentMode,
      'amountPaise': amountPaise,
      'amount': amountPaise / 100.0,
      'referenceNumber': referenceNumber,
      'attachmentUrls': attachmentUrls,
      'status': status.name,
      'reversalReason': reversalReason,
      'reversedTransactionId': reversedTransactionId,
      'originalTransactionId': originalTransactionId,
      'createdBy': createdBy,
      'createdByName': createdByName,
      'deviceId': deviceId,
      'idempotencyKey': idempotencyKey,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  factory TransactionModel.fromFirestore(Map<String, dynamic> json) {
    final rawAmount = json['amountPaise'] ?? (json['amount'] != null ? ((json['amount'] as num) * 100).toInt() : 0);
    final rawType = json['transactionType'] ?? (json['type'] == 'CASH_OUT' ? 'cashPaid' : (json['type'] == 'TRANSFER' ? 'transfer' : 'cashReceived'));
    final rawDesc = json['description'] ?? json['remark'];
    final idStr = (json['id'] ?? '').toString();
    final rawVch = json['voucherNumber'] ?? (idStr.isNotEmpty ? 'VCH-' + (idStr.length >= 6 ? idStr.substring(0, 6).toUpperCase() : idStr.toUpperCase()) : 'VCH-AUTO');

    DateTime parseDate(dynamic d) {
      if (d == null) return DateTime.now();
      if (d is DateTime) return d;
      if (d is String) {
        try { return DateTime.parse(d); } catch (_) { return DateTime.now(); }
      }
      return DateTime.now();
    }

    return TransactionModel(
      id: (json['id'] ?? json['txId'] ?? '').toString(),
      businessId: (json['businessId'] ?? json['userId'] ?? '').toString(),
      voucherNumber: rawVch.toString(),
      transactionDate: (json['transactionDate'] ?? '').toString(),
      transactionTime: (json['transactionTime'] ?? '12:00 PM').toString(),
      transactionType: TransactionType.values.firstWhere(
        (e) => e.name == rawType,
        orElse: () => TransactionType.cashReceived,
      ),
      accountId: (json['accountId'] ?? 'default_acc').toString(),
      targetAccountId: json['targetAccountId'] as String?,
      transferId: json['transferId'] as String?,
      categoryId: json['categoryId'] as String?,
      partyId: json['partyId'] as String?,
      partyName: json['partyName'] as String?,
      description: rawDesc?.toString(),
      paymentMode: (json['paymentMode'] ?? 'Cash').toString(),
      amountPaise: (rawAmount is num) ? rawAmount.toInt() : 0,
      referenceNumber: json['referenceNumber'] as String?,
      attachmentUrls: (json['attachmentUrls'] as List<dynamic>?)?.map((e) => e.toString()).toList() ?? [],
      status: TransactionStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => TransactionStatus.posted,
      ),
      reversalReason: json['reversalReason'] as String?,
      reversedTransactionId: json['reversedTransactionId'] as String?,
      originalTransactionId: json['originalTransactionId'] as String?,
      createdBy: (json['createdBy'] ?? 'user').toString(),
      createdByName: (json['createdByName'] ?? 'User').toString(),
      deviceId: (json['deviceId'] ?? 'device').toString(),
      idempotencyKey: (json['idempotencyKey'] ?? '').toString(),
      createdAt: parseDate(json['createdAt']),
      updatedAt: parseDate(json['updatedAt']),
      syncStatus: SyncStatus.synced,
    );
  }
}

