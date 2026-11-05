import '../core/constants/enums.dart';

class AccountModel {
  final String id;
  final String businessId;
  final String accountName;
  final AccountType accountType;
  final int openingBalancePaise;
  final String openingDate; // DD-MM-YYYY
  final int currentBalancePaise;
  final String? bankName;
  final String? accountNumber;
  final String? ifscCode;
  final String? branch;
  final bool allowNegativeBalance;
  final bool isActive;
  final DateTime createdAt;
  final DateTime updatedAt;
  final SyncStatus syncStatus;

  AccountModel({
    required this.id,
    required this.businessId,
    required this.accountName,
    required this.accountType,
    required this.openingBalancePaise,
    required this.openingDate,
    required this.currentBalancePaise,
    this.bankName,
    this.accountNumber,
    this.ifscCode,
    this.branch,
    this.allowNegativeBalance = false,
    this.isActive = true,
    required this.createdAt,
    required this.updatedAt,
    this.syncStatus = SyncStatus.synced,
  });

  AccountModel copyWith({
    String? id,
    String? businessId,
    String? accountName,
    AccountType? accountType,
    int? openingBalancePaise,
    String? openingDate,
    int? currentBalancePaise,
    String? bankName,
    String? accountNumber,
    String? ifscCode,
    String? branch,
    bool? allowNegativeBalance,
    bool? isActive,
    DateTime? createdAt,
    DateTime? updatedAt,
    SyncStatus? syncStatus,
  }) {
    return AccountModel(
      id: id ?? this.id,
      businessId: businessId ?? this.businessId,
      accountName: accountName ?? this.accountName,
      accountType: accountType ?? this.accountType,
      openingBalancePaise: openingBalancePaise ?? this.openingBalancePaise,
      openingDate: openingDate ?? this.openingDate,
      currentBalancePaise: currentBalancePaise ?? this.currentBalancePaise,
      bankName: bankName ?? this.bankName,
      accountNumber: accountNumber ?? this.accountNumber,
      ifscCode: ifscCode ?? this.ifscCode,
      branch: branch ?? this.branch,
      allowNegativeBalance: allowNegativeBalance ?? this.allowNegativeBalance,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      syncStatus: syncStatus ?? this.syncStatus,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'business_id': businessId,
      'account_name': accountName,
      'account_type': accountType.name,
      'opening_balance_paise': openingBalancePaise,
      'opening_date': openingDate,
      'current_balance_paise': currentBalancePaise,
      'bank_name': bankName,
      'account_number': accountNumber,
      'ifsc_code': ifscCode,
      'branch': branch,
      'allow_negative_balance': allowNegativeBalance ? 1 : 0,
      'is_active': isActive ? 1 : 0,
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
      'sync_status': syncStatus.name,
    };
  }

  factory AccountModel.fromMap(Map<String, dynamic> map) {
    return AccountModel(
      id: map['id'] as String,
      businessId: map['business_id'] as String,
      accountName: map['account_name'] as String,
      accountType: AccountType.values.firstWhere(
        (e) => e.name == map['account_type'],
        orElse: () => AccountType.cash,
      ),
      openingBalancePaise: map['opening_balance_paise'] as int,
      openingDate: map['opening_date'] as String,
      currentBalancePaise: map['current_balance_paise'] as int,
      bankName: map['bank_name'] as String?,
      accountNumber: map['account_number'] as String?,
      ifscCode: map['ifsc_code'] as String?,
      branch: map['branch'] as String?,
      allowNegativeBalance: (map['allow_negative_balance'] as int? ?? 0) == 1,
      isActive: (map['is_active'] as int? ?? 1) == 1,
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
      'accountName': accountName,
      'accountType': accountType.name,
      'openingBalancePaise': openingBalancePaise,
      'openingDate': openingDate,
      'currentBalancePaise': currentBalancePaise,
      'bankName': bankName,
      'accountNumber': accountNumber,
      'ifscCode': ifscCode,
      'branch': branch,
      'allowNegativeBalance': allowNegativeBalance,
      'isActive': isActive,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  factory AccountModel.fromFirestore(Map<String, dynamic> json) {
    return AccountModel(
      id: json['id'] as String,
      businessId: json['businessId'] as String,
      accountName: json['accountName'] as String,
      accountType: AccountType.values.firstWhere(
        (e) => e.name == json['accountType'],
        orElse: () => AccountType.cash,
      ),
      openingBalancePaise: json['openingBalancePaise'] as int,
      openingDate: json['openingDate'] as String,
      currentBalancePaise: json['currentBalancePaise'] as int,
      bankName: json['bankName'] as String?,
      accountNumber: json['accountNumber'] as String?,
      ifscCode: json['ifscCode'] as String?,
      branch: json['branch'] as String?,
      allowNegativeBalance: json['allowNegativeBalance'] as bool? ?? false,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      syncStatus: SyncStatus.synced,
    );
  }
}

