# Cash Book Management System - Database Schema Documentation

## 1. Overview
The database uses a dual-layer approach:
1. **Local Relational SQLite**: High-performance local querying, transactions, offline queue, and zero-latency UI rendering.
2. **Cloud NoSQL Firestore**: Real-time subscriptions, horizontal scalability, multi-device distribution, and rule-based security.

Both databases adhere to the same domain model and use integer `paise` for monetary values.

---

## 2. Cloud Firestore Schema

### 2.1 Collection: `businesses`
Document ID: `businessId` (UUID v4)
```json
{
  "id": "biz_8f29e...",
  "name": "Shivay Transport & Logistics",
  "ownerId": "usr_99182...",
  "ownerName": "Rajesh Kumar",
  "logoUrl": "https://firebasestorage...",
  "address": "Shop 12, Transport Nagar, Surat, Gujarat",
  "phone": "+91 9876543210",
  "email": "contact@shivay.com",
  "gstin": "24ABCDE1234F1Z5",
  "pan": "ABCDE1234F",
  "currencyCode": "INR",
  "currencySymbol": "₹",
  "financialYearStartMonth": 4,
  "createdAt": "2026-04-01T00:00:00Z",
  "updatedAt": "2026-11-05T10:00:00Z"
}
```

### 2.2 Sub-Collection: `businesses/{businessId}/members`
Document ID: `userId`
```json
{
  "userId": "usr_10293...",
  "email": "accountant@shivay.com",
  "displayName": "Amit Verma",
  "role": "Accountant",
  "permissions": ["create_transaction", "view_reports", "export_data"],
  "status": "active",
  "joinedAt": "2026-04-05T12:00:00Z"
}
```

### 2.3 Sub-Collection: `businesses/{businessId}/accounts`
Document ID: `accountId` (UUID v4)
```json
{
  "id": "acc_main_cash",
  "businessId": "biz_8f29e...",
  "accountName": "Main Cash Counter",
  "accountType": "cash", // 'cash' | 'bank'
  "openingBalancePaise": 5000000, // ₹50,000.00
  "openingDate": "2026-04-01",
  "currentBalancePaise": 12850000, // ₹1,28,500.00
  "bankName": null,
  "accountNumber": null,
  "ifscCode": null,
  "branch": null,
  "allowNegativeBalance": false,
  "isActive": true,
  "createdAt": "2026-04-01T00:00:00Z",
  "updatedAt": "2026-11-05T11:30:00Z"
}
```

### 2.4 Sub-Collection: `businesses/{businessId}/categories`
Document ID: `categoryId` (UUID v4)
```json
{
  "id": "cat_fuel_expense",
  "businessId": "biz_8f29e...",
  "name": "Diesel & Fuel",
  "type": "expense", // 'income' | 'expense'
  "parentCategoryId": null,
  "iconName": "local_gas_station",
  "colorHex": "#D32F2F",
  "isActive": true,
  "createdAt": "2026-04-01T00:00:00Z",
  "updatedAt": "2026-04-01T00:00:00Z"
}
```

### 2.5 Sub-Collection: `businesses/{businessId}/parties`
Document ID: `partyId` (UUID v4)
```json
{
  "id": "pty_reliance_ind",
  "businessId": "biz_8f29e...",
  "name": "Reliance Logistics Client",
  "partyType": "customer", // 'customer' | 'supplier' | 'employee'
  "phone": "+91 9988776655",
  "email": "billing@reliance.com",
  "address": "Hazira Industrial Area, Surat",
  "openingBalancePaise": 2500000, // ₹25,000.00
  "openingBalanceType": "receivable", // 'receivable' | 'payable'
  "currentBalancePaise": 6500000,
  "gstin": "24AAACR1234F1Z8",
  "isActive": true,
  "createdAt": "2026-04-01T00:00:00Z",
  "updatedAt": "2026-11-05T12:00:00Z"
}
```

### 2.6 Sub-Collection: `businesses/{businessId}/transactions`
Document ID: `transactionId` (UUID v4)
```json
{
  "id": "txn_89410...",
  "businessId": "biz_8f29e...",
  "voucherNumber": "VCH-2026-0084",
  "transactionDate": "2026-11-05",
  "transactionTime": "14:15:00",
  "transactionType": "cashPaid", // 'cashReceived' | 'cashPaid' | 'bankReceived' | 'bankPayment' | 'transfer' | 'openingBalance' | 'adjustment'
  "accountId": "acc_main_cash",
  "targetAccountId": null, // Populated only for transfers
  "transferId": null, // Links both legs of a transfer atomically
  "categoryId": "cat_fuel_expense",
  "partyId": "pty_reliance_ind",
  "partyName": "Reliance Logistics Client",
  "description": "Diesel for truck GJ-05-XX-1234",
  "paymentMode": "Cash", // 'Cash' | 'Cheque' | 'NEFT/RTGS' | 'UPI' | 'Card'
  "amountPaise": 550000, // ₹5,500.00
  "referenceNumber": "BILL-9082",
  "attachmentUrls": ["https://firebasestorage..."],
  "status": "posted", // 'draft' | 'posted' | 'reversed' | 'cancelled'
  "reversalReason": null,
  "reversedTransactionId": null,
  "originalTransactionId": null,
  "createdBy": "usr_10293...",
  "createdByName": "Amit Verma",
  "deviceId": "dev_win_x98...",
  "idempotencyKey": "idem_30924...",
  "createdAt": "2026-11-05T14:15:00Z",
  "updatedAt": "2026-11-05T14:15:00Z"
}
```

---

## 3. Local SQLite Schema (Offline Engine)

### 3.1 DDL Statements & Indexes
```sql
-- Accounts Table
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
);
CREATE INDEX idx_accounts_business ON local_accounts(business_id);

-- Categories Table
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
);
CREATE INDEX idx_categories_business ON local_categories(business_id);

-- Parties Table
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
);
CREATE INDEX idx_parties_business ON local_parties(business_id);

-- Transactions Table
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
    sync_status TEXT NOT NULL DEFAULT 'SYNCED' -- 'PENDING' | 'SYNCED' | 'FAILED'
);
CREATE INDEX idx_txn_date ON local_transactions(business_id, transaction_date);
CREATE INDEX idx_txn_account ON local_transactions(business_id, account_id);
CREATE INDEX idx_txn_party ON local_transactions(business_id, party_id);
CREATE INDEX idx_txn_category ON local_transactions(business_id, category_id);
CREATE INDEX idx_txn_sync_status ON local_transactions(sync_status);

-- Persistent Offline Synchronization Queue
CREATE TABLE sync_queue (
    queue_id TEXT PRIMARY KEY,
    business_id TEXT NOT NULL,
    entity_type TEXT NOT NULL, -- 'transaction' | 'account' | 'party' | 'category'
    entity_id TEXT NOT NULL,
    operation_type TEXT NOT NULL, -- 'INSERT' | 'UPDATE' | 'DELETE'
    payload_json TEXT NOT NULL,
    idempotency_key TEXT UNIQUE NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING' | 'IN_FLIGHT' | 'FAILED' | 'RESOLVED'
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE INDEX idx_sync_queue_status ON sync_queue(status, created_at);

-- Audit Log Table
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
);
CREATE INDEX idx_audit_business ON local_audit_logs(business_id, created_at);
```

