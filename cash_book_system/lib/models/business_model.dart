class BusinessModel {
  final String id;
  final String name;
  final String ownerId;
  final String ownerName;
  final String? logoUrl;
  final String? address;
  final String? phone;
  final String? email;
  final String? gstin;
  final String? pan;
  final String currencyCode;
  final String currencySymbol;
  final int financialYearStartMonth; // 4 for April (India)
  final String voucherPrefix;
  final int nextVoucherSequence;
  final DateTime createdAt;
  final DateTime updatedAt;

  BusinessModel({
    required this.id,
    required this.name,
    required this.ownerId,
    required this.ownerName,
    this.logoUrl,
    this.address,
    this.phone,
    this.email,
    this.gstin,
    this.pan,
    this.currencyCode = 'INR',
    this.currencySymbol = '₹',
    this.financialYearStartMonth = 4,
    this.voucherPrefix = 'VCH',
    this.nextVoucherSequence = 1,
    required this.createdAt,
    required this.updatedAt,
  });

  BusinessModel copyWith({
    String? id,
    String? name,
    String? ownerId,
    String? ownerName,
    String? logoUrl,
    String? address,
    String? phone,
    String? email,
    String? gstin,
    String? pan,
    String? currencyCode,
    String? currencySymbol,
    int? financialYearStartMonth,
    String? voucherPrefix,
    int? nextVoucherSequence,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return BusinessModel(
      id: id ?? this.id,
      name: name ?? this.name,
      ownerId: ownerId ?? this.ownerId,
      ownerName: ownerName ?? this.ownerName,
      logoUrl: logoUrl ?? this.logoUrl,
      address: address ?? this.address,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      gstin: gstin ?? this.gstin,
      pan: pan ?? this.pan,
      currencyCode: currencyCode ?? this.currencyCode,
      currencySymbol: currencySymbol ?? this.currencySymbol,
      financialYearStartMonth: financialYearStartMonth ?? this.financialYearStartMonth,
      voucherPrefix: voucherPrefix ?? this.voucherPrefix,
      nextVoucherSequence: nextVoucherSequence ?? this.nextVoucherSequence,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'owner_id': ownerId,
      'owner_name': ownerName,
      'logo_url': logoUrl,
      'address': address,
      'phone': phone,
      'email': email,
      'gstin': gstin,
      'pan': pan,
      'currency_code': currencyCode,
      'currency_symbol': currencySymbol,
      'fy_start_month': financialYearStartMonth,
      'voucher_prefix': voucherPrefix,
      'next_voucher_seq': nextVoucherSequence,
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
    };
  }

  factory BusinessModel.fromMap(Map<String, dynamic> map) {
    return BusinessModel(
      id: map['id'] as String,
      name: map['name'] as String,
      ownerId: map['owner_id'] as String,
      ownerName: map['owner_name'] as String,
      logoUrl: map['logo_url'] as String?,
      address: map['address'] as String?,
      phone: map['phone'] as String?,
      email: map['email'] as String?,
      gstin: map['gstin'] as String?,
      pan: map['pan'] as String?,
      currencyCode: map['currency_code'] as String? ?? 'INR',
      currencySymbol: map['currency_symbol'] as String? ?? '₹',
      financialYearStartMonth: map['fy_start_month'] as int? ?? 4,
      voucherPrefix: map['voucher_prefix'] as String? ?? 'VCH',
      nextVoucherSequence: map['next_voucher_seq'] as int? ?? 1,
      createdAt: DateTime.parse(map['created_at'] as String),
      updatedAt: DateTime.parse(map['updated_at'] as String),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'id': id,
      'name': name,
      'ownerId': ownerId,
      'ownerName': ownerName,
      'logoUrl': logoUrl,
      'address': address,
      'phone': phone,
      'email': email,
      'gstin': gstin,
      'pan': pan,
      'currencyCode': currencyCode,
      'currencySymbol': currencySymbol,
      'financialYearStartMonth': financialYearStartMonth,
      'voucherPrefix': voucherPrefix,
      'nextVoucherSequence': nextVoucherSequence,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  factory BusinessModel.fromFirestore(Map<String, dynamic> json) {
    return BusinessModel(
      id: json['id'] as String,
      name: json['name'] as String,
      ownerId: json['ownerId'] as String,
      ownerName: json['ownerName'] as String,
      logoUrl: json['logoUrl'] as String?,
      address: json['address'] as String?,
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      gstin: json['gstin'] as String?,
      pan: json['pan'] as String?,
      currencyCode: json['currencyCode'] as String? ?? 'INR',
      currencySymbol: json['currencySymbol'] as String? ?? '₹',
      financialYearStartMonth: json['financialYearStartMonth'] as int? ?? 4,
      voucherPrefix: json['voucherPrefix'] as String? ?? 'VCH',
      nextVoucherSequence: json['nextVoucherSequence'] as int? ?? 1,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }
}

