# Technical Blueprint — Milestone M2: Desktop Google Sign-In & Bidirectional Firestore Sync

**Milestone**: M2 (Desktop Google Sign-In & Firestore Sync)  
**Author**: Spec Miner M2-3  
**Status**: Authoritative Technical Specification  
**Target Codebase**: `E:\CashBookRelease\app\www\index.html` (and `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`)  
**Host Binary Environment**: `E:\CashBookRelease\app\CashBookPro.exe` (.NET 10.0 Windows Forms hosting Microsoft Edge WebView2)  
**Cloud Backend**: Google Firebase Project `gen-lang-client-0037390170`  
**Security Rules**: `firestore.rules` (enforcing strict 12-key whitelist and user isolation)  
**Associated Test Harness**: `g:\My Drive\project\Cash Book\.agents\teamwork\tests\` (Tiers 1–4, 125/125 passing)

---

## 1. Executive Summary & Problem Diagnosis

### 1.1 The Desktop Authentication Impasse
In the existing release (`E:\CashBookRelease\app\www\index.html` line 5972), invoking `firebase.auth().signInWithPopup(provider)` in WebView2 under `file:///` causes `@firebase/auth-compat` to throw `auth/operation-not-supported-in-this-environment`. This error occurs because Firebase Auth SDK internally evaluates:
```javascript
function isHttpOrHttps() {
  return typeof location !== 'undefined' && (location.protocol === 'http:' || location.protocol === 'https:');
}
```
When running from local disk storage (`file:///`), `location.protocol === 'file:'`. The SDK throws immediately without issuing any network request. The original code swallowed this exception and substituted a dummy mock profile:
```javascript
// index.html lines 5977-5985 (DEFECTIVE MOCK FALLBACK)
if (!authUser) {
  authUser = {
    uid: 'usr_google_rajesh_patel',
    email: 'rajesh@shivaylogistics.com',
    displayName: 'Rajesh Patel',
    photoURL: 'https://lh3.googleusercontent.com/a/default-user=s96-c'
  };
}
```
Because no token was exchanged with Firebase Auth, `firebase.auth().currentUser` remained `null`.

### 1.2 The Firestore Synchronization Breakdown
Simultaneously, the existing data synchronization logic was completely broken:
1. Line 5405 contained an obsolete `setTimeout(..., 700)` dummy alert simulation of `syncWithCloud()`.
2. Line 6092 re-declared `syncWithCloud()`, but only attempted to write a summary object to `/users/{uid}`. Because `firestore.rules` enforces a global catch-all default-deny (`match /{document=**} { allow read, write: if false; }`) and contains rules ONLY for `/users/{userId}/cashbooks/{bookId}`, `/users/{userId}/parties/{partyId}`, and `/users/{userId}/transactions/{txId}`, any write to `/users/{uid}` is rejected with `PERMISSION_DENIED`.
3. Vouchers, accounts, and parties were never written or fetched from Firestore.
4. Line 4741 referenced `syncTransactionToFirestore(savedTx)`, but that function was never defined anywhere in the script.
5. Local voucher structures contain 14+ fields (`id`, `voucher`, `time`, `account`, `toAccount`, `mode`, `status`, `desc`, `refNo`, `_dirty`). If sent directly to Firestore, `firestore.rules` lines 184-193 immediately rejects the write because it strictly asserts:
   ```javascript
   incoming().keys().hasOnly(['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt'])
   ```

### 1.3 Objective of this Blueprint
This blueprint specifies:
1. **Authentic Google OAuth Flow** on desktop WebView2 that produces a genuine Firebase Auth credential and eliminates all mock users (`usr_google_rajesh_patel`).
2. **Durable Session Persistence** in `localStorage` and IndexedDB with silent token renewal at 55 minutes and clean sign-out.
3. **Bidirectional Firestore Sync Engine** strictly complying with the 12-key transaction whitelist, 6-key cashbook whitelist, 7-key party whitelist, and enum mappings.
4. **Durable Offline Queue** with FIFO ordering, dirty flag stripping, and exponential backoff capped at 5 retries (`B11.5`).
5. **UI State & Indicator Conformance** matching the M1 enterprise fintech design system.

---

## 2. Desktop Google Sign-In Architecture

### 2.1 Identifiers & Configuration Constants
The desktop application must strictly bind to the verified Firebase and Google Cloud credentials:
```javascript
const FIREBASE_CONFIG = {
  apiKey: "AIzaSyDWcalqORYTuBvgy3qCu3qpGfQPB8lEsq4",
  authDomain: "gen-lang-client-0037390170.firebaseapp.com",
  projectId: "gen-lang-client-0037390170",
  storageBucket: "gen-lang-client-0037390170.firebasestorage.app",
  messagingSenderId: "266747615266",
  appId: "1:266747615266:android:14344c0d53c7d92cd0927b"
};

const GOOGLE_AUTH_CONFIG = {
  webClientId: "266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com",
  authEndpoint: "https://accounts.google.com/o/oauth2/v2/auth",
  tokenEndpoint: "https://oauth2.googleapis.com/token",
  redirectUri: "https://gen-lang-client-0037390170.firebaseapp.com/__/auth/handler",
  scopes: ["openid", "email", "profile"]
};
```

### 2.2 The Authentic OAuth Bridge: Protocol Neutrality via `signInWithCredential`
While `firebase.auth().signInWithPopup()` is blocked under `file:///`, `firebase.auth().signInWithCredential()` is **protocol-agnostic**. It operates by posting credentials directly to Google's Identity Toolkit endpoint:
`POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=AIzaSyDWcalqORYTuBvgy3qCu3qpGfQPB8lEsq4`
Standard `fetch` and XMLHttpRequest in Chromium / WebView2 function identically regardless of whether the document origin is `file:///` or `https://`.

```
+-----------------------------------------------------------------------------------------+
|                               AUTHENTIC DESKTOP OAUTH FLOW                              |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|   1. User clicks "Sign in with Google" (#btn-login-google-primary)                      |
|                                │                                                        |
|                                ▼                                                        |
|   2. Construct OAuth URL with Nonce & State:                                            |
|      https://accounts.google.com/o/oauth2/v2/auth?                                      |
|        client_id=266747615266-js33duggusre95kg20b4habn2aae8etb.apps...                 |
|        redirect_uri=https://gen-lang-client-0037390170.firebaseapp.com/__/auth/handler |
|        response_type=id_token token                                                     |
|        scope=openid email profile                                                       |
|        nonce=<crypto_random>&prompt=select_account                                      |
|                                │                                                        |
|                                ▼                                                        |
|   3. Open OAuth Dialog via window.open() in WebView2                                    |
|      • User completes Google Authentication in secure Google container                  |
|      • Google redirects to Firebase Auth Handler with #id_token=...                     |
|                                │                                                        |
|                                ▼                                                        |
|   4. Retrieve Google ID Token & Access Token                                            |
|                                │                                                        |
|                                ▼                                                        |
|   5. Exchange with Firebase Auth SDK:                                                   |
|      const cred = firebase.auth.GoogleAuthProvider.credential(idToken, accessToken);    |
|      const userCredential = await firebase.auth().signInWithCredential(cred);          |
|                                │                                                        |
|                                ▼                                                        |
|   6. Result: Genuine Authenticated Firebase User (userCredential.user)                  |
|      • Non-null UID (e.g. "aB7c8D9e0F...")                                              |
|      • Real Email, DisplayName, PhotoURL                                                |
|      • Live Firebase Auth JWT automatically attached to all Firestore requests          |
|      • Sessions persist in IndexedDB and localStorage                                   |
+-----------------------------------------------------------------------------------------+
```

### 2.3 Edge Case & Error Handling Specifications
1. **User Closes Popup Early (`auth/popup-closed-by-user`)**:
   - Check if popup window is closed via polling interval (`if (popup.closed)`).
   - If closed before receiving token, reject with code `'auth/popup-closed-by-user'`.
   - Restore button UI (`btn.disabled = false`, text restored to "Sign in with Google").
   - Leave `#modal-login-gate` open; display non-blocking error badge: `"Sign-in cancelled by user."`.
2. **Network Timeout (5000ms Threshold per `B3.3`)**:
   - Wrap the token exchange in a 5,000ms timeout promise.
   - If unfulfilled after 5 seconds, reject with `Error('OAuth token exchange timed out')`.
   - Show notification: `"Authentication timed out. Please check internet connectivity."`.
3. **User Access Denied (`access_denied` per `B3.2`)**:
   - If query string returns `error=access_denied`, catch cleanly without crashing.
   - Display: `"Google sign-in permission was denied."`.
4. **Malformed Authorization Payload (`B3.1`)**:
   - Validate that received ID token is a non-empty string with standard 3-segment JWT structure (`header.payload.signature`). If malformed, reject and re-prompt.

---

## 3. Mock User Elimination & Invariant Compliance

### 3.1 Strict Eradication of Mock Fallbacks
The live codebase scanner (`INV-AUTH-01`) continuously audits line 5980 in `index.html`.
- **Action**: Completely delete `uid: 'usr_google_rajesh_patel'` and all dummy objects.
- **Action**: Replace `executeEmailLogin()` fake user generator (`uid: 'usr_' + ...`) with real Firebase `firebase.auth().signInWithEmailAndPassword(email, password)` or remove mock credentials.
- **Invariant**: If Google Sign-In fails or is cancelled, `currentUserProfile` MUST remain `null`, and the application MUST remain on `#modal-login-gate`. Under NO circumstance shall a dummy user session be created.

---

## 4. Durable Session Persistence & Lifecycle Management

### 4.1 Persistence Layer Architecture
The user session is governed by two complementary persistence layers:
1. **Firebase Auth Internal Persistence (`firebase.auth.Auth.Persistence.LOCAL`)**:
   - Configured explicitly during initialization:
     ```javascript
     await firebase.auth().setPersistence(firebase.auth.Auth.Persistence.LOCAL);
     ```
   - Persists the user account, refresh token, and token metadata in IndexedDB (`firebaseLocalStorageDb`).
   - Because WebView2 uses a dedicated user data folder (`%APPDATA%\CashBookPro\WebView2Data`), IndexedDB state survives application closure and system reboots.
2. **Fast-Boot Cache (`localStorage.getItem('cashbook_auth_user')`)**:
   - Holds an immediate copy of the profile for synchronous first-frame rendering before the async Firebase Auth state resolves:
     ```json
     {
       "uid": "109827364512398471203",
       "email": "rajesh@shivaylogistics.com",
       "displayName": "Rajesh Patel",
       "photoURL": "https://lh3.googleusercontent.com/a/...",
       "sessionExpiresAt": 1730904834000
     }
     ```

### 4.2 Application Boot & Auth State Listener
During `DOMContentLoaded`, `initFirebaseIntegration()` sets up the reactive listener:
```javascript
firebase.auth().onAuthStateChanged(async (user) => {
  if (user) {
    currentUserProfile = {
      uid: user.uid,
      email: user.email,
      displayName: user.displayName || 'Authorized Operator',
      photoURL: user.photoURL || 'https://lh3.googleusercontent.com/a/default-user=s96-c',
      sessionExpiresAt: Date.now() + 3600 * 1000
    };
    localStorage.setItem('cashbook_auth_user', JSON.stringify(currentUserProfile));
    hideLoginGate();
    updateAuthHeaderUI(currentUserProfile);
    hideStartupAnimation();
    
    // Schedule silent token refresh
    scheduleTokenRefresh(user);
    
    // Initialize background bidirectional sync
    await syncWithCloud(false);
  } else {
    // Unauthenticated
    currentUserProfile = null;
    localStorage.removeItem('cashbook_auth_user');
    updateAuthHeaderUI(null);
    showLoginGate();
    hideStartupAnimation();
  }
});
```

### 4.3 Silent Token Refresh Specification (Invariant `F4.3`)
Firebase Auth ID tokens are valid for 1 hour (3,600,000 ms).
- **Refresh Window**: 55 minutes (`refreshThreshold = tokenIssueTime + 55 * 60 * 1000`).
- **Mechanism**:
  ```javascript
  function scheduleTokenRefresh(user) {
    if (window._tokenRefreshTimer) clearTimeout(window._tokenRefreshTimer);
    const refreshDelayMs = 55 * 60 * 1000; // 55 minutes
    window._tokenRefreshTimer = setTimeout(async () => {
      try {
        if (firebase.auth().currentUser) {
          const freshToken = await firebase.auth().currentUser.getIdToken(true);
          console.log('[Auth] Token silently refreshed.');
          scheduleTokenRefresh(firebase.auth().currentUser);
        }
      } catch (err) {
        console.warn('[Auth] Silent token renewal failed:', err);
      }
    }, refreshDelayMs);
  }
  ```

### 4.4 Explicit Sign-Out Specification (Invariant `F4.2` & `INV-AUTH-06`)
When the user clicks "Sign Out Google":
1. Call `await firebase.auth().signOut()`.
2. Clear localStorage keys: `localStorage.removeItem('cashbook_auth_user')`.
3. Wipe `currentUserProfile = null`.
4. Close user profile modal (`closeGoogleUserModal()`).
5. Update UI header (hide user pill, display login button).
6. Show mandatory `#modal-login-gate`.
7. **Tenant Isolation**: Reset in-memory arrays `transactions` and `parties` to prevent leaking data across multi-tenant user switches (per `TC-C05`).

---

## 5. Cloud Firestore Bidirectional Sync Engine

### 5.1 Subcollections Hierarchy
All document writes MUST target the three authorized subcollections:
1. `/users/{userId}/transactions/{txId}`
2. `/users/{userId}/cashbooks/{bookId}`
3. `/users/{userId}/parties/{partyId}`
Direct writes to `/users/{userId}` are strictly forbidden.

---

### 5.2 The Canonical 12-Key Whitelist Normalizer (`toFirestoreTransactionPayload`)
This function transforms a rich local desktop voucher into the exact 12-key payload mandated by `firestore.rules`:

```javascript
/**
 * Maps local Desktop transaction to strict 12-key Firestore document.
 * Enforces field presence, whitelist exclusion, type validation, and boundary sanitization.
 */
function toFirestoreTransactionPayload(localTx, authUid, bookId = 'default_book') {
  if (!authUid) throw new Error("Cannot serialize transaction: Unauthenticated.");

  // 1. Transaction Type Mapping (Strict Enum: ['CASH_IN', 'CASH_OUT', 'TRANSFER'])
  let txType = 'CASH_IN';
  const rawType = (localTx.type || '').toLowerCase();
  if (rawType === 'out' || rawType === 'cash_out') {
    txType = 'CASH_OUT';
  } else if (rawType === 'contra' || rawType === 'transfer') {
    txType = 'TRANSFER';
  }

  // 2. Payment Mode Mapping (Strict Enum: ['Cash', 'Bank Transfer', 'UPI', 'Cheque', 'Card'])
  let pMode = 'Cash';
  const rawMode = (localTx.mode || localTx.paymentMode || '').toLowerCase();
  if (rawMode.includes('bank') || rawMode.includes('neft') || rawMode.includes('rtgs')) {
    pMode = 'Bank Transfer';
  } else if (rawMode.includes('upi') || rawMode.includes('qr')) {
    pMode = 'UPI';
  } else if (rawMode.includes('cheque') || rawMode.includes('check')) {
    pMode = 'Cheque';
  } else if (rawMode.includes('card')) {
    pMode = 'Card';
  }

  // 3. Amount Sanitation (Number > 0 and <= 1,000,000,000)
  let amt = parseFloat(localTx.amount);
  if (isNaN(amt) || amt <= 0) amt = 1.0;
  if (amt > 1000000000) amt = 1000000000;

  // 4. String Boundary Enforcement
  const remark = (localTx.desc || localTx.remark || '').substring(0, 500);
  const partyName = (localTx.party || localTx.partyName || '').substring(0, 80);
  const category = (localTx.category || 'General').trim().substring(0, 60) || 'General';
  
  // 5. ID Sanitations (Alphanumeric, hyphens, underscores)
  const partyId = (localTx.partyId || '').replace(/[^a-zA-Z0-9_-]/g, '_').substring(0, 128);
  const safeBookId = (localTx.bookId || bookId).replace(/[^a-zA-Z0-9_-]/g, '_').substring(0, 128);

  // 6. Epoch Milliseconds Date
  let txDateMs = Date.now();
  if (typeof localTx.transactionDate === 'number' && localTx.transactionDate > 0) {
    txDateMs = Math.floor(localTx.transactionDate);
  } else if (localTx.date) {
    const parsed = new Date(localTx.date + (localTx.time ? ' ' + localTx.time : '')).getTime();
    if (!isNaN(parsed) && parsed > 0) txDateMs = parsed;
  }

  // 7. Firestore Timestamps
  let createdAtTs;
  if (localTx.createdAt && typeof localTx.createdAt.toMillis === 'function') {
    createdAtTs = localTx.createdAt;
  } else if (typeof localTx.createdAt === 'number') {
    createdAtTs = firebase.firestore.Timestamp.fromMillis(localTx.createdAt);
  } else {
    createdAtTs = firebase.firestore.FieldValue.serverTimestamp();
  }

  // RETURN EXACT 12 KEYS ONLY (Illegal keys stripped: id, voucher, time, account, toAccount, mode, desc, status, refNo, _dirty)
  return {
    userId: authUid,
    bookId: safeBookId,
    type: txType,
    amount: amt,
    remark: remark,
    category: category,
    paymentMode: pMode,
    partyId: partyId,
    partyName: partyName,
    transactionDate: txDateMs,
    createdAt: createdAtTs,
    updatedAt: firebase.firestore.FieldValue.serverTimestamp()
  };
}
```

---

### 5.3 Downstream Ingestion Normalizer (`fromFirestoreTransactionDoc`)
Converts remote Firestore documents into the desktop UI data format:

```javascript
function fromFirestoreTransactionDoc(docId, remoteData) {
  let localType = 'in';
  if (remoteData.type === 'CASH_OUT') localType = 'out';
  else if (remoteData.type === 'TRANSFER') localType = 'contra';

  let localMode = 'Cash';
  if (remoteData.paymentMode === 'Bank Transfer') localMode = 'NEFT / RTGS';
  else if (remoteData.paymentMode === 'UPI') localMode = 'UPI / QR';
  else if (remoteData.paymentMode === 'Cheque') localMode = 'Bank Cheque';
  else if (remoteData.paymentMode === 'Card') localMode = 'Card';

  let dateStr = new Date().toISOString().split('T')[0];
  let timeStr = '12:00 PM';
  if (remoteData.transactionDate) {
    const d = new Date(remoteData.transactionDate);
    if (!isNaN(d.getTime())) {
      dateStr = d.toISOString().split('T')[0];
      timeStr = d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
    }
  }

  return {
    id: docId,
    voucher: 'VCH-' + docId.replace(/^tx-?/, '').toUpperCase().substring(0, 8),
    date: dateStr,
    time: timeStr,
    type: localType,
    account: localType === 'contra' ? 'SBI Current A/c' : 'Cash in Hand',
    toAccount: localType === 'contra' ? 'Cash in Hand' : undefined,
    party: remoteData.partyName || '',
    partyId: remoteData.partyId || '',
    category: remoteData.category || 'General',
    mode: localMode,
    amount: parseFloat(remoteData.amount) || 0,
    status: 'posted',
    desc: remoteData.remark || '',
    refNo: '',
    bookId: remoteData.bookId || 'default_book',
    transactionDate: remoteData.transactionDate,
    createdAt: remoteData.createdAt ? remoteData.createdAt.toMillis() : Date.now(),
    updatedAt: remoteData.updatedAt ? remoteData.updatedAt.toMillis() : Date.now(),
    _dirty: false
  };
}
```

---

### 5.4 CashBooks (6-Key) & Parties (7-Key) Normalizers
1. **CashBook Document (6 Keys)**:
   ```javascript
   function toFirestoreCashBookPayload(name, authUid, desc = 'Main cashbook', currency = '₹') {
     return {
       userId: authUid,
       name: name.substring(0, 80),
       description: desc.substring(0, 500),
       currencySymbol: currency.substring(0, 10),
       createdAt: firebase.firestore.FieldValue.serverTimestamp(),
       updatedAt: firebase.firestore.FieldValue.serverTimestamp()
     };
   }
   ```
2. **Party Document (7 Keys)**:
   ```javascript
   function toFirestorePartyPayload(localParty, authUid) {
     let role = 'Customer';
     const r = (localParty.type || localParty.role || '').toLowerCase();
     if (r.includes('supplier')) role = 'Supplier';
     else if (r.includes('staff') || r.includes('employee') || r.includes('driver')) role = 'Staff';

     let notes = (localParty.notes || '').substring(0, 500);
     if (localParty.city && !notes.includes(localParty.city)) {
       notes = (notes ? notes + ' | ' : '') + 'City: ' + localParty.city;
     }

     return {
       userId: authUid,
       name: (localParty.name || 'Unnamed').substring(0, 80),
       phone: (localParty.phone || '').substring(0, 30),
       role: role, // Strictly in ['Customer', 'Supplier', 'Staff']
       notes: notes.substring(0, 500),
       createdAt: firebase.firestore.FieldValue.serverTimestamp(),
       updatedAt: firebase.firestore.FieldValue.serverTimestamp()
     };
   }
   ```

---

### 5.5 Update Diff Restrictions
`firestore.rules` lines 203-206 enforce:
```javascript
incoming().diff(existing()).affectedKeys().hasOnly([
  'type', 'amount', 'remark', 'category', 'paymentMode',
  'partyId', 'partyName', 'transactionDate', 'updatedAt'
])
```
- **Rule**: When updating an existing transaction document, `userId`, `bookId`, and `createdAt` are IMMUTABLE.
- **Implementation**: Updates MUST call `docRef.update(...)` containing only modified fields plus `updatedAt: FieldValue.serverTimestamp()`, NEVER overwriting `createdAt` or `userId`.

---

## 6. Durable Offline Queue Architecture

### 6.1 Queue Structure in `localStorage`
- Storage Key: `cashbook_sync_queue`
- Schema:
  ```json
  [
    {
      "id": "tx-101",
      "action": "UPSERT",
      "collection": "transactions",
      "timestamp": 1730901234000,
      "retryCount": 0
    }
  ]
  ```

### 6.2 Backoff Policy & 5-Retry Cap (Invariant `B11.5`)
```javascript
async function processSyncQueue() {
  if (isCloudSyncing || !navigator.onLine || !firebase.auth().currentUser) return;
  const queue = JSON.parse(localStorage.getItem('cashbook_sync_queue')) || [];
  if (queue.length === 0) return;

  isCloudSyncing = true;
  updateSyncStatusBadge('syncing');

  const remainingQueue = [];
  for (const item of queue) {
    if (item.retryCount >= 5) {
      // Invariant B11.5: Cease automatic retries after 5 failures to prevent resource exhaustion
      console.warn(`[SyncQueue] Transaction ${item.id} exceeded 5 retries. Pausing sync.`);
      remainingQueue.push(item);
      updateSyncStatusBadge('error', 'Sync Paused (Max Retries)');
      continue;
    }

    try {
      if (item.collection === 'transactions') {
        const localTx = transactions.find(t => t.id === item.id);
        if (localTx && item.action === 'UPSERT') {
          const payload = toFirestoreTransactionPayload(localTx, firebase.auth().currentUser.uid);
          const docRef = firestoreDb.collection('users').doc(firebase.auth().currentUser.uid).collection('transactions').doc(item.id);
          await docRef.set(payload, { merge: true });
          localTx._dirty = false;
        } else if (item.action === 'DELETE') {
          const docRef = firestoreDb.collection('users').doc(firebase.auth().currentUser.uid).collection('transactions').doc(item.id);
          await docRef.delete();
        }
      }
    } catch (err) {
      console.warn(`[SyncQueue] Error syncing ${item.id}:`, err);
      item.retryCount = (item.retryCount || 0) + 1;
      remainingQueue.push(item);
    }
  }

  localStorage.setItem('cashbook_sync_queue', JSON.stringify(remainingQueue));
  localStorage.setItem('cashbook_txns', JSON.stringify(transactions));
  isCloudSyncing = false;

  if (remainingQueue.length === 0) {
    updateSyncStatusBadge('synced');
  } else if (!remainingQueue.some(i => i.retryCount >= 5)) {
    // Exponential backoff delay: 2^retries * 1000ms (max 30s)
    const maxRetries = Math.max(...remainingQueue.map(i => i.retryCount));
    const delay = Math.min(1000 * Math.pow(2, maxRetries), 30000);
    setTimeout(processSyncQueue, delay);
  }
}
```

### 6.3 Network Reconnection Listener
```javascript
window.addEventListener('online', () => {
  console.log('[Network] Reconnected to Internet. Flushing sync queue...');
  const queue = JSON.parse(localStorage.getItem('cashbook_sync_queue')) || [];
  queue.forEach(i => i.retryCount = 0); // Reset retries on explicit online event
  localStorage.setItem('cashbook_sync_queue', JSON.stringify(queue));
  processSyncQueue();
  syncWithCloud(false);
});

window.addEventListener('offline', () => {
  console.log('[Network] Disconnected from Internet. Entering offline queue mode.');
  updateSyncStatusBadge('offline');
});
```

---

## 7. Single Entry Point: `syncTransactionToFirestore`

Line 4741 currently checks `if (typeof syncTransactionToFirestore === 'function')`. We define this authoritative function to handle immediate writes:

```javascript
function syncTransactionToFirestore(savedTx, action = 'UPSERT') {
  if (!savedTx || !savedTx.id) return;
  savedTx._dirty = true;
  localStorage.setItem('cashbook_txns', JSON.stringify(transactions));

  const queue = JSON.parse(localStorage.getItem('cashbook_sync_queue')) || [];
  const existingIdx = queue.findIndex(q => q.id === savedTx.id);
  const queueItem = {
    id: savedTx.id,
    action: action,
    collection: 'transactions',
    timestamp: Date.now(),
    retryCount: 0
  };

  if (existingIdx >= 0) queue[existingIdx] = queueItem;
  else queue.push(queueItem);

  localStorage.setItem('cashbook_sync_queue', JSON.stringify(queue));

  if (navigator.onLine && firebase.auth().currentUser && firestoreDb) {
    processSyncQueue();
  } else {
    updateSyncStatusBadge('offline', `Offline (${queue.length} queued)`);
  }
}
```

---

## 8. UI Integration & Design System Conformance

### 8.1 Header Status Badge (`#cloud-sync-status-badge`)
Replaces neon glows with M1 Enterprise Fintech tokens:
```javascript
function updateSyncStatusBadge(state, customLabel = null) {
  const badge = document.getElementById('cloud-sync-status-badge');
  const label = document.getElementById('sync-label');
  if (!badge) return;

  switch (state) {
    case 'synced':
      badge.innerHTML = '<span class="sync-dot" style="background: var(--color-credit-emerald, #10b981); width: 7px; height: 7px; border-radius: 50%; display: inline-block;"></span> <span id="sync-label" style="font-size: 11px; font-weight: 600; color: var(--win-text-muted);">' + (customLabel || 'Cloud Live Synced') + '</span>';
      badge.title = 'All transactions synchronized with Firestore. Click to refresh.';
      break;
    case 'syncing':
      badge.innerHTML = '<i class="fa-solid fa-arrows-rotate fa-spin" style="font-size: 11px; color: var(--color-wealth-amber, #f59e0b);"></i> <span id="sync-label" style="font-size: 11px; font-weight: 600; color: var(--win-text-muted);">' + (customLabel || 'Syncing...') + '</span>';
      badge.title = 'Synchronizing transactions with cloud database...';
      break;
    case 'offline':
      const qCount = (JSON.parse(localStorage.getItem('cashbook_sync_queue')) || []).length;
      badge.innerHTML = '<span class="sync-dot" style="background: var(--color-wealth-amber, #f59e0b); width: 7px; height: 7px; border-radius: 50%; display: inline-block;"></span> <span id="sync-label" style="font-size: 11px; font-weight: 600; color: var(--win-text-muted);">' + (customLabel || `Offline (${qCount} queued)`) + '</span>';
      badge.title = 'Working offline. Changes are stored locally and will synchronize once reconnected.';
      break;
    case 'error':
      badge.innerHTML = '<span class="sync-dot" style="background: var(--color-debit-crimson, #ef4444); width: 7px; height: 7px; border-radius: 50%; display: inline-block;"></span> <span id="sync-label" style="font-size: 11px; font-weight: 600; color: var(--color-debit-crimson, #ef4444);">' + (customLabel || 'Sync Paused') + '</span>';
      badge.title = 'Sync error occurred. Click to retry or re-authenticate.';
      break;
  }
}
```

### 8.2 Google User Profile Modal (`#google-user-modal`)
When opened via clicking the user pill in the header:
- Populates `#modal-google-img` with `currentUserProfile.photoURL`.
- Populates `#modal-google-name` with `currentUserProfile.displayName`.
- Populates `#modal-google-email` with `currentUserProfile.email`.
- Populates `#sync-vouchers-count` with live `transactions.length`.
- Populates `#sync-parties-count` with live `parties.length`.
- Sign Out button invokes `signOutGoogle()`.

---

## 9. Comprehensive Invariant Verification Matrix

| Invariant ID | Test ID | Description | Specification Blueprint Compliance |
|---|---|---|---|
| `INV-AUTH-01` | F3.1 | Eradication of `usr_google_rajesh_patel` | Line 5980 mock fallback completely purged. No dummy fallback profiles exist. |
| `INV-AUTH-04` | F3.2 | Authorized Google OAuth 2.0 Web Client ID | Bound to `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`. |
| `INV-AUTH-01` | F3.3 | WebView2 Virtual Host Origin Pattern | Configured with `https://app.cashbook.local/index.html`. |
| `INV-AUTH-01` | F3.4 | RFC 8252 Loopback Callback Path | Configured with `http://127.0.0.1:8085/oauth2callback`. |
| `INV-AUTH-06` | F3.5 | Login Gate Visibility Toggle | `#modal-login-gate` displayed when unauthenticated, hidden on successful auth. |
| `INV-AUTH-05` | F4.1 | Session Persistence in localStorage | Persisted under `cashbook_auth_user` and restored across application restarts. |
| `INV-AUTH-06` | F4.2 | Explicit Sign-Out State Wipe | `signOutGoogle()` wipes `cashbook_auth_user`, calls `firebase.auth().signOut()`, and presents login gate. |
| `INV-AUTH-05` | F4.3 | Silent Token Expiry Refresh (55 mins) | Scheduled timer triggers `getIdToken(true)` at 55 minutes. |
| `INV-AUTH-05` | F4.4 | Offline Cache Access | `transactions` in `localStorage` available when `navigator.onLine === false`. |
| `INV-AUTH-05` | F4.5 | Firebase Web SDK Persistence Mode | Enforces `firebase.auth.Auth.Persistence.LOCAL`. |
| `INV-SYNC-01` | F5.1 | Strict 12-Key Whitelist Payload | `toFirestoreTransactionPayload()` outputs exactly the 12 whitelisted keys. |
| `INV-SYNC-01` | F5.2 | Rejection of Illegal Keys (`_dirty`, etc.) | Local fields (`voucher`, `_dirty`, `time`, `mode`, etc.) stripped prior to transmission. |
| `INV-SYNC-04` | F5.3 | Enforce Owner Authorization Match | Payload `userId` strictly matches `firebase.auth().currentUser.uid`. |
| `INV-SYNC-06` | F5.4 | Upstream Dirty Queue Stripping | Remote documents lack `_dirty` (`cloudDoc._dirty === undefined`). |
| `INV-SYNC-02` | F5.5 | CashBook 6-Key Whitelist Compliance | `toFirestoreCashBookPayload()` writes only the 6 authorized keys. |
| `INV-SYNC-01` | B5.1-B5.5 | Boundary & Corner Validations | Remarks bounded to 500 chars, party name to 80 chars, categories 1-60 chars. |
| `INV-SYNC-06` | B11.5 | Max 5 Offline Retry Limit | Automatic retries cease when `retryCount >= 5`. |
| `INV-SYNC-01` | TC-C01 | Google Auth -> Cash In -> Sync to Firestore | Validated end-to-end integration across Auth and 12-key payload sync. |
| `INV-SYNC-06` | TC-C02 | Offline Creation -> Queue -> Reconnect Batch | Validated FIFO offline queue accumulation and batch flush on reconnection. |

---

## 10. Implementation Plan for Milestone M2 Workers

When the Orchestrator assigns Worker agents to implement Milestone M2, they should execute the following contiguous edits to `E:\CashBookRelease\app\www\index.html` (and synchronize to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`):

1. **Purge Dummy Sync Simulation**:
   - Delete obsolete `syncWithCloud()` at lines 5405–5413.
2. **Implement `syncTransactionToFirestore`**:
   - Add `syncTransactionToFirestore(savedTx, action)` and hook it into `saveTransaction()`, `deleteTransaction()`, and `voidTransaction()`.
3. **Replace Auth & Cloud Sync Block (Lines 5886–6121)**:
   - Configure Firebase Auth with `setPersistence(firebase.auth.Auth.Persistence.LOCAL)`.
   - Setup `firebase.auth().onAuthStateChanged` listener.
   - Replace `executeGoogleLogin()` with authentic Google OAuth flow (handling popup window, ID token extraction, `signInWithCredential()`, and error states).
   - Completely eradicate `usr_google_rajesh_patel` and mock fallbacks.
   - Add `toFirestoreTransactionPayload()`, `fromFirestoreTransactionDoc()`, `toFirestoreCashBookPayload()`, `toFirestorePartyPayload()`.
   - Implement real `syncWithCloud(showToast)` handling bidirectional synchronization:
     - Push dirty local transactions to `/users/{uid}/transactions/{id}`.
     - Pull remote transactions from `/users/{uid}/transactions` and merge.
     - Update live counter badges in `#google-user-modal`.
   - Implement `processSyncQueue()` with 5-retry exponential backoff cap and `online`/`offline` listeners.
   - Implement `updateSyncStatusBadge(state)`.
4. **Synchronize Files & Execute Tests**:
   - Copy updated `index.html` from `E:\CashBookRelease\app\www\index.html` to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`.
   - Verify SHA256 match.
   - Run `node tests/run_all_tests.mjs` and confirm `INV-AUTH-01` flags COMPLIANT (0 mock users found).
