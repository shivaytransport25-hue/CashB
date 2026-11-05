import '../core/constants/enums.dart';

class CategoryModel {
  final String id;
  final String businessId;
  final String name;
  final String type; // 'income' | 'expense'
  final String? parentCategoryId;
  final String? iconName;
  final String? colorHex;
  final bool isActive;
  final DateTime createdAt;
  final DateTime updatedAt;
  final SyncStatus syncStatus;

  CategoryModel({
    required this.id,
    required this.businessId,
    required this.name,
    required this.type,
    this.parentCategoryId,
    this.iconName,
    this.colorHex,
    this.isActive = true,
    required this.createdAt,
    required this.updatedAt,
    this.syncStatus = SyncStatus.synced,
  });

  bool get isIncome => type == 'income';
  bool get isExpense => type == 'expense';

  CategoryModel copyWith({
    String? id,
    String? businessId,
    String? name,
    String? type,
    String? parentCategoryId,
    String? iconName,
    String? colorHex,
    bool? isActive,
    DateTime? createdAt,
    DateTime? updatedAt,
    SyncStatus? syncStatus,
  }) {
    return CategoryModel(
      id: id ?? this.id,
      businessId: businessId ?? this.businessId,
      name: name ?? this.name,
      type: type ?? this.type,
      parentCategoryId: parentCategoryId ?? this.parentCategoryId,
      iconName: iconName ?? this.iconName,
      colorHex: colorHex ?? this.colorHex,
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
      'type': type,
      'parent_category_id': parentCategoryId,
      'icon_name': iconName,
      'color_hex': colorHex,
      'is_active': isActive ? 1 : 0,
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
      'sync_status': syncStatus.name,
    };
  }

  factory CategoryModel.fromMap(Map<String, dynamic> map) {
    return CategoryModel(
      id: map['id'] as String,
      businessId: map['business_id'] as String,
      name: map['name'] as String,
      type: map['type'] as String,
      parentCategoryId: map['parent_category_id'] as String?,
      iconName: map['icon_name'] as String?,
      colorHex: map['color_hex'] as String?,
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
      'type': type,
      'parentCategoryId': parentCategoryId,
      'iconName': iconName,
      'colorHex': colorHex,
      'isActive': isActive,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  factory CategoryModel.fromFirestore(Map<String, dynamic> json) {
    return CategoryModel(
      id: json['id'] as String,
      businessId: json['businessId'] as String,
      name: json['name'] as String,
      type: json['type'] as String,
      parentCategoryId: json['parentCategoryId'] as String?,
      iconName: json['iconName'] as String?,
      colorHex: json['colorHex'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      syncStatus: SyncStatus.synced,
    );
  }
}

