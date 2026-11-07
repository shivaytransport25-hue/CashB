# Project: Cash Book Suite Visual Styling & Google Sign-In Overhaul

## Architecture
The Cash Book Pro suite consists of three interconnected client applications and a centralized Firebase backend:
1. **Windows Desktop Software**:
   - Runtime: .NET 10.0 host running Microsoft Edge WebView2.
   - UI / Logic: `E:\CashBookRelease\app\www\index.html` (5,573 lines) packaged into Inno Setup installer `CashBook_Setup_v2.0.0.exe` and installed to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`.
   - Auth & Cloud: Google OAuth 2.0 Web Client, Firebase Auth, Firestore client SDK.
2. **Android Native App**:
   - Location: `G:\My Drive\project\Cash Book\app`
   - UI: Jetpack Compose with Material 3 styling.
   - Auth & Cloud: Google Identity CredentialManager, FirebaseAuth, FirebaseFirestore.
3. **Flutter Mobile App**:
   - Location: `G:\My Drive\project\Cash Book\cash_book_system`
   - UI: Flutter ThemeData with custom ledger widgets and tables.
   - Auth & Cloud: `google_sign_in`, `firebase_auth`, `cloud_firestore`.
4. **Cloud Backend**:
   - Firebase Project: `gen-lang-client-0037390170`
   - Security Rules: `firestore.rules` (enforcing strict owner match `request.auth.uid == userId` and 12-key whitelist for `/users/{userId}/transactions/{txId}`).

## Code Layout
- Desktop Staging: `E:\CashBookRelease\app\www\index.html` (and CSS/JS files in that folder)
- Desktop Installed Target: `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`
- Android Native: `G:\My Drive\project\Cash Book\app\src\main\java\com\aistudio\cashbook\shvycb\`
- Flutter System: `G:\My Drive\project\Cash Book\cash_book_system\lib\`
- Specifications: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\spec_requirements.md`
- Visual Reference: `C:\Users\WIN\.gemini\antigravity\brain\1407bb07-e95c-496b-ab38-d1549f649037\.user_uploaded\media_1793958737050_b0f8f00b.jpg`
- Test Infrastructure: `g:\My Drive\project\Cash Book\.agents\teamwork\tests\`

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Desktop Visual Design Overhaul | Remove AI blur halos (`pulseGlow`), neon glows, saturated gradients; implement enterprise fintech daylight/dark palette | M1 | Survey (Explorer 1 & Spec Miner 3) |
| F2 | Desktop Tabular Ledger & Contra Format | High-contrast tabular numbers (`font-variant-numeric: tabular-nums`), clean borders, distinct Contra presentation | M1 | Survey (Explorer 1 & Spec Miner 3) |
| F3 | Desktop Real Google Sign-In | Eliminate fake user fallback (`usr_google_rajesh_patel`); implement real Google OAuth in WebView2 via loopback / origin fix | M2 | Survey (Explorer 1 & Spec Miner 3) |
| F4 | Desktop Session Persistence | Durable session persistence across app restarts without requiring re-login | M2 | Survey (Explorer 1 & Spec Miner 3) |
| F5 | Desktop Bidirectional Firestore Sync | Implement real voucher/party subcollection synchronization compliant with `firestore.rules` 12-key whitelist | M2 | Survey (Explorer 1 & Spec Miner 3) |
| F6 | Mobile Visual Design Overhaul | Remove 3-color saturated gradients, neon glows, breathing animations, and toy bouncy physics in Flutter & Android | M3 | Survey (Explorer 2 & Spec Miner 3) |
| F7 | Mobile Tabular Ledger & High Contrast | Tabular monospaced figures, clean card perimeters, professional credit/debit indicators in Flutter & Android | M3 | Survey (Explorer 2 & Spec Miner 3) |
| F8 | Flutter Real Google Sign-In | Install `google_sign_in`, initialize Firebase, remove mock dummy user fallback (`usr_google_default`) | M4 | Survey (Explorer 2 & Spec Miner 3) |
| F9 | Android CredentialManager Fix | Fix catch block to eliminate silent fallback to anonymous user authentication | M4 | Survey (Explorer 2 & Spec Miner 3) |
| F10 | Mobile Session Persistence | Secure session persistence across app restarts on Android and Flutter | M4 | Survey (Explorer 2 & Spec Miner 3) |
| F11 | Mobile Firestore Sync & Schema Alignment | Align database targeting, ensure 12-key whitelist compliance, enable bidirectional voucher synchronization | M4 | Survey (Explorer 2 & Spec Miner 3) |
| F12 | Full E2E Verification & Adversarial Hardening | Comprehensive test suite (Tiers 1-4) followed by white-box adversarial testing (Tier 5) | M5 | Survey / Orchestrator |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Desktop Visual Styling & Ledger Overhaul | `E:\CashBookRelease\app\www\index.html` UI/CSS overhaul per fintech design tokens | none | DONE |
| M2 | Desktop Google Sign-In & Firestore Sync | `E:\CashBookRelease\app\www\index.html` real Google auth, session persistence, bidirectional sync | M1 | PLANNED |
| M3 | Mobile Visual Styling & Ledger Overhaul | Flutter & Android native UI themes, colors, and tabular ledger formatting | none | IN_PROGRESS |
| M4 | Mobile Google Sign-In & Firestore Sync | Flutter & Android Google Sign-In fix, session persistence, and Firestore sync | M3 | PLANNED |
| M5 | Final Verification & Hardening | 100% pass of E2E test suite (Tiers 1-4) + Adversarial hardening (Tier 5) | M1, M2, M3, M4 | PLANNED |

## Interface Contracts
### Client UI ↔ Auth & Session State
- `window.cashbookAuth = { currentUser, isAuthenticated, idToken, sessionExpiresAt }`
- Event `cashbook:auth-changed` fired on sign-in, sign-out, or session restore.
- Mobile: Stream/StateFlow emitting `AuthState.Authenticated(user)` or `AuthState.Unauthenticated`.

### Client ↔ Firestore Document Schema
- Collection: `/users/{userId}/transactions/{transactionId}`
- Enforced 12-key whitelist:
  `['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`
- Constraints:
  - `userId`: String, strictly matches `request.auth.uid`
  - `type`: String, `'CASH_IN'` | `'CASH_OUT'` | `'CONTRA'`
  - `amount`: Number > 0
  - `paymentMode`: String, `'CASH'` | `'BANK'` | `'ONLINE'`
- Collection: `/users/{userId}/cashbooks/{bookId}`
- Collection: `/users/{userId}/parties/{partyId}`

### Google OAuth Configuration
- Web Client ID: `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`
- Android Client ID: `266747615266-25209tatgef75impctcu805va2omvogq.apps.googleusercontent.com`
- Firebase Project ID: `gen-lang-client-0037390170`
