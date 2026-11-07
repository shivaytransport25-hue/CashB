# Handoff Report — Explorer M2-1

**Milestone**: M2 (Desktop Google Sign-In & Firestore Sync)  
**Task**: Investigate Google Sign-In implementation, WebView2 runtime environment, OAuth failure under `file:///`, host application construction in `CashBookDesktop`, and formulate authentic strategy for desktop Google OAuth and session persistence.  
**Deliverable Artifact**: `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_1\analysis.md`  
**Date**: 2026-11-06  

---

## 1. Observation

1. **Host Application Architecture (`Form1.cs`)**:
   In `E:\CashBookDesktop\Form1.cs`:
   - Lines 61-64 initialize WebView2 environment with persistent user data folder `%APPDATA%\CashBookPro\WebView2Data`:
     ```csharp
     var userDataFolder = Path.Combine(
         Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
         "CashBookPro", "WebView2Data");
     Directory.CreateDirectory(userDataFolder);
     var env = await CoreWebView2Environment.CreateAsync(null, userDataFolder);
     await webView.EnsureCoreWebView2Async(env);
     ```
   - Lines 74-78 navigate directly to a `file:///` URI:
     ```csharp
     string appHtmlPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "www", "index.html");
     if (File.Exists(appHtmlPath))
     {
         webView.CoreWebView2.Navigate(new Uri(appHtmlPath).AbsoluteUri);
     }
     ```
   - No `SetVirtualHostNameToFolderMapping` is configured.
   - No `WebMessageReceived` event handler is attached to `webView.CoreWebView2`.
   - The application is a WinForms .NET 10.0 project (`E:\CashBookDesktop\CashBookPro.csproj`) with package `Microsoft.Web.WebView2` v1.0.4314.35.

2. **Synchronous `file:///` Rejection in Firebase Auth SDK**:
   In `E:\CashBookRelease\app\www\js\firebase-auth-compat.js` lines 1-2:
   - `signInWithPopup(e)` calls `Ei(ci(), this._delegate, "operation-not-supported-in-this-environment")`.
   - `ci()` evaluates `("http:" === ni() || "https:" === ni() || p() || ii()) && !(f() || h()) && ai() && !oi()`.
   - `ni()` returns `self.location?.protocol`, which is `"file:"`.
   - `ii()` returns `true` ONLY if `self.location.protocol` is `file:` AND the user agent matches `/iphone|ipad|ipod|android/`. On Windows desktop, it returns `false`.
   - Therefore, `ci()` unconditionally returns `false` on desktop `file:///`, and `signInWithPopup` throws `auth/operation-not-supported-in-this-environment` **synchronously and immediately** before opening any window or making any network call.

3. **Silent Mock User Injection in `index.html`**:
   In `E:\CashBookRelease\app\www\index.html` lines 5972-5986:
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
   The caught error is ignored, and a fake user object is placed into `localStorage['cashbook_auth_user']`.
   Crucially, `firebase.auth().currentUser` remains `null`.

4. **Google Cloud OAuth 2.0 Web Client Configuration**:
   In `G:\My Drive\project\Cash Book\app\google-services.json` lines 24-27:
   - Client ID: `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`
   - Client Type: `3` (Web Application Client).
   - Live endpoint probe:
     ```
     curl.exe -s -i "https://accounts.google.com/o/oauth2/v2/auth?client_id=266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com&redirect_uri=http://127.0.0.1:54321/auth-callback"
     ```
     Returns HTTP 302 to `https://accounts.google.com/signin/oauth/error?authError=...redirect_uri_mismatch...`.
   - Live endpoint probe with Firebase handler:
     ```
     curl.exe -s -i "https://accounts.google.com/o/oauth2/v2/auth?client_id=266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com&redirect_uri=https://gen-lang-client-0037390170.firebaseapp.com/__/auth/handler"
     ```
     Returns HTTP 302 to `https://accounts.google.com/v3/signin/identifier?...` (Success, 200 OK login page).

5. **Test Invariant Baseline**:
   Running `node "g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs"`:
   - 125/125 tests pass across Tiers 1-4.
   - Codebase invariant auditor scans `E:\CashBookRelease\app\www\index.html` line 5980 and flags `INV-AUTH-01 Eradication of usr_google_rajesh_patel IN_PROGRESS (M1-M4)`.

---

## 2. Logic Chain

1. **Step 1 (Root Cause of Immediate Failure)**: Observation 1 establishes that `Form1.cs` loads `index.html` via `file:///`. Observation 2 proves that `firebase-auth-compat.js` executes `ci()`, which evaluates to `false` under Windows desktop `file:` scheme and immediately throws `auth/operation-not-supported-in-this-environment`. Therefore, `signInWithPopup` under `file:///` fails by design within the Firebase SDK itself.
2. **Step 2 (The Origin of the Mock User)**: Observation 3 shows that line 5972 catches the exception from Step 1 and falls through to inject `usr_google_rajesh_patel` into `localStorage`. This fake identity creates a misleading visual login state while leaving `firebase.auth().currentUser == null`.
3. **Step 3 (Downstream Cloud Sync Blockage)**: Because `firebase.auth().currentUser` is `null`, any subsequent Firestore request is unauthenticated. Combined with Explorer M2-2's findings on `firestore.rules` requiring `isSignedIn() && request.auth.uid == userId`, all cloud sync operations are blocked with `PERMISSION_DENIED`.
4. **Step 4 (Google Cloud Constraint)**: Observation 4 proves that the Web Client ID only allows `https://gen-lang-client-0037390170.firebaseapp.com/__/auth/handler` as its redirect URI, rejecting arbitrary loopback ports with `redirect_uri_mismatch`. Furthermore, RFC 8252 and Google policy block interactive logins inside embedded web views (`403 disallowed_useragent`).
5. **Step 5 (Synthetic Solution Path)**:
   - To bypass the `file:///` trap: `Form1.cs` must call `SetVirtualHostNameToFolderMapping("app.cashbook.local", wwwPath, Allow)` and navigate to `https://app.cashbook.local/index.html`.
   - To comply with Google's OAuth policy: The host orchestrates Google login via the user's default system browser using the registered Firebase Auth handler or Google Identity Services, capturing the credential token.
   - To establish a real Firebase session: The token is passed to `index.html` via `PostWebMessageAsJson` and redeemed via `firebase.auth().signInWithCredential(GoogleAuthProvider.credential(idToken))`.
   - To persist the session: `firebase.auth().setPersistence(Persistence.LOCAL)` coupled with the already-present `%APPDATA%\CashBookPro\WebView2Data` user data folder automatically restores the real session across app restarts.

---

## 3. Caveats

1. **Google Cloud Console Access**: The Web Client ID cannot be modified or reconfigured with new redirect URIs without owner credentials to the Google Cloud Console. The solution must utilize the existing registered redirect URI `https://gen-lang-client-0037390170.firebaseapp.com/__/auth/handler`.
2. **Standalone Web Mode vs Desktop WinForms**: When running `index.html` outside `CashBookPro.exe` in a standard browser (e.g., during development), `window.chrome.webview` is undefined. The code must gracefully fall back to Google Identity Services (`gsi/client`) or standard `signInWithPopup(provider)`.
3. **No other caveats.**

---

## 4. Conclusion

1. The Google Sign-In failure on Windows Desktop is caused by loading `index.html` under `file:///`, which triggers an immediate SDK-level exception in `firebase-auth-compat.js` and causes fallback to the dummy user `usr_google_rajesh_patel`.
2. The dummy user must be completely eliminated from `index.html` line 5980 (`INV-AUTH-01`).
3. The authentic architecture requires:
   - **Host Layer (`Form1.cs`)**: Map the `www` folder to `https://app.cashbook.local` using `SetVirtualHostNameToFolderMapping`, and attach a `WebMessageReceived` bridge for external browser authentication.
   - **Client Layer (`index.html`)**: Listen for host authentication messages, redeem the ID token with `firebase.auth().signInWithCredential()`, enable `Persistence.LOCAL`, bind `onAuthStateChanged`, and pass real authenticated credentials to the Firestore sync engine developed by Explorer M2-2.

---

## 5. Verification Method

1. **Automated Invariant & Feature Suite Verification**:
   Execute the master test suite from PowerShell:
   ```powershell
   node 'g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs'
   ```
   Verify 100% pass across all 125 test cases. Ensure `INV-AUTH-01`, `INV-AUTH-04`, `INV-AUTH-05`, and `INV-AUTH-06` pass without errors.
2. **Mock User Eradication Audit**:
   Verify removal of `usr_google_rajesh_patel` in `E:\CashBookRelease\app\www\index.html`:
   ```powershell
   Select-String -Path "E:\CashBookRelease\app\www\index.html" -Pattern "usr_google_rajesh_patel"
   ```
   Must return zero occurrences.
3. **WebView2 Virtual Host Inspection**:
   Inspect `E:\CashBookDesktop\Form1.cs` to confirm `SetVirtualHostNameToFolderMapping("app.cashbook.local", ...)` and removal of direct `file:///` navigation.
4. **Invalidation Conditions**:
   The strategy is invalidated if:
   - Any dummy fallback (such as `usr_google_rajesh_patel` or hardcoded UID) is retained in `index.html`.
   - `firebase.auth().currentUser` remains `null` after user clicks "Sign in with Google".
   - `localStorage['cashbook_auth_user']` is not purged upon explicit sign out.
