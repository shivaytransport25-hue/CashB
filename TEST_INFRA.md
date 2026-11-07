# Cash Book Pro Test Infrastructure Specification (`TEST_INFRA.md`)

- **Author**: E2E Testing Track Lead (teamwork_test_writer)
- **Status**: ACTIVE & AUTHORITATIVE
- **Date**: 2026-11-06
- **Test Framework**: Standalone Node.js Test Harness (`node:test`, `node:assert`, ES Modules)
- **Runtime Environment**: Node.js v24.20.0+ (zero external npm dependencies required)
- **Location**: `.agents/teamwork/tests/`

---

## 1. Architectural Scope & Testing Philosophy

The Cash Book Pro suite spans three client presentation layers and a centralized Cloud Firestore backend:
1. **Windows Desktop**: Microsoft Edge WebView2 client hosting `index.html` (5,500+ lines).
2. **Android Native**: Jetpack Compose application (`com.example`) with `CredentialManager` and Room/Firestore.
3. **Flutter Mobile**: Cross-platform Flutter engine (`cash_book_system`) with SQLite and Firestore sync.
4. **Cloud Backend**: Google Cloud / Firebase project `gen-lang-client-0037390170` secured by `firestore.rules`.

### Core Test Principles
- **Opaque-Box Requirement-Driven**: Tests are derived strictly from authoritative specifications (`ORIGINAL_REQUEST.md`, `PROJECT.md`, `spec_requirements.md`, and `firestore.rules`).
- **No Facade Tests**: Tests execute actual logic, parsing, cryptographic/hash checks, accounting calculations, and invariant checks. Tests do not mock away the behavior being evaluated.
- **Defect Escalation**: Test writer does not alter production implementation code. When an invariant fails against the codebase, the test framework records the exact file, line, and invariant violation for escalation to the implementing agent.

---

## 2. Invariant Specifications

The test runner continuously enforces four categories of architectural invariants:

### 2.1 Visual Styling & Design System Invariants
| Invariant ID | Target | Rule / Constraint | Authoritative Spec |
|---|---|---|---|
| `INV-VIS-01` | Desktop CSS (`index.html`) | Zero instances of `@keyframes pulseGlow`, `filter: blur()`, neon glows, or oversaturated AI gradients. | `ORIGINAL_REQUEST.md` R1, `spec_requirements.md` §2.1 |
| `INV-VIS-02` | Desktop & Mobile Ledgers | All monetary values display using tabular proportional figures (`font-variant-numeric: tabular-nums` or `tnum` or `JetBrains Mono` / monospace). | `PROJECT.md` F2, `spec_requirements.md` §2.3 |
| `INV-VIS-03` | Table Amount Alignment | Inflow (+), Outflow (-), and Balance columns MUST be strictly right-aligned (`text-align: right`). | `spec_requirements.md` §2.3.2 |
| `INV-VIS-04` | Color Semantic Tokens | Cash In / Credit: Green (`#047857` / `#15803D`); Cash Out / Debit: Red (`#B91C1C` / `#DC2626`); Contra: Blue (`#1D4ED8` / `#2563EB`). | `PROJECT.md` F1, `spec_requirements.md` §2.2.3 |
| `INV-VIS-05` | WCAG Contrast | Text contrast ratio >= 4.5:1 (WCAG AA) on card surfaces; numeric figures >= 7.0:1 (WCAG AAA). | `spec_requirements.md` §2.2.2 |
| `INV-VIS-06` | Mobile Physics | Zero toy bouncy physics (`BounceClickModifier` with scale < 0.95 or breathing loops) in active accounting screens. | `PROJECT.md` F6 |

### 2.2 Authentication & Session Persistence Invariants
| Invariant ID | Target | Rule / Constraint | Authoritative Spec |
|---|---|---|---|
| `INV-AUTH-01` | Desktop WebView2 | Zero dummy user fallbacks (`usr_google_rajesh_patel`). Must use Virtual Host mapping (`https://app.cashbook.local`) or RFC 8252 loopback. | `ORIGINAL_REQUEST.md` R2, `PROJECT.md` F3 |
| `INV-AUTH-02` | Android Native | Zero silent fallback to `signInAnonymously()`. `CredentialManager` errors must report cleanly without masking. | `PROJECT.md` F9, `spec_requirements.md` §3.2.2 |
| `INV-AUTH-03` | Flutter Mobile | Zero mock dummy fallback (`usr_google_default`). Use `google_sign_in` plugin with Web Client ID. | `PROJECT.md` F8, `spec_requirements.md` §3.2.3 |
| `INV-AUTH-04` | OAuth Client IDs | Web Client ID must strictly match `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`. | `PROJECT.md` § Google OAuth Configuration |
| `INV-AUTH-05` | Session Persistence | Durable session cached in persistent local storage (`localStorage['cashbook_auth_user']`, `EncryptedSharedPreferences`). Auto-login gate bypass on app reboot. | `ORIGINAL_REQUEST.md` R2, `PROJECT.md` F4/F10 |
| `INV-AUTH-06` | Explicit Sign-Out | Sign-out completely purges cached session and locks UI behind `modal-login-gate`. | `spec_requirements.md` §3.3.3 |

### 2.3 Cloud Firestore Schema & Sync Invariants
| Invariant ID | Target | Rule / Constraint | Authoritative Spec |
|---|---|---|---|
| `INV-SYNC-01` | Transaction Document Schema | Subcollection `/users/{userId}/transactions/{txId}` MUST enforce strict 12-key whitelist: `['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`. Zero extra keys allowed. | `firestore.rules:189`, `PROJECT.md` § Interface Contracts |
| `INV-SYNC-02` | CashBook Document Schema | Subcollection `/users/{userId}/cashbooks/{bookId}` MUST enforce strict 6-key whitelist: `['userId', 'name', 'description', 'currencySymbol', 'createdAt', 'updatedAt']`. | `firestore.rules:120` |
| `INV-SYNC-03` | Party Document Schema | Subcollection `/users/{userId}/parties/{partyId}` MUST enforce strict 7-key whitelist: `['userId', 'name', 'phone', 'role', 'notes', 'createdAt', 'updatedAt']`. | `firestore.rules:152` |
| `INV-SYNC-04` | Owner Authorization | `userId` in payload must strictly match `request.auth.uid`. Writing with unauthenticated or mismatched UID must fail with `PERMISSION_DENIED`. | `firestore.rules:182` |
| `INV-SYNC-05` | Amount Validation | Amount must be numeric, `> 0` and `<= 1000000000` (100 crore). Stored in Rupees with up to 2 decimal places. | `firestore.rules:85-87` |
| `INV-SYNC-06` | Bidirectional Sync | Local modifications tagged with `_dirty` synced upstream in batches; remote additions merged downstream. | `spec_requirements.md` §3.4.2 |

### 2.4 Accounting & Money Math Invariants
| Invariant ID | Target | Rule / Constraint | Authoritative Spec |
|---|---|---|---|
| `INV-ACC-01` | Integer Paise Arithmetic | All money math performed in integer paise (`minor units`) where ₹1.00 = 100 paise. Zero floating-point rounding errors. | `spec_requirements.md` §4 F11, `cashbook-fix-verify` skill |
| `INV-ACC-02` | Day Book Equation | `Closing Balance == Opening Balance + Total Inflow - Total Outflow` at all times. | `FinancialCalculationEngine.kt` |
| `INV-ACC-03` | Contra Transfer Integrity | Contra transfer between Account A and Account B decrements Account A by X and increments Account B by X. Net cash liquidity across all accounts remains constant. | `PROJECT.md` F2, `spec_requirements.md` §2.2.3 |
| `INV-ACC-04` | Immutable Reversal | Vouchers are never deleted or mutated; reversal generates a counter-voucher referencing `[REVERSAL of <VCH_ID>]`. | `spec_requirements.md` §4 F12 |
| `INV-ACC-05` | Indian Number Formatting | Grouping follows Indian lakhs and crores: `₹ 1,00,000.00` and `₹ 10,50,750.50`. | `spec_requirements.md` §2.3.3 |

---

## 3. Test Suite Structure & Tiers

The test suite is organized into modular files located in `.agents/teamwork/tests/`:

```
.agents/teamwork/tests/
├── test_helpers.mjs                  # Core test runner utilities, assertions, accounting engine, schema validators
├── tier1_feature_coverage.test.mjs   # Tier 1: Feature Coverage (>=5 test cases per feature F1-F11)
├── tier2_boundary_corner.test.mjs    # Tier 2: Boundary & Corner Cases (>=5 test cases per feature F1-F11)
├── tier3_cross_feature.test.mjs      # Tier 3: Cross-Feature Combinations (Pairwise workflows)
├── tier4_accounting_scenarios.test.mjs # Tier 4: Real-World Commercial Accounting Scenarios
└── run_all_tests.mjs                 # Master test orchestrator & reporting CLI
```

### Tier Matrix

| Tier | Name | Target Coverage | Min Tests | Purpose |
|---|---|---|---|---|
| **Tier 1** | Feature Coverage | F1 to F11 (All features in `PROJECT.md`) | 55+ | Verify baseline functionality and invariants for each discrete feature. |
| **Tier 2** | Boundary & Corner Cases | F1 to F11 edge inputs | 55+ | Stress test extreme amounts, negative values, long remarks, special characters, network dropouts, missing keys. |
| **Tier 3** | Cross-Feature Combinations | Pairwise feature interaction | 10+ | Verify multi-step end-to-end flows: Auth -> Add Voucher -> Offline Cache -> Sync -> Reboot. |
| **Tier 4** | Real-World Accounting Scenarios | Multi-voucher commercial workloads | 5+ | Execute realistic business day books (Retail, Fleet, Wholesale Contra, Reversals, High-Volume Stress). |

---

## 4. How to Run the Tests

### Master Test Runner (All Tiers)
From the project root:
```bash
node .agents/teamwork/tests/run_all_tests.mjs
```

### Running Individual Tiers
```bash
# Tier 1: Feature Coverage only
node .agents/teamwork/tests/tier1_feature_coverage.test.mjs

# Tier 2: Boundary & Corner Cases only
node .agents/teamwork/tests/tier2_boundary_corner.test.mjs

# Tier 3: Cross-Feature Combinations only
node .agents/teamwork/tests/tier3_cross_feature.test.mjs

# Tier 4: Real-World Accounting Scenarios only
node .agents/teamwork/tests/tier4_accounting_scenarios.test.mjs
```

### Running via Native Node Test Runner
```bash
node --test .agents/teamwork/tests/*.test.mjs
```

---

## 5. Pass / Fail Semantics

- **PASS (`exit code 0`)**: All assertions succeed, zero invariant violations detected, all accounting reconciliations balance to 0 paise difference, and all schema constraints match `firestore.rules`.
- **FAIL / DEFECT ESCALATION (`exit code 1`)**: Any test failure produces a structured failure report containing:
  1. Test ID and Description.
  2. Invariant ID (`INV-*`).
  3. Affected file path and line number (if static scan).
  4. Expected value vs Actual value.
  5. Actionable escalation guidance for the responsible implementation milestone (M1 to M4).
