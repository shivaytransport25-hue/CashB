# Shivay CashBook — Commercial Cash Book & Ledger Management System

**Shivay CashBook** is a real-time, cloud-synchronized commercial Cash Book, Party Khata Ledger, and Financial Audit Management System engineered for Indian businesses, transport & logistics operators, traders, and multi-branch enterprises.

---

## 1. System Architecture & Technology Stack

### Native Android Application (`/app`)
- **UI Framework**: Jetpack Compose (Material 3 utilitarian accounting register design with `1dp` hairline borders, dense tabular voucher rows, and monospaced numeric columns).
- **Typography**: Bundled local Google Fonts (`Plus Jakarta Sans`, `Inter`, and `JetBrains Mono` for right-aligned tabular figures).
- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) and Firebase Authentication.
- **Database & Real-Time Sync**: Cloud Firestore (`ai-studio-android-shivayca-1eed692a-22c9-4cae-897e-2c89377484c8`) with built-in offline persistence and real-time `addSnapshotListener` flows.
- **Security**: Firebase App Check (`AppCheckHelper`) + Zero-Trust Firestore Security Rules (`firestore.rules`).
- **AI Financial Audit & Forecasting**: Firebase AI Logic (`gemini-4-argon` with `ThinkingLevel.HIGH`) for working-capital audit, burn-rate check, party credit exposure analysis, and 30-day cashflow forecasting.
- **Reporting & Native Printing**:
  - **A4 Statement of Account (PDF)** via `android.graphics.pdf.PdfDocument`.
  - **Half-A4 Commercial Receipt / Payment Voucher Slip (PDF)** with Amount in Words (`Rupees ... Only`) and signatory block.
  - **Direct Printing** via Android `PrintManager` (`PrintDocumentAdapter`).
  - **Microsoft Excel Register (`.CSV`)** with UTF-8 BOM (`0xEF 0xBB 0xBF`) so Excel on Windows/macOS renders the Indian Rupee symbol (`₹`) and Unicode text accurately.
  - **Full System JSON Backup (`.JSON`)** with record-count verification across Cash Books, Parties, and Vouchers.

---

## 2. Cloud Firestore Database Schema (`firebase-blueprint.json`)

All user and business records are isolated under the authenticated user's UID path and validated by `firestore.rules`:

```text
/users/{userId}
  ├── uid: string (immutable)
  ├── email: string
  ├── displayName: string
  ├── createdAt: timestamp (immutable)
  └── updatedAt: timestamp
  │
  ├── /cashbooks/{bookId}
  │     ├── ownerId: string (must equal userId, immutable)
  │     ├── name: string (1..80 chars, e.g. "Main Office Cash Book", "SBI Current A/c #4092")
  │     ├── description: string (0..200 chars)
  │     ├── currencySymbol: string (default "₹")
  │     ├── createdAt: timestamp (immutable)
  │     └── updatedAt: timestamp
  │
  ├── /parties/{partyId}
  │     ├── ownerId: string (must equal userId, immutable)
  │     ├── name: string (1..100 chars, e.g. "Bharat Logistics & Freight")
  │     ├── phone: string (0..30 chars)
  │     ├── role: "Customer" | "Supplier" | "Staff"
  │     ├── notes: string (0..300 chars)
  │     ├── createdAt: timestamp (immutable)
  │     └── updatedAt: timestamp
  │
  └── /transactions/{transactionId}
        ├── ownerId: string (must equal userId, immutable)
        ├── bookId: string (references /cashbooks/{bookId})
        ├── type: "CASH_IN" | "CASH_OUT"
        ├── amount: number (> 0, <= 1,000,000,000)
        ├── remark: string (0..500 chars)
        ├── category: string (1..60 chars, e.g. "Capital / Opening", "Account Transfer (Contra)", "Fuel & Diesel")
        ├── paymentMode: string ("Cash" | "UPI" | "Bank Transfer" | "Cheque" | "Card")
        ├── partyId: string (optional reference to /parties/{partyId})
        ├── partyName: string (denormalized display name)
        ├── transactionDate: int (epoch millis)
        ├── createdAt: timestamp (immutable)
        └── updatedAt: timestamp
```

---

## 3. Financial Calculation Engine Invariants (`FinancialCalculationEngine.kt`)

1. **Exact Decimal / Paise Minor-Unit Arithmetic**:
   - All financial additions, subtractions, running balances, and party settlements convert currency amounts to exact integer Paise (`BigDecimal.valueOf(amount).multiply(100).setScale(0, RoundingMode.HALF_UP)`) before summation, eliminating IEEE-754 floating-point drift (`0.1 + 0.2 != 0.30000000000000004`).
2. **Opening Balance & Contra Transfer Separation**:
   - Opening capital entries (`category == "Capital / Opening"`) are tracked distinctly in `openingBalance`.
   - Account-to-Account Contra Transfers (`category == "Account Transfer (Contra)"`) update the respective source and destination Cash/Bank book closing balances (`closingBalance = openingBalance + operatingCashIn - operatingCashOut + transferIn - transferOut`) **without inflating operating income or operating expense totals**.
3. **Immutable Reversal Audit Trail**:
   - Posted vouchers can be reversed via **Post Reversal Entry**, which creates a counter-entry (`CASH_IN` ↔ `CASH_OUT`) prefixed with `[REVERSAL of VCH-XXXXXX]` so chronological audit history is preserved.
4. **Indian Rupee Formatting & Amount-in-Words**:
   - Standard Indian Lakh/Crore grouping: `₹1,000.00`, `₹25,500.00`, `₹1,25,000.00`, `₹1,00,00,000.00`.
   - Automatic conversion to Indian accounting words: `Rupees One Lakh Twenty-Five Thousand Only`.
   - Standard Indian date format across all registers and exports: `DD-MM-YYYY`.

---

## 4. Verification & Testing Commands

### Run Unit & Financial Calculation Engine Tests
```bash
gradle :app:testDebugUnitTest --tests 'com.example.FinancialCalculationEngineTest'
```

### Run Firestore Security Rules Tests (Node.js + Kotlin Robolectric)
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
gradle :app:testDebugUnitTest --tests '*Rule*'
```
