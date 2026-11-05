# Cash Book Management System - Architecture & Technical Design

## 1. Executive Overview
The **Cash Book Management System** is a professional, multi-platform financial management application built with Flutter, SQLite (offline-first), Firebase Cloud Firestore (real-time cloud synchronization), Firebase Authentication, Firebase Cloud Functions, and Firebase Storage.

The system is designed for small to enterprise businesses that require robust cash and bank accounting, multi-user role-based access, multi-device synchronization, offline transaction capability, and audit-grade financial reporting.

---

## 2. Target Platforms
- **Android Mobile**: Optimized for fast transaction recording, mobile responsiveness, receipt sharing (WhatsApp / PDF), and offline-first usage.
- **Windows Desktop**: High-density financial dashboard, keyboard navigation shortcuts, sidebar navigation, extensive multi-column data grids, Excel / PDF export, and direct printing.
- **iOS Mobile**: Native iOS design compliance, secure local storage, biometric unlock, and real-time syncing.
- **macOS Desktop**: High-resolution financial reporting, keyboard shortcuts, and direct printing support.

---

## 3. Core Architecture Principles

### 3.1 Offline-First with Conflict-Free Synchronization
1. **Local Authoritative Cache**: Every transaction, account modification, party update, and category creation is committed to the local SQLite database first.
2. **Deterministic Transaction Queue**: Unsynchronized actions are appended to a persistent `sync_queue` table with a globally unique UUID and an idempotency key.
3. **Background Sync Worker**: When connectivity is verified, the synchronization engine dequeues pending items in strict chronological order and posts them to Cloud Firestore.
4. **Optimistic UI with Synchronized State Feedback**: The UI immediately reflects changes while indicating whether a record is `Pending Sync` or `Confirmed Cloud`.
5. **No Silent Overwrites**: Conflicting edits trigger a conflict resolution strategy where transaction history is preserved, and adjustments or reversals are documented.

### 3.2 Strict Financial Precision & Zero Floating-Point Arithmetic
1. **Paise / Minor Currency Unit Storage**: All monetary amounts are stored as 64-bit integers representing `paise` (1 INR = 100 paise).
2. **Decimal Formatting & Parsing**:
   - `100000 paise` $\rightarrow$ `₹1,000.00`
   - `12500000 paise` $\rightarrow$ `₹1,25,000.00` (Indian Lakhs/Crores numbering convention)
3. **Centralized Accounting Calculation Engine**:
   - `Closing Balance = Opening Balance + Total Money Received - Total Money Paid`
   - Transfers decrease the source account and increase the destination account atomically without impacting Income, Expense, or Profit calculations.
   - Reversals cancel transactions by posting an inverse entry with an explicit reference to the original `transactionId` and reason for reversal.

### 3.3 Role-Based Access Control (RBAC) & Multi-Tenancy
- **Tenant Isolation**: Every business has an independent namespace `businesses/{businessId}`.
- **Roles**:
  - `Owner`: Unrestricted access, user management, billing, business profile, backup/restore, all reports.
  - `Administrator`: Account management, party/category management, transaction entry/modification, report generation.
  - `Accountant`: Transaction entry, ledger review, report exports, receipt/voucher issuance.
  - `Data Entry Operator`: Transaction creation, draft entry, view permitted daily records; cannot edit or delete posted records.
  - `Viewer`: Read-only access to dashboard and generated reports.

---

## 4. High-Level System Architecture Diagram

```
+-------------------------------------------------------------------------------+
|                             Presentation Layer                                |
|  [Responsive Dashboard]  [Quick Entry Modal]  [Account Grids]  [Reports View] |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                            Application Services                               |
|  [AuthService] [AccountingService] [ReportService] [BackupService] [SyncEngine] |
+-------------------------------------------------------------------------------+
                 |                                             |
                 v                                             v
+----------------------------------+          +----------------------------------+
|       Local Storage Engine       |          |       Cloud Backend Layer        |
|  - SQLite (Drift / sqflite)      |          |  - Firebase Authentication       |
|  - Local Tables (Accounts,       |          |  - Cloud Firestore (Real-Time)   |
|    Parties, Transactions)        | <======> |  - Firebase Cloud Functions      |
|  - Persistent Sync Queue         |  Sync    |  - Firebase Storage (Receipts)   |
|  - Audit Log Table               |  Worker  |  - Security Rules (RBAC)         |
+----------------------------------+          +----------------------------------+
```

---

## 5. Technology Stack Summary
- **Framework**: Flutter 3.x+ / Dart 3.x+
- **Local DB**: SQLite via `sqflite` / `sqflite_common_ffi` (cross-platform desktop & mobile)
- **Cloud DB**: Firebase Cloud Firestore
- **Auth**: Firebase Auth (Email/Password, Multi-Device Session Tracking)
- **File & Receipt Storage**: Firebase Cloud Storage & Local Cache
- **Exporting & Reporting**: `excel`, `pdf`, `printing`, `csv`
- **Design System**: Material Design 3 with custom financial palettes (Light & Dark mode)

