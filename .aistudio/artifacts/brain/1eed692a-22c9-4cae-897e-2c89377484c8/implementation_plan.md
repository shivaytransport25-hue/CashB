# Shivay CashBook & Ledger

Shivay CashBook & Ledger is a real-time cloud-synchronized commercial bookkeeping application built for daily business operations. It enables business owners and accountants to record Cash In and Cash Out vouchers across multiple independent books, track customer and supplier party balances, export standard PDF and Excel-ready CSV account statements for PC accounting, and run on-demand financial audit and cashflow projections.

## User Review & Critical Decisions

> [!IMPORTANT]
> Review the updated design and product specifications below—specifically tailored to ensure a genuine, human-crafted commercial accounting software look and feel with zero "AI-generated" visual clichés.

- **Confirmed Decision — Authentic Human-Engineered Accounting UI**: The interface strictly avoids generic "AI-generated" aesthetics (no neon gradients, no purple glows, no decorative sparkles, no chatbot gimmicks, and no bloated rounded cards). Instead, it uses a **classic, utilitarian business accounting layout** inspired by battle-tested bookkeeping software (Tally, Zoho Books, Khatabook, and classic CashBook apps) with crisp `1dp` hairline borders, dense tabular voucher rows, right-aligned monospaced figures, and standard accounting terminology.
- **Confirmed Decision — Business & Multi-Book Ledger Management**: Supports multiple separate business cash books (e.g., *Main Office Cash*, *Transport & Fleet Ops*, *Petty Cash*, *Branch Ledger*) with instant switching, opening/closing balance summaries, Total Cash In (`+`), and Total Cash Out (`-`).
- **Confirmed Decision — Full Commercial Feature Suite**: Includes Party/Customer ledger balance tracking (*Receivable / You'll Get* vs. *Payable / You'll Give*), Payment Mode & Head of Account (Category) filtering, one-tap PDF Account Statement & Excel CSV export for PC, and a professional **Financial Audit & Cashflow Forecast** module (powered under the hood by `gemini-4-argon` with `ThinkingLevel.HIGH`, presented as a clean executive audit report rather than an AI gimmick).
- **Confirmed Decision — Real-Time Cloud Sync**: Instant multi-device synchronization backed by Google Sign-In and Firebase Firestore so entries logged on mobile are immediately reflected across all signed-in devices.

---

## 1. Overview & Core Concept

- **What It Does**: Acts as a dependable digital register for daily business cash and bank movements. Every Cash In (Receipt/Credit) and Cash Out (Payment/Debit) entry immediately updates the running balance, updates the linked Party account, and syncs to the cloud.
- **Target Audience / Persona**: Business proprietors, transport & fleet operators, traders, and accounting staff who value speed, accuracy, high information density, and clean printable statements.
- **Key Value**: Fast 3-second voucher entry, zero calculation mistakes, instant party reconciliation, and standard CSV/PDF exports that open directly in Microsoft Excel or desktop accounting software on PC.

---

## 2. User Experience & Visual Design (Human-Crafted Commercial Standard)

### Strict Anti-"AI Slop" Design Rules

1. **Zero Decorative Gradients or Neon Accents**: Solid, high-contrast surfaces only—Crisp Paper White (`#FFFFFF`), Warm Ledger Gray (`#F4F5F7`), and Deep Charcoal Navy (`#111827`) for headers.
2. **Tabular Accounting Grid & Hairline Dividers**: Transactions are presented in structured date-grouped ledger tables with `1dp` structural dividers (`#E2E8F0`), clear column alignment, and each entry's chronological **Running Balance (`Bal: ₹...`)** printed directly below the transaction amount.
3. **Classic Accounting Color Semantics**:
   - **Cash In (Credit / Receipt)**: Classic Accounting Green (`#15803D` text, `#DCFCE7` subtle badge tint).
   - **Cash Out (Debit / Payment)**: Classic Accounting Crimson (`#B91C1C` text, `#FEE2E2` subtle badge tint).
   - **Primary Action / Header Bar**: Business Slate Blue (`#1E3A5F`), authoritative and distraction-free.
4. **Standard Bookkeeping Terminology**: Uses authentic accounting terms throughout (*Cash Book*, *Party Ledger*, *Head of Account*, *Payment Mode*, *Opening/Closing Balance*, *Net Balance*, *Voucher Entry*, *Statement of Account*, *Audit & Forecast*).

### Key User Flows

1. **Account Sign-In & Cloud Register Sync**:
   - Clean, professional business sign-in screen with Google Sign-In (`CredentialManager`) that immediately loads the user's cloud books and party registers.
2. **Book Register & Summary Header**:
   - Compact top bar showing the active Cash Book title, currency selector, and a one-tap **Switch Book** selector.
   - Structured **Balance Passbook Header** displaying **Net Balance**, **Total Cash In (+)**, **Total Cash Out (-)**, and entry count, alongside quick filter pills (Date Range, Entry Type, Payment Mode, Account Head, Party) and a direct **PDF / Excel CSV Export** action button.
3. **Fast Voucher Entry (`+ CASH IN` / `- CASH OUT`)**:
   - Two solid, full-width rectangular action buttons anchored at the bottom of the register: **+ CASH IN** (Green) and **- CASH OUT** (Red).
   - Tapping either opens a focused, keyboard-first **Voucher Entry Sheet** with: Amount field, Particulars / Remark, Party selector (with inline *+ Add New Party*), Head of Account (Sales, Freight/Trip, Fuel & Diesel, Salary/Bhatta, Maintenance, Office Expense, Supplier Payment, Advance), Payment Mode (`Cash`, `Bank / NEFT`, `UPI`, `Cheque`), and Voucher Date/Time.
4. **Party / Khata Ledger Tab**:
   - Dedicated register of Customers, Suppliers, and Staff showing total **To Receive (Dr)** and **To Pay (Cr)** across the business, with individual party statement drill-down and one-tap settlement recording.
5. **Financial Audit & Forecast Tab**:
   - Presented as a clean, tabular **Business Audit & Cashflow Report** screen. Users can generate a comprehensive financial audit (analyzing monthly burn rate, top expense heads, overdue party receivables, and 30-day cashflow projection) or ask specific accounting questions about their ledger data.
6. **PC Statement Export (Printable PDF & Excel CSV)**:
   - Generates a formal, multi-column **Statement of Account PDF** (complete with business title, date range, summary box, and tabular Date / Particulars / Party / Mode / Cash In / Cash Out / Running Balance columns) or an **Excel-compatible CSV** file for desktop reconciliation.

### Typography & Hierarchy

- **Headings & UI Labels**: *Plus Jakarta Sans* (`SemiBold` / `Bold`) for clean, legible labels and section headers.
- **Amounts & Running Balances**: *JetBrains Mono* (`Medium` / `Bold`) with tabular numeral alignment so columns of figures line up to the decimal point.
- **Body & Particulars**: *Inter* (`Regular` / `Medium`) for high legibility at compact table densities.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Per-User Cloud Register Hierarchy with Real-Time Snapshot Sync**
  - *Chosen Approach*: Store books, vouchers, and parties under `/users/{userId}/cashbooks`, `/users/{userId}/transactions`, and `/users/{userId}/parties` in Firebase Firestore using real-time `.snapshots()` Flows.
  - *Why*: Ensures every entry made on a phone is instantaneously visible on any other mobile or PC emulator device signed into the same account, protected by strict per-user Firestore security rules.
- **Decision 2: Chronological Running Balance Computation**
  - *Chosen Approach*: Compute running balance after every transaction in chronological order (`transactionDate` + `createdAt`) and display it on each row, just like a physical bank passbook or Tally ledger.
  - *Why*: Business owners rely on line-by-line running balances to spot discrepancies immediately when reviewing or printing statements.
- **Decision 3: Executive Financial Audit Powered by High-Thinking Model (`gemini-4-argon`)**
  - *Chosen Approach*: Use Firebase AI Logic with `gemini-4-argon` and `ThinkingLevel.HIGH` (no `maxOutputTokens` cap) behind a utilitarian **Audit & Forecast** report interface.
  - *Why*: Delivers deep quantitative reasoning on real ledger numbers while keeping the UI grounded in serious accounting software design rather than a novelty chatbot.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                          │
│         configureAppCheck(this, intent) -> AuthGate -> MainScaffold         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
       ┌───────────────────────────────┼───────────────────────────────┐
       ▼                               ▼                               ▼
┌─────────────────────┐   ┌─────────────────────────┐   ┌─────────────────────┐
│   Business Auth     │   │   Ledger & Khata UI     │   │  Audit & PC Export  │
│  ┌───────────────┐  │   │  ┌───────────────────┐  │   │  ┌───────────────┐  │
│  │ SignInScreen  │  │   │  │ CashBookRegister  │  │   │  │ AuditReportTab│  │
│  │ (Google Auth) │  │   │  │ BookSwitcherSheet │  │   │  │ (HighThinking)│  │
│  └───────────────┘  │   │  │ VoucherEntrySheet │  │   │  ├───────────────┤  │
│                     │   │  │ PartyLedgerScreen │  │   │  │ StatementExpt │  │
│                     │   │  └───────────────────┘  │   │  │ (PDF & Excel) │  │
│                     │   │                         │   │  └───────────────┘  │
└──────────┬──────────┘   └────────────┬────────────┘   └──────────┬──────────┘
           │                           │                           │
           └───────────────────────────┼───────────────────────────┘
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     CashBookViewModel & AuditViewModel                      │
│  • Reactive StateFlows: activeBook, ledgerRowsWithRunningBalance, summary   │
│  • Party Khata aggregation (Total Receivable vs Total Payable)              │
│  • Filter state: searchQuery, entryType, paymentMode, category, partyId     │
└──────────────────────┬───────────────────────────────┬──────────────────────┘
                       │                               │
                       ▼                               ▼
┌─────────────────────────────────────────┐ ┌─────────────────────────────────┐
│          CashBookRepository             │ │       FinancialAuditService     │
│  • Custom DB ID: R.string.firestore_... │ │  • Firebase.ai.generativeModel  │
│  • Real-time .snapshots() Flows         │ │  • Model: "gemini-4-argon"      │
│  • Strict requireUserId() & error logs  │ │  • ThinkingLevel.HIGH           │
└──────────────────────┬──────────────────┘ └─────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│               Firebase Cloud Backend (Firestore + Auth + AI)                │
│  • /users/{uid}/cashbooks/{bookId}                                          │
│  • /users/{uid}/transactions/{txId}                                         │
│  • /users/{uid}/parties/{partyId}                                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

1. **`CashBook`** (`/users/{userId}/cashbooks/{bookId}`):
   - `id`, `userId`, `name`, `description`, `currencySymbol` (default `"₹"`), `createdAt`, `updatedAt`.
2. **`CashTransaction`** (`/users/{userId}/transactions/{txId}`):
   - `id`, `userId`, `bookId`, `type` (`"CASH_IN"` | `"CASH_OUT"`), `amount` (`Double`), `remark` (`String`), `category` (`String`), `paymentMode` (`String`), `partyId` (`String`), `partyName` (`String`), `transactionDate` (`Long`), `createdAt`, `updatedAt`.
3. **`Party`** (`/users/{userId}/parties/{partyId}`):
   - `id`, `userId`, `name`, `phone`, `role` (`"Customer"` | `"Supplier"` | `"Staff"`), `notes`, `createdAt`, `updatedAt`.

### Interactive Component & State Mapping

- **Cash Book Register & Book Switcher**: Switching or creating a book updates the active stream immediately. If a new user has zero books, a one-tap "Create Default Business Cash Book" or sample ledger initializer is available.
- **Voucher Entry & Edit Sheet**: Validates positive numeric amounts and saves to Firestore with server timestamps. Tapping any existing row opens the voucher editor to modify details or delete the entry with confirmation.
- **Party Ledger & Settlement**: Aggregates net balance per party across all linked vouchers, supports filtering the register by party, and allows recording a quick "Received" or "Paid" settlement voucher directly from the party detail view.
- **PDF & Excel CSV Statement Exporter**: Builds a clean, tabular multi-page PDF statement and RFC-4180 CSV file in cache storage and invokes the Android share/save sheet via `FileProvider`.
