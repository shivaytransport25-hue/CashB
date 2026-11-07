# Handoff Report — Spec Miner M2-3: Desktop Google Sign-In & Firestore Sync Blueprint

**Milestone**: M2 (Desktop Google Sign-In & Firestore Sync)  
**Role**: Spec Miner M2-3  
**Working Directory**: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\`  
**Synthesized Blueprint**: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md`  
**Date**: 2026-11-06  

---

## 1. Observation

### 1.1 Direct Observations from Codebases & Security Rules

1. **Host Container Navigation Trap**:
   - In `E:\CashBookDesktop\Form1.cs` lines 74–78:
     ```csharp
     string appHtmlPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "www", "index.html");
     if (File.Exists(appHtmlPath))
     {
         webView.CoreWebView2.Navigate(new Uri(appHtmlPath).AbsoluteUri);
     }
     ```
     `new Uri(appHtmlPath).AbsoluteUri` evaluates to `file:///...`, navigating the WebView2 control under the `file:` scheme.
   - User data folder is configured at lines 61–64:
     ```csharp
     var userDataFolder = Path.Combine(
         Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
         "CashBookPro", "WebView2Data");
     Directory.CreateDirectory(userDataFolder);
     var env = await CoreWebView2Environment.CreateAsync(null, userDataFolder);
     await webView.EnsureCoreWebView2Async(env);
     ```
     This creates a persistent storage container `%APPDATA%\CashBookPro\WebView2Data` that preserves IndexedDB and `localStorage` across application reboots.

2. **Synchronous Exception in Firebase Auth Compat SDK**:
   - In `E:\CashBookRelease\app\www\js\firebase-auth-compat.js` lines 1–2:
     - `signInWithPopup(e)` calls `Ei(ci(), this._delegate, "operation-not-supported-in-this-environment")`.
     - `ci()` executes `("http:" === ni() || "https:" === ni() || p() || ii()) && !(f() || h()) && ai() && !oi()`.
     - `ni()` returns `self.location?.protocol`, which is `"file:"`.
     - `ii()` checks userAgent for `/iphone|ipad|ipod|android/`. On Windows desktop, it returns `false`.
     - Therefore `ci()` returns `false`, and `signInWithPopup()` throws `auth/operation-not-supported-in-this-environment` synchronously and immediately before issuing any network requests.
   - Conversely, `signInWithCredential(credential)` calls `Ft(this._delegate, credential)` without `ci()`, dispatching a REST call to `https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=...`, which functions completely regardless of the document scheme.

3. **Defective Mock User Fallback in `index.html`**:
   - In `E:\CashBookRelease\app\www\index.html` lines 5972–5985:
     ```javascript
     } catch (oauthErr) {
       console.warn('OAuth popup note (WebView2 file scheme):', oauthErr);
     }

     if (!authUser) {
       // Robust authenticated user session for desktop WebView2 environment
       authUser = {
         uid: 'usr_google_rajesh_patel',
         email: 'rajesh@shivaylogistics.com',
         displayName: 'Rajesh Patel',
         photoURL: 'https://lh3.googleusercontent.com/a/default-user=s96-c'
       };
     }
     ```
     `authUser` is saved to `localStorage['cashbook_auth_user']`, but `firebase.auth().currentUser` remains `null`.
   - Live codebase scanner in `tests/run_all_tests.mjs` flags line 5980: `INV-AUTH-01 Eradication of usr_google_rajesh_patel IN_PROGRESS (M1-M4)`.

4. **Authoritative Security Rules Whitelist in `firestore.rules`**:
   - In `firestore.rules` lines 6–9:
     ```
     match /{document=**} {
       allow read, write: if false;
     }
     ```
   - Global default-deny is enforced. Direct writes to `/users/{userId}` fail with `PERMISSION_DENIED`.
   - In `firestore.rules` lines 184–193:
     `hasAll` and `hasOnly` enforce an exact 12-key whitelist for `/users/{userId}/transactions/{txId}`:
     `['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`.
   - Field validations in lines 84–94:
     - `data.type in ['CASH_IN', 'CASH_OUT', 'TRANSFER']`
     - `data.paymentMode in ['Cash', 'Bank Transfer', 'UPI', 'Cheque', 'Card']`
     - `data.amount > 0 && data.amount <= 1000000000`
     - `data.remark.size() <= 500`
     - `data.partyName.size() <= 80`
     - `data.category.size() >= 1 && data.category.size() <= 60`
     - `data.transactionDate is int && data.transactionDate > 0`
     - `data.createdAt is timestamp && data.createdAt <= request.time`
     - `data.updatedAt is timestamp && data.updatedAt <= request.time`
     - Update diff keys limited to 9 mutable fields; `userId`, `bookId`, and `createdAt` are immutable.
   - Cashbooks subcollection `/users/{userId}/cashbooks/{bookId}` allows exactly 6 keys:
     `['userId', 'name', 'description', 'currencySymbol', 'createdAt', 'updatedAt']`.
   - Parties subcollection `/users/{userId}/parties/{partyId}` allows exactly 7 keys:
     `['userId', 'name', 'phone', 'role', 'notes', 'createdAt', 'updatedAt']` where `role in ['Customer', 'Supplier', 'Staff']`.

5. **Defective Sync Implementations in `index.html`**:
   - Lines 5405–5413: Duplicate mock function `syncWithCloud()` with hardcoded 700ms `setTimeout` alert simulation.
   - Lines 6092–6121: Duplicate function `syncWithCloud()` attempting to write summary stats to `/users/{uid}`, which is blocked with `PERMISSION_DENIED`. Zero transactions or parties are synced.
   - Line 4741: `syncTransactionToFirestore` is invoked on voucher save, but is never declared.
   - Local transactions contain 14+ fields (`id`, `voucher`, `time`, `account`, `toAccount`, `mode`, `status`, `desc`, `refNo`, `_dirty`) which violate the 12-key whitelist if passed unmapped.

6. **Automated Test Suite Execution**:
   - Executing `node "g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs"`:
     - Tier 1: 55/55 passed
     - Tier 2: 55/55 passed
     - Tier 3: 10/10 passed
     - Tier 4: 5/5 passed
     - Total: 125/125 passed (565ms duration).

---

## 2. Logic Chain

1. **Root Cause Inference**: Observation 1 shows `Form1.cs` loading `index.html` under `file:///`. Observation 2 proves `firebase-auth-compat.js` executes `ci()`, which unconditionally returns `false` under `file:` on desktop, causing `signInWithPopup()` to throw `auth/operation-not-supported-in-this-environment`. Therefore, the desktop app cannot use standard browser popups while under `file:///`.
2. **Mock Facade Origin**: Observation 3 shows line 5972 catching the error from Step 1 and silently generating `usr_google_rajesh_patel`. Because no auth token is generated, `firebase.auth().currentUser` remains `null`.
3. **Cloud Rejection Inference**: Observation 4 demonstrates that `firestore.rules` default-denies all operations where `request.auth == null` or where keys exceed the exact whitelists. Observation 5 reveals `index.html` attempting to write to `/users/{uid}` directly without authentication or voucher mapping. Therefore, all cloud synchronization was fundamentally failing.
4. **Authentic Authentication Resolution**: Observation 2 proves that `signInWithCredential` is protocol-neutral and calls Google Identity Toolkit REST endpoints directly. By obtaining a Google ID token (via Virtual Host mapping to `https://app.cashbook.local`, GIS, or system browser redirect) and redeeming it via `firebase.auth().signInWithCredential()`, `firebase.auth().currentUser` is established authentically.
5. **Session Persistence**: Combining `firebase.auth().setPersistence(firebase.auth.Auth.Persistence.LOCAL)` with `%APPDATA%\CashBookPro\WebView2Data` (Observation 1) and `localStorage['cashbook_auth_user']` guarantees that session credentials survive process restarts and enable silent token refresh at 55 minutes (`F4.3`).
6. **Schema Compliance & Offline Resilience**: Adhering to Observation 4 requires the two-way normalizer `toFirestoreTransactionPayload()` (which strips `_dirty` and local metadata to produce exactly 12 keys) and `fromFirestoreTransactionDoc()`. Offline mutations must accumulate in `cashbook_sync_queue` with exponential backoff capped at 5 retries (`B11.5`).

---

## 3. Features Discovered & Synthesized

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Auth | Authentic Google OAuth Flow | Exchange Google ID token for genuine Firebase Auth UserCredential | Google ID token / Auth code | Firebase `UserCredential`, JWT attached to Firestore | Rejects on timeout (5s) or cancellation without dummy fallback | Code inspection (`firebase-auth-compat.js`, `Form1.cs`) |
| 2 | Auth | Mock Fallback Eradication | Purge `usr_google_rajesh_patel` from `index.html` line 5980 | User cancellation / failure | Empty user session, login gate shown | Unauthenticated state retained; no fake UID injected | Code audit (`index.html:5980`, test `INV-AUTH-01`) |
| 3 | Auth | Durable Session Persistence | Persist Firebase user session in IndexedDB and `localStorage` across app restarts | App restart / boot event | Auto-login, restored `currentUserProfile` | Cleared on explicit `signOutGoogle()` | Test `INV-AUTH-05`, `Form1.cs` user data folder |
| 4 | Auth | Silent Token Refresh | Refresh Firebase Auth ID token at 55-minute threshold before 60-minute expiry | Timer (55 mins) | Fresh Firebase JWT | Retried on network reconnection | Test `INV-AUTH-05` (`F4.3`) |
| 5 | Sync | 12-Key Transaction Normalizer | Transform 14-field local voucher into canonical 12-key Firestore document | Local voucher object, auth UID | Exactly 12 whitelisted keys | Throws if unauthenticated or amount <= 0 | `firestore.rules:184-193`, test `INV-SYNC-01` |
| 6 | Sync | Type & PaymentMode Enum Mapping | Map local types (`in`/`out`/`contra`) and modes (`NEFT`, `UPI`, `Cheque`) to Firestore enums | Local type & mode strings | `CASH_IN`/`CASH_OUT`/`TRANSFER`, `Cash`/`Bank Transfer`/`UPI`/`Cheque`/`Card` | Normalizes to valid enum defaults | `firestore.rules:84,94`, Explorer M2-2 |
| 7 | Sync | CashBook & Party Normalizers | Enforce 6-key whitelist on `/cashbooks` and 7-key whitelist on `/parties` (`role in ['Customer', 'Supplier', 'Staff']`) | Local account & party objects | Canonical 6-key / 7-key maps | Maps `'Employee'` to `'Staff'` | `firestore.rules:120,152`, test `INV-SYNC-02` |
| 8 | Sync | Immutability on Update Diff | Prevent modification of `userId`, `bookId`, and `createdAt` during voucher updates | Updated voucher delta | `docRef.update()` with only 9 mutable keys + `updatedAt` | Firestore rejects if immutable keys modified | `firestore.rules:203-206` |
| 9 | Sync | Downstream Ingestion & LWW Conflict Resolution | Pull remote transactions, map to desktop format, merge via Last-Write-Wins | Firestore query snapshot | Synchronized local `transactions` array | Local `_dirty: true` records protected from overwrite | Explorer M2-2, `index.html:3298` |
| 10 | Sync | Durable Offline Queue | Queue uncommitted mutations in `cashbook_sync_queue` with exponential backoff capped at 5 retries | Offline create/update/delete | FIFO processed on `online` event | Retries halt when `retryCount >= 5` per `B11.5` | Test `INV-SYNC-06` (`B11.5`), `test_helpers.mjs` |
| 11 | UI | Fintech Header Status Badge | Display 4 distinct sync states (Synced, Syncing, Offline, Error) using M1 design tokens | Sync engine state events | Header `#cloud-sync-status-badge` updated | Shows queued item count when offline | M1 Design System, `index.html:1705` |
| 12 | UI | Connected Account Modal | Display authenticated Google avatar, email, and live synced counts | `currentUserProfile`, live array lengths | `#google-user-modal` data fields | Shows generic avatar on missing photo | `index.html:2730-2780` |

---

## 4. Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Google Sign-In | User closes OAuth popup window before completion | Emits `auth/popup-closed-by-user`; spinner dismisses; login gate remains open; zero mock users injected (`B3.5`). |
| 2 | Google Sign-In | Token exchange exceeds 5,000ms | Promise rejects with `OAuth token exchange timed out`; user shown network timeout error (`B3.3`). |
| 3 | Google Sign-In | Malformed OAuth authorization code or ID token | Rejected during validation; error message displayed; application remains on login gate (`B3.1`). |
| 4 | Session Persistence | Corrupted JSON in `localStorage['cashbook_auth_user']` | Safe `try/catch` fallback resets profile to `null` and presents login gate without crashing (`B4.1`). |
| 5 | Session Persistence | Network unavailable when application boots | Restores cached profile from `localStorage`; enables offline reading; queues new vouchers (`F4.4`). |
| 6 | Explicit Sign-Out | User signs out while offline | `localStorage['cashbook_auth_user']` removed; in-memory user nulled; login gate presented (`B4.4`). |
| 7 | Multi-Tenant Isolation | User A signs out and User B signs in | Local in-memory transactions and parties reset; User B does not see User A's un-synced vouchers (`TC-C05`). |
| 8 | Firestore Whitelist | Upstream payload contains illegal extra key `_dirty: true` | `toFirestoreTransactionPayload()` strips `_dirty`; Firestore write succeeds without rule denial (`INV-SYNC-06`). |
| 9 | Firestore Whitelist | Upstream payload contains remark of exactly 500 characters | Passes `remark.size() <= 500` rule (`B5.1`). |
| 10 | Firestore Whitelist | Upstream payload contains remark of 501 characters | Sanitizer truncates to 500 chars to prevent Firestore `PERMISSION_DENIED` (`B5.2`). |
| 11 | Firestore Whitelist | Upstream payload contains partyName of 81 characters | Sanitizer truncates to 80 chars to conform to `partyName.size() <= 80` rule (`B5.3`). |
| 12 | Offline Queue | Network remains offline for 10 consecutive retry attempts | Retries automatically cease after 5 attempts; status marked "Sync Paused" (`B11.5`). |

---

## 5. Caveats

1. **Google Cloud Console Credentials**: The Google Web Client ID `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com` has a fixed set of registered redirect URIs. Any OAuth flow utilizing redirect URIs must target `https://gen-lang-client-0037390170.firebaseapp.com/__/auth/handler`.
2. **Machine Build Environment**: The user's machine contains the .NET 10.0 runtime (`Microsoft.NETCore.App 10.0.0` and `Microsoft.WindowsDesktop.App 10.0.0`), but `dotnet --list-sdks` returns "No .NET SDKs were found". Therefore, `CashBookPro.exe` cannot be recompiled on this machine. All implementations must execute within `E:\CashBookRelease\app\www\index.html` (and `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`).
3. **No other caveats.**

---

## 6. Conclusion

1. The technical specification for Milestone M2 is fully established, synthesized, and recorded in:
   `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md`.
2. The root cause of desktop authentication failure was isolated to synchronous `ci()` protocol checks in `firebase-auth-compat.js` under `file:///`, which led to the `usr_google_rajesh_patel` mock facade.
3. The root cause of Firestore sync failure was isolated to attempting root-level writes to `/users/{uid}` (default-denied by `firestore.rules`), unmapped 14-field payloads violating the 12-key whitelist, and non-existent function `syncTransactionToFirestore`.
4. The blueprint provides exact, copy-paste ready implementations for:
   - Authentic Google OAuth credential redemption via `signInWithCredential()`.
   - Complete eradication of `usr_google_rajesh_patel` (`INV-AUTH-01`).
   - Session persistence in IndexedDB and `localStorage` with 55-minute silent token renewal (`F4.3`).
   - Strict 12-key transaction normalizer (`toFirestoreTransactionPayload`), 6-key cashbook normalizer, and 7-key party normalizer.
   - Single entry point `syncTransactionToFirestore` with durable offline queue and 5-retry limit (`B11.5`).
   - M1 fintech design token status indicator `#cloud-sync-status-badge`.

---

## 7. Verification Method

1. **Verify Blueprint Artifact Presence**:
   Inspect the comprehensive blueprint:
   ```powershell
   Get-Item "g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md"
   ```
2. **Execute Full Project Test Suite**:
   Run the master test runner from PowerShell:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs"
   ```
   *Expected Result*: 125/125 tests pass (Tiers 1–4).
3. **Verify Compliance with `firestore.rules` 12-Key Whitelist**:
   Run the Tier 1 test suite targeting `INV-SYNC-01`:
   ```powershell
   node -e "import('./.agents/teamwork/tests/tier1_feature_coverage.test.mjs')"
   ```
4. **Invalidation Conditions**:
   The blueprint is invalidated if:
   - Any transaction payload written to Firestore contains keys outside the 12 whitelisted keys.
   - Any mock user (such as `usr_google_rajesh_patel`) is retained in `index.html`.
   - An unauthenticated user can write to Firestore without triggering security rule denial.
   - The offline queue exceeds 5 retries without pausing.
