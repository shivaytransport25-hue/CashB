import '../core/constants/enums.dart';

class UserModel {
  final String id;
  final String email;
  final String displayName;
  final String? phoneNumber;
  final String? photoUrl;
  final String defaultBusinessId;
  final UserRole role;
  final List<String> permissions;
  final DateTime createdAt;
  final DateTime lastLoginAt;

  UserModel({
    required this.id,
    required this.email,
    required this.displayName,
    this.phoneNumber,
    this.photoUrl,
    required this.defaultBusinessId,
    this.role = UserRole.owner,
    this.permissions = const [],
    required this.createdAt,
    required this.lastLoginAt,
  });

  UserModel copyWith({
    String? id,
    String? email,
    String? displayName,
    String? phoneNumber,
    String? photoUrl,
    String? defaultBusinessId,
    UserRole? role,
    List<String>? permissions,
    DateTime? createdAt,
    DateTime? lastLoginAt,
  }) {
    return UserModel(
      id: id ?? this.id,
      email: email ?? this.email,
      displayName: displayName ?? this.displayName,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      photoUrl: photoUrl ?? this.photoUrl,
      defaultBusinessId: defaultBusinessId ?? this.defaultBusinessId,
      role: role ?? this.role,
      permissions: permissions ?? this.permissions,
      createdAt: createdAt ?? this.createdAt,
      lastLoginAt: lastLoginAt ?? this.lastLoginAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'email': email,
      'display_name': displayName,
      'phone_number': phoneNumber,
      'photo_url': photoUrl,
      'default_business_id': defaultBusinessId,
      'role': role.name,
      'permissions': permissions.join(','),
      'created_at': createdAt.toIso8601String(),
      'last_login_at': lastLoginAt.toIso8601String(),
    };
  }

  factory UserModel.fromMap(Map<String, dynamic> map) {
    final rawPerms = map['permissions'] as String?;
    final List<String> perms = (rawPerms != null && rawPerms.isNotEmpty)
        ? rawPerms.split(',')
        : [];

    return UserModel(
      id: map['id'] as String,
      email: map['email'] as String,
      displayName: map['display_name'] as String,
      phoneNumber: map['phone_number'] as String?,
      photoUrl: map['photo_url'] as String?,
      defaultBusinessId: map['default_business_id'] as String,
      role: UserRole.values.firstWhere(
        (e) => e.name == map['role'],
        orElse: () => UserRole.owner,
      ),
      permissions: perms,
      createdAt: DateTime.parse(map['created_at'] as String),
      lastLoginAt: DateTime.parse(map['last_login_at'] as String),
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'id': id,
      'email': email,
      'displayName': displayName,
      'phoneNumber': phoneNumber,
      'photoUrl': photoUrl,
      'defaultBusinessId': defaultBusinessId,
      'role': role.name,
      'permissions': permissions,
      'createdAt': createdAt.toIso8601String(),
      'lastLoginAt': lastLoginAt.toIso8601String(),
    };
  }

  factory UserModel.fromFirestore(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      email: json['email'] as String,
      displayName: json['displayName'] as String,
      phoneNumber: json['phoneNumber'] as String?,
      photoUrl: json['photoUrl'] as String?,
      defaultBusinessId: json['defaultBusinessId'] as String,
      role: UserRole.values.firstWhere(
        (e) => e.name == json['role'],
        orElse: () => UserRole.owner,
      ),
      permissions: (json['permissions'] as List<dynamic>?)?.map((e) => e.toString()).toList() ?? [],
      createdAt: DateTime.parse(json['createdAt'] as String),
      lastLoginAt: DateTime.parse(json['lastLoginAt'] as String),
    );
  }
}

