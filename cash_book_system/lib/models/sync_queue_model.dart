class SyncQueueModel {
  final String queueId;
  final String businessId;
  final String entityType; // 'transaction' | 'account' | 'party' | 'category'
  final String entityId;
  final String operationType; // 'INSERT' | 'UPDATE' | 'DELETE'
  final String payloadJson;
  final String idempotencyKey;
  final String status; // 'PENDING' | 'IN_FLIGHT' | 'FAILED' | 'RESOLVED'
  final int retryCount;
  final String? lastError;
  final DateTime createdAt;
  final DateTime updatedAt;

  SyncQueueModel({
    required this.queueId,
    required this.businessId,
    required this.entityType,
    required this.entityId,
    required this.operationType,
    required this.payloadJson,
    required this.idempotencyKey,
    this.status = 'PENDING',
    this.retryCount = 0,
    this.lastError,
    required this.createdAt,
    required this.updatedAt,
  });

  Map<String, dynamic> toMap() {
    return {
      'queue_id': queueId,
      'business_id': businessId,
      'entity_type': entityType,
      'entity_id': entityId,
      'operation_type': operationType,
      'payload_json': payloadJson,
      'idempotency_key': idempotencyKey,
      'status': status,
      'retry_count': retryCount,
      'last_error': lastError,
      'created_at': createdAt.toIso8601String(),
      'updated_at': updatedAt.toIso8601String(),
    };
  }

  factory SyncQueueModel.fromMap(Map<String, dynamic> map) {
    return SyncQueueModel(
      queueId: map['queue_id'] as String,
      businessId: map['business_id'] as String,
      entityType: map['entity_type'] as String,
      entityId: map['entity_id'] as String,
      operationType: map['operation_type'] as String,
      payloadJson: map['payload_json'] as String,
      idempotencyKey: map['idempotency_key'] as String,
      status: map['status'] as String? ?? 'PENDING',
      retryCount: map['retry_count'] as int? ?? 0,
      lastError: map['last_error'] as String?,
      createdAt: DateTime.parse(map['created_at'] as String),
      updatedAt: DateTime.parse(map['updated_at'] as String),
    );
  }
}

class AuditLogModel {
  final String id;
  final String businessId;
  final String action; // 'CREATE' | 'UPDATE' | 'REVERSE' | 'BACKUP' | 'RESTORE'
  final String entityType;
  final String entityId;
  final String performedBy;
  final String performedByName;
  final String detailsJson;
  final DateTime createdAt;

  AuditLogModel({
    required this.id,
    required this.businessId,
    required this.action,
    required this.entityType,
    required this.entityId,
    required this.performedBy,
    required this.performedByName,
    required this.detailsJson,
    required this.createdAt,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'business_id': businessId,
      'action': action,
      'entity_type': entityType,
      'entity_id': entityId,
      'performed_by': performedBy,
      'performed_by_name': performedByName,
      'details_json': detailsJson,
      'created_at': createdAt.toIso8601String(),
    };
  }

  factory AuditLogModel.fromMap(Map<String, dynamic> map) {
    return AuditLogModel(
      id: map['id'] as String,
      businessId: map['business_id'] as String,
      action: map['action'] as String,
      entityType: map['entity_type'] as String,
      entityId: map['entity_id'] as String,
      performedBy: map['performed_by'] as String,
      performedByName: map['performed_by_name'] as String,
      detailsJson: map['details_json'] as String,
      createdAt: DateTime.parse(map['created_at'] as String),
    );
  }
}

