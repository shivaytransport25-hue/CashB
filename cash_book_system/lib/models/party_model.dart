import '../core/constants/enums.dart';

class PartyModel {
  final String id;
  final String businessId;
  final String name;
  final PartyType partyType;
  final String? phone;
  final String? email;
  final String? address;
  final int openingBalancePaise;
  final OpeningBalanceType openingBalanceType;
  final int currentBalancePaise; // Positive = Receivable from party, Negative = Payable to party
  final String? gstin;
  final bool isActive;
  final DateTime createdAt;
  final DateTime updatedAt;
  final SyncStatus syncStatus;

  PartyModel({
    required this.id,
    required this.businessId,
    required this.name,
    required this.partyType,
    this.phone,
    this.email,
    this.address,
    this.openingBalancePaise = 0,
    this.openingBalanceType = OpeningBalanceType.receivable,
    this.currentBalancePaise = 0,
    this.gstin,
    this.isActive = true,
    required this.createdAt,
    required this.updatedAt,
    this.syncStatus = SyncStatus.synced,
  });

  PartyModel copyWith({
    String? id,
    String? businessId,
    String? name,
    PartyType? partyType,
    String? phone,
    String? email,
    String? address,
    int? openingBalancePaise,
    OpeningBalanceType? openingBalanceType,
    int? currentBalancePaise,
    String? gstin,
    bool? isActive,
    DateTime? createdAt,
    DateTime? updatedAt,
    SyncStatus? syncStatus,
  }) {
    return PartyModel(
      id: id ?? this.id,
      businessId: businessId ?? this.businessId,
      name: name ?? this.name,
      partyType: partyType ?? this.partyType,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      address: address ?? this.address,
      openingBalancePaise: openingBalancePaise ?? this.openingBalancePaise,
      openingBalanceType: openingBalanceType ?? this.openingBalanceType,
      currentBalancePaise: currentBalancePaise ?? this.currentBalancePaise,
      gstin: gstin ?? this.gstin,
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
      'name': name,
      'party_type': partyType.name,
      'phone': phone,
      'email': email,
      'address': address,
      'opening_balance_paise': openingBalancePaise,
      'opening_balance_type': openingBalanceType.name,
      'current_balance_paise': currentBalancePaise,
      'gstin': gstin,
      'is_active': isActive ? 1 : 0,
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
      'sync_status': syncStatus.name,
    };
  }

  factory PartyModel.fromMap(Map<String, dynamic> map) {
    return PartyModel(
      id: map['id'] as String,
      businessId: map['business_id'] as String,
      name: map['name'] as String,
      partyType: PartyType.values.firstWhere(
        (e) => e.name == map['party_type'],
        orElse: () => PartyType.customer,
      ),
      phone: map['phone'] as String?,
      email: map['email'] as String?,
      address: map['address'] as String?,
      openingBalancePaise: map['opening_balance_paise'] as int? ?? 0,
      openingBalanceType: OpeningBalanceType.values.firstWhere(
        (e) => e.name == map['opening_balance_type'],
        orElse: () => OpeningBalanceType.receivable,
      ),
      currentBalancePaise: map['current_balance_paise'] as int? ?? 0,
      gstin: map['gstin'] as String?,
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
      'name': name,
      'partyType': partyType.name,
      'phone': phone,
      'email': email,
      'address': address,
      'openingBalancePaise': openingBalancePaise,
      'openingBalanceType': openingBalanceType.name,
      'currentBalancePaise': currentBalancePaise,
      'gstin': gstin,
      'isActive': isActive,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  factory PartyModel.fromFirestore(Map<String, dynamic> json) {
    return PartyModel(
      id: json['id'] as String,
      businessId: json['businessId'] as String,
      name: json['name'] as String,
      partyType: PartyType.values.firstWhere(
        (e) => e.name == json['partyType'],
        orElse: () => PartyType.customer,
      ),
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      address: json['address'] as String?,
      openingBalancePaise: json['openingBalancePaise'] as int? ?? 0,
      openingBalanceType: OpeningBalanceType.values.firstWhere(
        (e) => e.name == json['openingBalanceType'],
        orElse: () => OpeningBalanceType.receivable,
      ),
      currentBalancePaise: json['currentBalancePaise'] as int? ?? 0,
      gstin: json['gstin'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      syncStatus: SyncStatus.synced,
    );
  }
}

