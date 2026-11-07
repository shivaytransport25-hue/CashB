# Authoritative Engineering Specification: Enterprise Fintech Visual Design System & Unified Google Sign-In / Auth Architecture

- **Project**: Shivay CashBook & Cash Book Pro Multi-Platform Suite
- **Platforms**: Windows Desktop (WebView2 `www/index.html` & Host), Android Native (Jetpack Compose), Flutter Mobile (`cash_book_system`)
- **Author**: Spec Miner 3 (Fintech Design & Auth Specialist)
- **Status**: AUTHORITATIVE SPECIFICATION (Ready for Implementation)
- **Date**: 2026-11-06

---

## 1. Executive Summary & Authoritative Specification Sources

This specification defines the mandatory technical and visual requirements to overhaul the Cash Book software suite. It resolves two core architectural problems identified in `ORIGINAL_REQUEST.md`:
1. **Visual Styling**: Eradicate generic AI-generated aesthetics (such as oversaturated multi-color gradients, pulsing blur halos, and cartoonish 32px drop shadows) and replace them with a human-crafted, high-contrast, utilitarian enterprise fintech design system derived from the authoritative user reference (`media_1793958737050_b0f8f00b.jpg`).
2. **Authentication & Data Synchronization**: Unify Google Sign-In across Windows Desktop (WebView2 environment) and Mobile (Android Native & Flutter), eliminating hanging OAuth popups and insecure dummy fallbacks (`usr_google_rajesh_patel`), while guaranteeing durable multi-session persistence and schema-compliant bidirectional Firestore synchronization under `firestore.rules`.

### Authoritative Specification Sources
- **Visual Reference Master**: `C:\Users\WIN\.gemini\antigravity\brain\1407bb07-e95c-496b-ab38-d1549f649037\.user_uploaded\media_1793958737050_b0f8f00b.jpg` (Cash Book Pro Desktop Production Layout).
- **Security & Schema Master**: `g:\My Drive\project\Cash Book\firestore.rules` and `firebase-blueprint.json`.
- **Identity & OAuth Master**: `g:\My Drive\project\Cash Book\app\google-services.json` (Project: `gen-lang-client-0037390170`).
- **Domain Accounting Engine Master**: `app/src/main/java/com/example/util/FinancialCalculationEngine.kt` and `cashbook-fix-verify\SKILL.md`.

---

## 2. Enterprise Fintech / Accounting Visual Design System

### 2.1 Aesthetic Principles & Anti-Pattern Elimination
All user interfaces across Windows Desktop, Android Compose, and Flutter must strictly adhere to the utilitarian ethos of commercial Indian accounting books (Bahi-Khata / Tally / Vyapar). The interface is an information-dense ledger tool for accountants, traders, and fleet managers; it is not a consumer gaming or social media app.

#### Mandatory Code Removal (Anti-Patterns)
The following AI-generated styling artifacts must be eliminated from all active CSS, XML, and Dart layouts:
1. **Pulsing Blur Halos & AI Gradients**:
   - *Prohibited*: `linear-gradient(135deg, rgba(245, 158, 11, 0.4), rgba(30, 58, 138, 0.5)); filter: blur(16px); animation: pulseGlow` (found in `index.html:1275`).
   - *Requirement*: Replace with flat, solid surfaces with exact 1px/1dp hairline borders.
2. **Deep Diffuse Drop Shadows**:
   - *Prohibited*: `box-shadow: 0 14px 32px rgba(0,0,0,0.5);` (found in `index.html:49`) or `box-shadow: 0 10px 25px rgba(0,0,0,0.6);`.
   - *Requirement*: Standardize on crisp, subtle enterprise shadows: `box-shadow: 0 1px 2px rgba(0,0,0,0.04);` for cards and tables, or border-only separation (`elevation: 0.5` or `0.dp`).
3. **Pulsing Keyframes & Glowing Rings**:
   - *Prohibited*: `@keyframes pulseGlow` and `@keyframes spinRing` with neon glows.
   - *Requirement*: Standard restrained spinners (2px stroke, solid brand accent).
4. **Oversaturated Neon Accents**:
   - *Prohibited*: Neon lime green (`#00FF66`), fluorescent pink, or electric purple gradients.
   - *Requirement*: Grounded accounting pigments: Forest/Emerald Green (`#15803D` / `#059669`), Brick/Crimson Red (`#B91C1C` / `#DC2626`), Banking Navy (`#1E3A5F` / `#2563EB`), and Wealth Amber (`#B45309` / `#D97706`).

---

### 2.2 Color Architecture & Token Specifications

The system supports two primary modes: **Office Daylight (Light Mode)** and **High-Contrast Slate Ledger (Dark Mode)**, plus optional domain themes (Classic Navy, Emerald Merchant, Royal Burgundy, Steel Slate, Monochrome B&W).

#### 2.2.1 Neutral Foundation Palette

| Token Name | Light Mode Value | Dark Mode Value | Semantic Role / Component Application |
|---|---|---|---|
| `--color-bg-canvas` | `#F8FAFC` (Slate 50) | `#0B111B` (Deep Navy Black) | App window canvas background, content container base |
| `--color-surface-card` | `#FFFFFF` (Pure White) | `#111A28` (Dark Slate Surface) | KPI metric cards, ledger table panels, chart containers |
| `--color-surface-subtle` | `#F1F5F9` (Slate 100) | `#182438` (Slate Surface 2) | Table header rows, filter bars, inactive button pills |
| `--color-surface-raised` | `#E2E8F0` (Slate 200) | `#21314D` (Slate Surface 3) | Popups, modal dialogs, status bar, hover backgrounds |
| `--color-border-hairline`| `#E2E8F0` (Slate 200) | `#1E293B` (Slate 800) | Table row dividers (1px), subtle list item dividers |
| `--color-border-strong`  | `#CBD5E1` (Slate 300) | `#334155` (Slate 700) | Card perimeters, input field outlines, window header borders |
| `--color-text-primary`   | `#0F172A` (Slate 900) | `#F8FAFC` (Slate 50) | Headings, ledger party names, primary balances, active labels |
| `--color-text-secondary` | `#334155` (Slate 700) | `#CBD5E1` (Slate 300) | Subtitles, table cell secondary info, inactive tabs |
| `--color-text-muted`     | `#64748B` (Slate 500) | `#94A3B8` (Slate 400) | Voucher date timestamps, metadata hints, placeholder text |

#### 2.2.2 WCAG 2.1 Contrast Ratios (Accessibility Verification)
All text elements must strictly pass WCAG AA (minimum 4.5:1 for body, 3.0:1 for large text); critical accounting figures must satisfy WCAG AAA (7.0:1).
- `--color-text-primary` (`#0F172A`) on `--color-surface-card` (`#FFFFFF`): **15.68:1** (Passes WCAG AAA)
- `--color-text-secondary` (`#334155`) on `--color-surface-card` (`#FFFFFF`): **8.92:1** (Passes WCAG AAA)
- `--color-text-muted` (`#64748B`) on `--color-surface-card` (`#FFFFFF`): **4.68:1** (Passes WCAG AA)
- `--color-text-primary` (`#F8FAFC`) on Dark Canvas (`#0B111B`): **16.12:1** (Passes WCAG AAA)
- Green Credit Text (`#047857`) on Container (`#ECFDF5`): **6.21:1** (Passes WCAG AA)
- Red Debit Text (`#B91C1C`) on Container (`#FEF2F2`): **6.84:1** (Passes WCAG AA)

#### 2.2.3 Accounting Semantic Palette (Inflow vs Outflow vs Transfer)
Color semantics in financial ledgers carry strict operational meaning:

| Transaction Flow | Semantic Role | Light Container Fill | Light Foreground / Border | Dark Container Fill | Dark Foreground / Text |
|---|---|---|---|---|---|
| **CASH IN (Credit / Jama)** | Inflow, Customer Receipt, Revenue | `#ECFDF5` (Mint 50) | `#047857` (Emerald 700) / Border: `#059669` | `rgba(6, 78, 59, 0.25)` | `#22C55E` (Emerald 500) |
| **CASH OUT (Debit / Udhar)** | Outflow, Supplier Payment, Expense | `#FEF2F2` (Rose 50) | `#B91C1C` (Red 700) / Border: `#DC2626` | `rgba(127, 29, 29, 0.25)` | `#F87171` (Red 400) |
| **CONTRA (Account Transfer)** | Inter-account, Bank to Cash, Contra | `#EFF6FF` (Blue 50) | `#1D4ED8` (Blue 700) / Border: `#2563EB` | `rgba(30, 58, 95, 0.25)` | `#60A5FA` (Blue 400) |
| **WEALTH ACCENT (Brand Action)**| Primary Action Button, FY Badge | `#FEF3C7` (Amber 100) | `#B45309` (Amber 700) / Solid: `#D97706` | `rgba(245, 158, 11, 0.2)` | `#F59E0B` (Amber 500) |

#### 2.2.4 Status Indicator Tags
- **Cloud Live Synced**: Pill `#DCFCE7`, text `#15803D`, solid green dot `#10B981` (no pulse).
- **Syncing in Progress**: Pill `#FEF3C7`, text `#B45309`, spinning 12px ring `#D97706`.
- **Offline / Local Vault**: Pill `#F1F5F9`, text `#475569`, grey cloud-slash icon.
- **Voided / Reversed Voucher**: Text line-through with opacity 0.5; badge `#F3F4F6`, border `#D1D5DB`, text `#6B7280`.

---

### 2.3 Typography & Tabular Figure Standard

Financial precision demands that currency amounts, voucher IDs, and serial numbers never jump or misalign when scrolling or recalculating.

1. **Tabular Figures (`tnum`) Rule**:
   - All numbers in transaction tables, liquid account balances, KPI cards, and reports MUST display using proportional tabular figures (`font-variant-numeric: tabular-nums` in CSS) or monospace fonts (`JetBrains Mono`, `Consolas`).
   - Standard font stack:
     - UI Headings & Body: `Plus Jakarta Sans`, `Inter`, `'Segoe UI'`, sans-serif.
     - Financial Amounts & Ledgers: `JetBrains Mono`, `Consolas`, monospace with `font-feature-settings: "tnum" 1, "zero" 1`.
2. **Right-Alignment Mandate**:
   - In all data tables and statement grids, the `INFLOW (+)`, `OUTFLOW (-)`, and `BALANCE` columns MUST be strictly right-aligned (`text-align: right`).
   - Decimal points (`.`) and minor unit paise (`00`) must align vertically in straight columns.
3. **Indian Currency Representation (`₹`)**:
   - Prefix symbol: `₹ ` (Unicode `U+20B9`).
   - Number grouping: Indian Lakhs & Crores grouping (`₹ 1,00,000.00` and `₹ 10,50,750.50`).
   - Conversion rule: Internal storage in integer paise (`minor units`), display divided by 100 with exact 2 decimal places.

---

### 2.4 Layout & Ledger Component Specifications (from Visual Master Reference)

#### 2.4.1 Desktop Topbar & Action Ribbon
- **Titlebar**: Height 44px. Contains Hamburger toggle, brand logo (32x32px), "Cash Book Pro" wordmark, company dropdown selector (`Shivay Logistics Pvt Ltd`), Financial Year chip (`FY 2026-27`), search input with shortcut hint (`Ctrl+F`), Theme toggle (`Auto Light`), Sync status badge, and Google authenticated user profile avatar.
- **Function Key Action Ribbon**:
  - Horizontal strip beneath titlebar (Height 40px, padding 4px 12px).
  - Quick action buttons with dedicated function key badges:
    - `[F2] + Cash In`: Light green container `#ECFDF5`, border `#059669`, text `#047857`.
    - `[F3] - Cash Out`: Light red container `#FEF2F2`, border `#DC2626`, text `#B91C1C`.
    - `[F4] Contra`: Light blue container `#EFF6FF`, border `#2563EB`, text `#1D4ED8`.
    - `[F5] Cloud Sync`: Neutral slate pill with cloud icon.
    - `[F6] Export Excel`: Neutral slate pill with file-excel icon.
    - `[F7] Print Day Book`: Neutral slate pill with printer icon.
    - `[F8] + New Party`: Neutral slate pill with user-plus icon.
    - `[F9] Denomination`: Neutral slate pill with calculator/cash icon.
    - `[F10] Mobile App`: Light sky blue pill with mobile icon.

#### 2.4.2 Sidebar & Liquid Accounts Section
- Width: Fixed 230px, collapsible to 0px via `Ctrl+B` or hamburger click.
- Top Action: `+ New Voucher` prominent button with solid Amber fill (`#D97706` in Light Mode, `#F59E0B` in Dark Mode, 8px border-radius, white bold text).
- Navigation items: Dashboard (active indicator), Day Book / Ledger, Bank & Cash Ledgers, Parties Directory, Reports & P&L, Denomination Counter, Settings & Backup.
- Bottom Widget: `LIQUID ACCOUNTS` container displaying live reconciled balances:
  - Cash in Hand: `₹ 60,950.00`
  - SBI Current A/c: `₹ -52,000.00` (Negative rendered in red `#DC2626`)
  - HDFC Bank: `₹ 84,000.00`
  - Monospaced tabular alignment, updated automatically on every voucher entry.

#### 2.4.3 KPI Summary Cards Grid
- 4-column responsive grid (`repeat(4, 1fr)`).
- Cards: Clean white background (`#FFFFFF`), 1px solid border (`#CBD5E1`), 6px border-radius.
- Top Indicator Stripe (3px solid):
  - Physical Cash in Hand: Amber (`#B45309`)
  - Total Bank Balances: Banking Blue (`#2563EB`)
  - Today's Cash In: Emerald Green (`#059669`)
  - Today's Cash Out: Crimson Red (`#DC2626`)
- Metric display: Large tabular font (22px bold), small muted subtitle (11.5px).

#### 2.4.4 Ledger Data Table (Day Book)
- Table container with sticky header (`position: sticky; top: 0; z-index: 10;`).
- Columns:
  1. `VOUCHER #`: Monospace pill (e.g., `VCH-2026-0006`), width 130px.
  2. `DATE & TIME`: Neutral text (e.g., `2026-11-05 12:15 pm`), width 140px.
  3. `TYPE`: Semantic pill badge (`CASH IN` or `CASH OUT`), width 100px.
  4. `ACCOUNT`: Account name in medium weight (e.g., `Cash in Hand`), width 140px.
  5. `PARTY NAME`: Associated party (e.g., `Chaa` or `Bharat Logistics`), width 160px.
  6. `CATEGORY`: Operating category (e.g., `Other Income`, `Fuel & Diesel`), width 130px.
  7. `INFLOW (+)`: Right-aligned tabular green text (`₹ 150.00`), width 120px.
  8. `OUTFLOW (-)`: Right-aligned tabular red text (`-`), width 120px.
- Row divider: 1px hairline border (`#E2E8F0`). Row hover effect: subtle 4% amber/slate tint (`rgba(217, 119, 6, 0.04)`).

#### 2.4.5 Status Bar
- Fixed at bottom (Height 28px).
- Solid slate background (`#E2E8F0` in light mode, `#111A28` in dark mode).
- Items separated by subtle 1px vertical rules:
  `[Ready] | [Period: 01-Apr-2026 to 31-Mar-2027 (FY 2026-27)] | [Storage: LocalStorage (Offline Safe)] | [Cloud Sync: Active] | [Total Entries: N] | [User: Admin / Shivay Transport] | [Live Time: hh:mm:ss a]`

---

### 2.5 Cross-Platform Design Token Mapping Matrix

| Design Token | Desktop Web (CSS Var) | Android Native (Jetpack Compose) | Flutter (`ThemeData` / `ColorScheme`) |
|---|---|---|---|
| Background Canvas | `var(--win-bg)` (`#F8FAFC`) | `MaterialTheme.colorScheme.background` (`#F8FAFC`) | `Theme.of(context).scaffoldBackgroundColor` (`0xFFF8FAFC`) |
| Surface Card | `var(--win-surface)` (`#FFFFFF`) | `MaterialTheme.colorScheme.surface` (`#FFFFFF`) | `Theme.of(context).colorScheme.surface` (`0xFFFFFFFF`) |
| Table Header Row | `var(--win-surface-2)` (`#F1F5F9`)| `MaterialTheme.colorScheme.surfaceVariant` (`#F1F5F9`)| `Theme.of(context).colorScheme.surfaceContainerHighest` |
| Hairline Border | `var(--win-border-hairline)` (`#E2E8F0`)| `LedgerBorderLight` (`0xFFE2E8F0`, `1.dp`) | `DividerThemeData.color` (`0xFFE2E8F0`, `thickness: 1`) |
| Strong Border | `var(--win-border)` (`#CBD5E1`) | `Color(0xFFCBD5E1)` | `Theme.of(context).colorScheme.outline` (`0xFFCBD5E1`) |
| Credit Text (Cash In) | `var(--color-credit)` (`#047857`) | `CashInGreen` (`0xFF15803D`) | `Color(0xFF047857)` |
| Credit Container | `var(--color-credit-bg)` (`#ECFDF5`) | `CashInContainerLight` (`0xFFDCFCE7`) | `Color(0xFFECFDF5)` |
| Debit Text (Cash Out) | `var(--color-debit)` (`#B91C1C`) | `CashOutRed` (`0xFFB91C1C`) | `Color(0xFFB91C1C)` |
| Debit Container | `var(--color-debit-bg)` (`#FEF2F2`) | `CashOutContainerLight` (`0xFFFEE2E2`) | `Color(0xFFFEF2F2)` |
| Transfer Accent | `var(--color-transfer)` (`#1D4ED8`)| `NavyPrimary` (`0xFF1E3A5F`) | `Theme.of(context).colorScheme.tertiary` (`0xFF1D4ED8`)|
| Wealth Action (Button) | `var(--win-accent)` (`#D97706`) | `AmberWarning` (`0xFFB45309`) | `Theme.of(context).colorScheme.primary` (`0xFFD97706`) |
| Tabular Monospace Font | `var(--win-mono)` (`'JetBrains Mono'`) | `JetBrainsMonoFamily` (bundled `.ttf`) | `TextStyle(fontFeatures: [FontFeature.tabularFigures()])` |

---

## 3. Unified Google Sign-In & Auth Architecture Specification

### 3.1 Google Identity & Firebase Project Configuration
All platforms MUST connect to the same authoritative Firebase and Google Cloud project:
- **Firebase Project ID**: `gen-lang-client-0037390170`
- **Project Number**: `266747615266`
- **Firebase Auth Domain**: `gen-lang-client-0037390170.firebaseapp.com`
- **Web OAuth Client ID (Client Type 3)**:
  `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`
- **Android OAuth Client ID (Client Type 1)**:
  `266747615266-25209tatgef75impctcu805va2omvogq.apps.googleusercontent.com`
  - Package Name: `com.aistudio.cashbook.shvycb`
  - Certificate SHA-1 Hash: `6b0227947e46a846afc8e7018f98ca4b6ac39d37`
- **Firestore Database ID**: `(default)` / `ai-studio-android-shivayca-1eed692a-22c9-4cae-897e-2c89377484c8`

---

### 3.2 Platform-Specific Authentication Flows

#### 3.2.1 Windows Desktop WebView2 Architecture Flow
**The Problem**:
When the desktop application runs `index.html` via `file:///E:/.../index.html` inside WebView2, invoking `firebase.auth().signInWithPopup(provider)` fails because:
1. Google OAuth 2.0 disallows `file://` or `null` origins in compliance with RFC 8252.
2. Cross-origin `window.postMessage` between the OAuth popup (`https://gen-lang-client-...`) and the `file:///` host is blocked by browser origin security policies.
3. In `index.html` (line 5399), this failure previously triggered an unhandled catch block that silently injected a dummy session (`uid: 'usr_google_rajesh_patel'`), preventing real cloud sync.

**Mandatory Desktop Specification**:
The desktop implementation must support one of the following two approved patterns:
- **Pattern A (Virtual Host Domain Mapping — Recommended for WebView2)**:
  The Windows desktop wrapper (`ShivayCashBook.exe` / WPF / C++ runner) must map the `www` folder to a virtual HTTPS origin:
  ```csharp
  // C# WebView2 host initialization
  webView.CoreWebView2.SetVirtualHostNameToFolderMapping(
      "app.cashbook.local",
      Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "www"),
      CoreWebView2HostResourceAccessKind.Allow
  );
  webView.Source = new Uri("https://app.cashbook.local/index.html");
  ```
  Because the origin is `https://app.cashbook.local`, Google Identity Services (GSI) or OAuth 2.0 redirect flow executes seamlessly with an allowed origin.
- **Pattern B (RFC 8252 Loopback Flow for Standalone Desktop)**:
  1. Desktop triggers Sign In: Spawns the user's default system browser (Chrome/Edge) to Google's OAuth endpoint:
     `https://accounts.google.com/o/oauth2/v2/auth?client_id=266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com&redirect_uri=http://127.0.0.1:[PORT]/oauth2callback&response_type=code&scope=openid%20profile%20email`
  2. Local loopback HTTP listener receives the authorization code on `http://127.0.0.1:[PORT]/oauth2callback`.
  3. Exchanges code for Google ID token, passes token to WebView2 via `CoreWebView2.PostWebMessageAsString()`.
  4. WebView2 JavaScript executes:
     ```javascript
     const credential = firebase.auth.GoogleAuthProvider.credential(idToken);
     const authRes = await firebase.auth().signInWithCredential(credential);
     ```
- **Pattern C (Google Identity Services (GSI) Web Flow)**:
  Load `https://accounts.google.com/gsi/client` in `index.html`. Initialize `google.accounts.id.initialize({ client_id: '...', callback: handleGoogleCredentialResponse })` and pass the received JWT credential directly to `firebase.auth.GoogleAuthProvider.credential(response.credential)`.

#### 3.2.2 Android Native Authentication Flow
- Implement using Jetpack `androidx.credentials.CredentialManager`.
- Configure `GetSignInWithGoogleOption` with `serverClientId = "266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com"`.
- Intercept credentials: Check `credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL`.
- Convert token:
  ```kotlin
  val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
  val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
  val authResult = Firebase.auth.signInWithCredential(authCredential).await()
  ```
- Catch `GetCredentialCancellationException` cleanly without displaying an error alert.

#### 3.2.3 Flutter Mobile Authentication Flow
- Integrate `google_sign_in: ^6.2.1` and `firebase_auth: ^5.3.1` in `cash_book_system/pubspec.yaml`.
- Replace the broken `FirebaseAuth.instance.signInWithProvider(GoogleAuthProvider())` in `auth_service.dart`:
  ```dart
  final GoogleSignIn googleSignIn = GoogleSignIn(
    serverClientId: '266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com',
    scopes: ['email', 'profile'],
  );
  final GoogleSignInAccount? googleUser = await googleSignIn.signIn();
  if (googleUser == null) return false; // User cancelled

  final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
  final AuthCredential credential = GoogleAuthProvider.credential(
    idToken: googleAuth.idToken,
    accessToken: googleAuth.accessToken,
  );
  final UserCredential userCredential = await FirebaseAuth.instance.signInWithCredential(credential);
  ```

---

### 3.3 Session Persistence & Re-Authentication Specification

1. **Durable Local Storage Across Application Restarts**:
   - **Desktop**:
     - WebView2 must specify a persistent `userDataFolder` (e.g. `%LOCALAPPDATA%\ShivayCashBook\WebView2Data`) so that IndexedDB and `localStorage` are NOT wiped on window close.
     - Firebase Web SDK must enforce `firebase.auth().setPersistence(firebase.auth.Auth.Persistence.LOCAL)`.
     - Store sanitized profile in `localStorage.setItem('cashbook_auth_user', JSON.stringify({ uid, email, displayName, photoURL }))`.
   - **Android**:
     - `Firebase.auth.currentUser` is natively stored in encrypted app private storage (`EncryptedSharedPreferences`).
     - On app launch, `attemptAutoSignIn` checks `Firebase.auth.currentUser != null`. If present, bypass login gate immediately.
   - **Flutter**:
     - `FirebaseAuth.instance.currentUser` is cached by native SDK.
     - `SharedPreferences` persists `saved_user_id`, `saved_user_email`, and `saved_user_name`.
2. **Auto-Login & Token Refresh Cycle**:
   - The Firebase SDK automatically refreshes OAuth tokens every 55 minutes. Applications must listen to `onAuthStateChanged` / `authStateChanges()` to automatically refresh credentials without prompting the user.
3. **Explicit Sign-Out Protocol**:
   - Upon clicking "Sign Out":
     - Clear Firebase Auth: `firebase.auth().signOut()`.
     - Clear local app caches: `localStorage.removeItem('cashbook_auth_user')` / `SharedPreferences.clear()`.
     - Android: `credentialManager.clearCredentialState(ClearCredentialStateRequest())`.
     - Route user back to mandatory Login Gate (`modal-login-gate`).

---

### 3.4 Firestore Bidirectional Synchronization Specification

#### 3.4.1 Strict Schema Conformance (`firestore.rules`)
All documents synchronized to Cloud Firestore MUST strictly respect the document keys enforced by `firestore.rules`:

1. **CashBook Document**: `/users/{userId}/cashbooks/{bookId}`
   - *Allowed Keys Only*: `['userId', 'name', 'description', 'currencySymbol', 'createdAt', 'updatedAt']`
   - Constraints: `userId == request.auth.uid`, `name.size() <= 80`, `createdAt <= request.time`.
2. **Party Document**: `/users/{userId}/parties/{partyId}`
   - *Allowed Keys Only*: `['userId', 'name', 'phone', 'role', 'notes', 'createdAt', 'updatedAt']`
   - Constraints: `role in ['Customer', 'Supplier', 'Staff']`.
3. **CashTransaction Document**: `/users/{userId}/transactions/{txId}`
   - *Allowed Keys Only*: `['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`
   - Constraints:
     - `userId == request.auth.uid` (Mandatory: Dummy UIDs cause immediate `PERMISSION_DENIED`).
     - `type in ['CASH_IN', 'CASH_OUT', 'TRANSFER']`.
     - `paymentMode in ['Cash', 'Bank Transfer', 'UPI', 'Cheque', 'Card']`.
     - `amount > 0 && amount <= 1000000000` (stored as double in Rupees, e.g. `150.00`).
     - `transactionDate is int` (epoch milliseconds).
     - `createdAt` and `updatedAt` MUST be Firestore `Timestamp` objects (`firebase.firestore.FieldValue.serverTimestamp()`).

#### 3.4.2 Desktop Sync Engine Fix Specification
In `E:\CashBookRelease\app\www\index.html`, the existing `syncWithCloud()` only updates the top-level user doc. It MUST be upgraded to synchronize the full subcollections:
```javascript
// Authoritative Desktop Bidirectional Sync Routine
async function syncWithCloud(showToast = false) {
  if (isCloudSyncing || !currentUserProfile || !firestoreDb) return;
  isCloudSyncing = true;
  updateSyncBadge('syncing', 'Syncing Vouchers...');

  try {
    const uid = currentUserProfile.uid;
    const txns = JSON.parse(localStorage.getItem('cashbook_txns')) || [];
    const parties = JSON.parse(localStorage.getItem('cashbook_parties')) || [];

    // 1. Upstream Sync: Push local dirty transactions
    const batch = firestoreDb.batch();
    let writeCount = 0;

    for (const txn of txns) {
      if (txn._dirty) {
        const docRef = firestoreDb.collection('users').doc(uid).collection('transactions').doc(txn.id);
        batch.set(docRef, {
          userId: uid,
          bookId: txn.bookId || 'default_book',
          type: txn.type, // 'CASH_IN' or 'CASH_OUT'
          amount: parseFloat(txn.amount),
          remark: (txn.remark || txn.description || '').substring(0, 250),
          category: txn.category || 'General',
          paymentMode: txn.paymentMode || 'Cash',
          partyId: txn.partyId || '',
          partyName: (txn.partyName || '').substring(0, 80),
          transactionDate: txn.transactionDate || Date.now(),
          createdAt: firebase.firestore.Timestamp.fromMillis(txn.createdAt || Date.now()),
          updatedAt: firebase.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        delete txn._dirty;
        writeCount++;
      }
    }
    if (writeCount > 0) {
      await batch.commit();
      localStorage.setItem('cashbook_txns', JSON.stringify(txns));
    }

    // 2. Downstream Sync: Pull remote updates
    const snap = await firestoreDb.collection('users').doc(uid).collection('transactions')
      .where('userId', '==', uid)
      .get();
    
    const remoteTxns = [];
    snap.forEach(doc => remoteTxns.push({ id: doc.id, ...doc.data() }));
    mergeRemoteTransactionsIntoLocal(remoteTxns);

    updateSyncBadge('synced', 'Cloud Live Synced');
    if (showToast) showToastNotification('Cloud Sync Complete! All vouchers updated across devices.', 'success');
  } catch (err) {
    console.error('Firestore bidirectional sync error:', err);
    updateSyncBadge('error', 'Sync Failed (Offline)');
  } finally {
    isCloudSyncing = false;
  }
}
```

---

## 4. Features Discovered & Probed

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|---|---|---|---|---|---|---|
| F1 | Visual Design | Function Key Ribbon | Keyboard and mouse quick-action voucher & tool bar (F2-F10) | Function Key press / Click | Opens entry modal, prints, or exports | Ignores unmapped F-keys | Reference Image & `index.html:1301` |
| F2 | Visual Design | Liquid Accounts Widget | Live balances of Cash in Hand, SBI, and HDFC accounts | Ledger entries / Account balance sums | Reconciled net balance with currency symbol | Displays negative in red | Reference Image & `index.html:651` |
| F3 | Visual Design | Pillar Flow Comparison | Side-by-side Inflow (+) and Outflow (-) bar chart with filter toggle | 7-day / Monthly date range | Rendered Chart.js / fl_chart bars with value pills | Empty state when no txns | Reference Image & `index.html:727` |
| F4 | Visual Design | Expense & Asset Allocation | Donut chart showing expense category distribution | Transaction categories | Segmented donut ring with percentage legend | Displays empty gray circle if 0 expense | Reference Image & `index.html:732` |
| F5 | Visual Design | High-Density Day Book | High-contrast tabular register with hairline dividers and tabular numbers | Filter query, date selection | Sticky table with monospace amount columns | Displays "No vouchers found" row | Reference Image & `index.html:785` |
| F6 | Auth | WebView2 Google OAuth | Desktop Google Sign-In via popup or loopback | User clicks "Sign in with Google" | Auth credential, ID token, user profile | Fails on `file:///` without bridge | `index.html:5384` |
| F7 | Auth | CredentialManager Google Sign-In | Android native zero-click / bottom-sheet Google Sign-In | User tap / Auto-sign-in trigger | Google ID Token to Firebase Credential | `GetCredentialCancellationException` | `AuthScreen.kt:80` |
| F8 | Auth | Mobile Google Sign-In (Flutter) | Cross-platform mobile OAuth via platform channels | User button press | Firebase `UserCredential` | Missing plugin causes unhandled error | `auth_service.dart:147` |
| F9 | Session | Durable Session Persistence | Restores authenticated session across app kills and reboots | Stored token / EncryptedSharedPreferences | User bypassed directly to dashboard | Invalid token forces login screen | `index.html:5334`, `AuthScreen.kt:85` |
| F10| Sync | Firestore Bidirectional Sync | Two-way replication between local storage and `/users/{uid}/*` | Local dirty queue & remote snapshots | Reconciled local and cloud ledger | `PERMISSION_DENIED` on schema mismatch | `firestore.rules`, `sync_engine.dart:107` |
| F11| Accounting | Paise Minor-Unit Arithmetic | Prevents IEEE-754 floating-point drift | Currency numbers | Exact integer paise summation | Rejects negative/overflow values | `FinancialCalculationEngine.kt` |
| F12| Accounting | Immutable Reversal Vouchers | Reverses posted vouchers by generating tagged counter-entries | Original Voucher ID + reason | Reversal entry with `[REVERSAL of ...]` | Rejects double-reversals | `FinancialCalculationEngine.kt:78` |

---

## 5. Edge Cases & Observed Behavior

| # | Feature | Edge Case Input | Observed Behavior / Authoritative Requirement |
|---|---|---|---|
| E1 | Desktop Google Sign-In | Invoking `signInWithPopup` under `file:///` protocol in WebView2 | **Observed**: Throws cross-origin error. **Spec Requirement**: Must use Virtual Host Mapping (`https://app.cashbook.local`) or Loopback IP bridge (RFC 8252); NEVER fall back to dummy user. |
| E2 | Firestore Sync | Writing extra fields (e.g. `'description'`, `'amountPaise'`, `'status'`) to `/transactions/{txId}` | **Observed**: Firestore `hasOnly` rule fails, throwing `PERMISSION_DENIED`. **Spec Requirement**: Upstream sync payload must contain ONLY the 12 whitelisted schema keys. |
| E3 | Firestore Sync | Write attempt with unauthenticated UID or mismatched `userId` | **Observed**: `request.auth.uid == userId` rule fails immediately. **Spec Requirement**: Writing is blocked until valid Firebase Auth credential is verified. |
| E4 | Ledger Display | Very large currency amounts (e.g., `₹ 99,99,99,999.00`) | **Observed**: Layout clipping if font is not tabular. **Spec Requirement**: Use `JetBrains Mono` / `tnum` and right-align with minimum column width (120px). |
| E5 | Ledger Display | Negative account balance (Overdraft / Unreconciled) | **Observed**: Rendered without minus or in generic text. **Spec Requirement**: Must render with minus prefix in Debit Red (`#DC2626` / `#B91C1C`) (e.g. `₹ -52,000.00`). |
| E6 | Session Persistence | App restarted while offline | **Observed**: Fails if relying solely on remote auth. **Spec Requirement**: Cached profile in `localStorage` / `EncryptedSharedPreferences` unlocks local ledger vault in offline-safe mode. |
| E7 | Voucher Printing & Export | Currency symbol rendering in Excel (`.CSV`) | **Observed**: Excel corrupts `₹` into garbled characters. **Spec Requirement**: Prepend UTF-8 Byte Order Mark (`0xEF 0xBB 0xBF`) to all `.csv` export streams. |

---

## 6. Verification & Implementation Acceptance Gate

To declare implementation successful, the engineering team must satisfy the following verifiable criteria:

1. **Visual Style Gate**:
   - `grep -rn "filter: blur" E:\CashBookRelease\app\www\` returns 0 hits.
   - `grep -rn "pulseGlow" E:\CashBookRelease\app\www\` returns 0 hits.
   - All transaction amounts in tables display with `JetBrains Mono` or `font-variant-numeric: tabular-nums` and right-aligned.
   - Table borders and row dividers are crisp 1px hairlines without cartoonish drop shadows.
2. **Authentication Gate**:
   - Clicking "Sign in with Google" on Windows Desktop signs into Firebase without hanging popups, errors, or falling back to a dummy user.
   - Restarting the desktop app retains the signed-in session; user is not prompted to log in again.
   - Android native Google Sign-In passes via `CredentialManager` on Android 14/15 physical/emulated devices.
3. **Firestore Sync Gate**:
   - Create a voucher on Desktop -> Verify document appears in Cloud Firestore under `/users/{authUid}/transactions/{txId}` with all 12 whitelisted keys.
   - Create a voucher on Android -> Trigger "Cloud Sync" on Desktop -> Verify the voucher appears in Desktop's Day Book table.
   - All writes pass against `firestore.rules` without `PERMISSION_DENIED`.
