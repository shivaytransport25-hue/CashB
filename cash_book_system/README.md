# Cash Book Pro - Enterprise Multi-Device Financial Management System

A cross-platform financial accounting and cash book management system built for **Windows Desktop**, **Android**, **iOS**, and **macOS**. Engineered with an **offline-first SQLite database**, **real-time Google Cloud Firestore synchronization**, and **zero-floating-point financial calculation precision** in Indian Rupee (`₹`).

---

## 📦 Ready-to-Install Build Deliverables

The application packages are compiled and located in the project root:

| Platform | Deliverable File | Type | Description |
|---|---|---|---|
| 🪟 **Windows Desktop** | `CashBook_Setup_v1.0.0.exe` | Inno Setup Installer | Standard Windows setup wizard with desktop shortcut, start menu entry, and uninstaller. |
| 🤖 **Android Mobile** | `CashBookPro_v1.0.0.apk` | Release Package | Sideloadable Android application package for phones and tablets. |
| 🍎 **iOS / iPhone** | `CashBookPro_v1.0.0.ipa` | Release IPA Archive | iOS application package ready for sideloading (AltStore, Sideloadly) or enterprise distribution. |

---

## 🏛 System Architecture & Technology Stack

- **Frontend Framework**: Flutter (Dart) with responsive Material 3 design adapting between mobile bottom navigation and desktop sidebar.
- **Local Storage Engine**: SQLite with `sqflite` (Android/iOS) and `sqflite_common_ffi` (Windows/macOS) for instant local-first CRUD operations.
- **Cloud Backend**: Google Cloud Firestore with real-time stream listeners, multi-tenant isolation, and automated conflict resolution.
- **Reporting & Printing Engine**: Native Excel (`.xlsx`) generation with live summation formulas, vector PDF landscape Cash Book reports, printable transaction vouchers, and raw CSV exports.

---

## 💎 Core Financial & Accounting Features

1. **Integer Paise Financial Precision**:
   - Stored in integer `paise` (`₹1.00 = 100 paise`) across SQLite and Firestore.
   - Eliminates IEEE 754 binary floating-point rounding discrepancies (`0.1 + 0.2 = 0.30000000000000004`).
2. **Indian Numbering Formatting**:
   - Formats amounts according to the Indian grouping system: `₹12,34,567.89`.
   - Built-in currency-to-words converter for vouchers (Crores, Lakhs, Thousands, Rupees, Paise).
3. **Atomic Account-to-Account Transfers**:
   - Transfers generate a linked debit and credit pair sharing a single `transferId`.
   - Transfers never pollute income or expense ledgers.
4. **Audit Trail & Immutable Records**:
   - Posted financial transactions cannot be hard-deleted.
   - Correction is executed through authorized reversal entries (`isReversal = true`), linking back to the original voucher ID.
5. **Role-Based Access Control (RBAC)**:
   - **Owner**: Complete system control, business configuration, user management, and cloud backups.
   - **Administrator**: Transaction management, account setup, category controls, permitted user assignment.
   - **Accountant**: Transaction entry, financial reports, Excel/PDF exports.
   - **Data Entry Operator**: Transaction recording only; no access to posted transaction modifications.
   - **Viewer**: Read-only ledger view.

---

## 🚀 Installation & Running Guide

### 1. Windows Desktop (`CashBook_Setup_v1.0.0.exe`)
1. Double-click `CashBook_Setup_v1.0.0.exe` in the project root.
2. Follow the standard Inno Setup installation wizard:
   - Choose install directory (defaults to `C:\Users\<User>\AppData\Local\Programs\CashBookPro`).
   - Choose desktop shortcut option.
   - Click **Install** and **Finish**.
3. Launch **CashBook Pro** from your Desktop or Start Menu.
4. *Keyboard Shortcuts*:
   - `Ctrl + N`: Open New Transaction Entry.
   - `Ctrl + R`: Refresh & Force Sync Queue.

### 2. Android Mobile (`CashBookPro_v1.0.0.apk`)
1. Transfer `CashBookPro_v1.0.0.apk` to your Android device via USB, Google Drive, or local share.
2. Open the file on your device. If prompted, allow "Install from Unknown Sources".
3. Tap **Install** and launch **CashBook Pro**.

### 3. iOS Device (`CashBookPro_v1.0.0.ipa`)
- **Option A (AltStore / Sideloadly)**:
  1. Connect your iPhone to your PC/Mac.
  2. Open **Sideloadly** or **AltServer**.
  3. Select `CashBookPro_v1.0.0.ipa` and enter your Apple ID to sign and install the app on your device.
- **Option B (Apple Developer / TestFlight)**:
  - Upload via Xcode Organizer or Fastlane to your Apple Developer Console for internal/external TestFlight distribution.

---

## ☁️ Firebase Cloud Configuration

To connect the application to your Google Firebase project:

1. **Place Configuration Files**:
   - Android: `android/app/google-services.json`
   - iOS: `ios/Runner/GoogleService-Info.plist`
   - Web/Desktop: `lib/firebase_options.dart` (generated via `flutterfire configure`)
2. **Deploy Firestore Security Rules**:
   ```bash
   firebase deploy --only firestore:rules
   ```
   *(Production rules are provided in `firestore.rules`)*
3. **Deploy Firebase Cloud Functions**:
   ```bash
   cd functions/firebase_functions
   npm install
   firebase deploy --only functions
   ```

---

## 🧪 Automated Testing

All financial calculations and balance equations are verified with automated unit tests:

```bash
flutter test
```

### Verified Test Matrix:
- ✅ Integer paise precision currency conversions (`₹1,250.50` ⇄ `125050 paise`).
- ✅ Indian number format regex grouping (`₹1,00,00,000.00`).
- ✅ Closing balance formula: `Opening + Receipts - Payments + Net Transfers`.
- ✅ Double-entry atomic account-to-account transfer integrity.
- ✅ Reversal entry generation and audit linkage.
- ✅ Idempotency key generation and duplicate prevention.

---

## 📁 Source Directory Structure

```
cash_book_system/
├── lib/
│   ├── core/                  # Precision math, Indian Rupee formatting, Enums
│   ├── models/                # Business, User, Account, Transaction, Party, Audit models
│   ├── database/              # SQLite cross-platform local database engine (FFI & Mobile)
│   ├── services/              # Auth, Accounting, Reports (Excel/PDF/CSV), Backup services
│   ├── sync/                  # Real-time Cloud Sync & Persistent Queue Drainage engine
│   ├── accounting/            # Central financial rules and calculation engine
│   ├── authentication/        # Login, Register, Role switcher screens
│   ├── dashboard/             # Financial cards, income/expense charts, balance charts
│   ├── transactions/          # Multi-tab entry, searchable list, voucher printing
│   ├── accounts/              # Cash & Bank account cards, ledger statements
│   ├── parties/               # Customer, Supplier, Employee ledgers and balances
│   ├── reports/               # Date-filtered reporting, Excel export, PDF Cash Book
│   ├── backup/                # Encrypted/JSON database snapshot and restore
│   ├── settings/              # Business profile, RBAC permissions matrix, sync controls
│   └── main.dart              # MultiProvider root, responsive layout, shortcut listeners
├── functions/firebase_functions/ # Cloud Functions for atomic validation
├── firestore.rules            # Multi-tenant security & RBAC enforcement rules
├── windows/installer_script.iss # Inno Setup 6 packaging configuration
└── test/                      # Comprehensive unit tests for financial engine
```

