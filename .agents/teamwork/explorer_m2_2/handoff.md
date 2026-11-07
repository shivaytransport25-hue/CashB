# Handoff Report — Explorer M2-2

**Milestone**: M2 (Desktop Google Sign-In & Firestore Sync)  
**Task**: Investigate Cloud Firestore sync in `index.html`, audit compliance with `firestore.rules` 12-key whitelist for `/users/{uid}/transactions/{txId}`, and formulate bidirectional sync and offline queueing.  
**Deliverable Artifact**: `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\analysis.md`  
**Date**: 2026-11-06  

---

## 1. Observation

1. **Security Rules Catch-All Deny & Subcollection Restrictions**:
   In `g:\My Drive\project\Cash Book\firestore.rules`:
   - Lines 6-9 enforce a default deny:
     ```
     match /{document=**} {
       allow read, write: if false;
     }
     ```
   - Only three subcollections exist under `/users/{userId}`:
     - `/users/{userId}/cashbooks/{bookId}` (Lines 106-136)
     - `/users/{userId}/parties/{partyId}` (Lines 138-168)
     - `/users/{userId}/transactions/{txId}` (Lines 170-212)
   - There is NO rule allowing reads or writes to `/users/{userId}` directly.

2. **Strict 12-Key Whitelist for Transactions**:
   In `firestore.rules` lines 184-193:
   ```
   incoming().keys().hasAll([
     'userId', 'bookId', 'type', 'amount', 'remark',
     'category', 'paymentMode', 'partyId', 'partyName',
     'transactionDate', 'createdAt', 'updatedAt'
   ]) &&
   incoming().keys().hasOnly([
     'userId', 'bookId', 'type', 'amount', 'remark',
     'category', 'paymentMode', 'partyId', 'partyName',
     'transactionDate', 'createdAt', 'updatedAt'
   ])
   ```
   Lines 84-94 validate:
   - `data.userId == request.auth.uid && data.userId == userId`
   - `data.type in ['CASH_IN', 'CASH_OUT', 'TRANSFER']`
   - `data.amount > 0 && data.amount <= 1000000000`
   - `data.category.size() >= 1 && data.category.size() <= 60`
   - `data.paymentMode in ['Cash', 'Bank Transfer', 'UPI', 'Cheque', 'Card']`
   - `data.transactionDate is int && data.transactionDate > 0`
   - `data.createdAt is timestamp && data.createdAt <= request.time`
   - `data.updatedAt is timestamp && data.updatedAt <= request.time`
   - Update diff keys strictly limited to: `['type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'updatedAt']`.

3. **Current Desktop Sync Implementation in `E:\CashBookRelease\app\www\index.html`**:
   - Lines 5405-5413 contain a duplicate dummy simulation function:
     ```javascript
     function syncWithCloud() {
       const syncEl = document.querySelector('.sync-status');
       syncEl.innerHTML = '<i class="fa-solid fa-arrows-rotate fa-spin"></i> <span>Syncing Firestore...</span>';
       setTimeout(() => {
         syncEl.innerHTML = '<span class="sync-dot"></span><span id="sync-label">Cloud Live Synced</span>';
         alert('All local transactions, accounts, and party ledgers synced successfully with Cloud Database Vault!');
       }, 700);
     }
     ```
   - Lines 6092-6121 contain a second declaration of `syncWithCloud()`:
     ```javascript
     async function syncWithCloud(showToast = false) {
       // ...
       if (currentUserProfile && firestoreDb) {
         const uid = currentUserProfile.uid;
         const userDocRef = firestoreDb.collection('users').doc(uid);
         await userDocRef.set({
           email: currentUserProfile.email,
           displayName: currentUserProfile.displayName,
           lastSyncedAt: new Date().toISOString(),
           company: compProfile,
           transactionsCount: txnsList.length
         }, { merge: true });
       }
     }
     ```
   - Line 4741 checks for a non-existent function:
     ```javascript
     if (typeof syncTransactionToFirestore === 'function') {
       const savedTx = editingTxnId ? transactions.find(t => t.id === editingTxnId) : transactions[transactions.length - 1];
       if (savedTx) syncTransactionToFirestore(savedTx);
     }
     ```
     `syncTransactionToFirestore` is never declared anywhere in `index.html`.

4. **Local Data Structure in `index.html`**:
   - Lines 3298-3306 show local transaction objects containing:
     `['id', 'voucher', 'date', 'time', 'type', 'account', 'toAccount', 'party', 'category', 'mode', 'amount', 'status', 'desc', 'refNo']`.
   - Local `type` values are `'in'`, `'out'`, `'contra'`.
   - Local `mode` values are `'Cash'`, `'NEFT / RTGS'`, `'UPI / QR'`, `'Bank Cheque'`.

5. **Test Invariants Verified**:
   - Executing `node tests/run_all_tests.mjs`:
     All 125 test cases pass across Tier 1 (F1-F11), Tier 2 (B1-B11), Tier 3 (TC-C01 to TC-C10), and Tier 4 (Accounting Scenarios 1-5).
   - Invariants `INV-SYNC-01` to `INV-SYNC-06` mandate stripping `_dirty` and local fields before writing to Firestore, matching the 12-key whitelist, ensuring `cloudDoc.id === undefined` (document ID is the path segment, not in body), and restricting retries to 5.

---

## 2. Logic Chain

1. **Step 1 (Path Denial)**: Observation 1 establishes that Firestore rules deny all root-level writes to `/users/{userId}`. Observation 3 shows `index.html` attempting `firestoreDb.collection('users').doc(uid).set(...)`. Therefore, the existing desktop cloud sync is completely non-functional and blocked by security rules.
2. **Step 2 (Missing Entities)**: Observation 3 shows that neither `transactions` nor `parties` nor `cashbooks` are ever written or fetched in `syncWithCloud()`. Furthermore, Observation 3 shows that `syncTransactionToFirestore` is a dead call. Therefore, vouchers never leave or enter desktop storage.
3. **Step 3 (Schema Incompatibility)**: Observation 2 establishes that `/users/{userId}/transactions/{txId}` rejects any document containing keys other than the exact 12 whitelisted keys (`keys().hasOnly(...)`). Observation 4 demonstrates that local Desktop vouchers possess 14+ fields (including `voucher`, `time`, `account`, `desc`, `mode`, `status`). Direct persistence of local objects without transformation will result in immediate `PERMISSION_DENIED`.
4. **Step 4 (Enum Incompatibility)**: Observation 2 establishes that `type` must be `'CASH_IN' | 'CASH_OUT' | 'TRANSFER'` and `paymentMode` must be `'Cash' | 'Bank Transfer' | 'UPI' | 'Cheque' | 'Card'`. Observation 4 shows local vouchers use `'in'`, `'out'`, `'contra'` and `'NEFT / RTGS'`, `'UPI / QR'`, `'Bank Cheque'`. Thus, a bidirectional normalization layer is strictly mandatory for both upstream push and downstream pull.
5. **Step 5 (Resilience)**: Observation 5 establishes that offline queuing requires marking local items `_dirty: true`, storing operations in `cashbook_sync_queue`, stripping `_dirty` on commit, and capping exponential backoff retries at 5.

---

## 3. Caveats

1. **Google OAuth Token Dependency**: Firestore synchronization requires a non-null `firebase.auth().currentUser`. If Explorer M2-1's Google OAuth flow fails to populate a valid Firebase credential, Firestore calls will fail with `PERMISSION_DENIED` at the rule level.
2. **Multi-Book Scoping**: Desktop currently operates on a single active business register, whereas Android supports multi-book switching (`cashbooks/{bookId}`). The Desktop implementation should use `'default_book'` or dynamically create and link to `/users/{uid}/cashbooks/default_book`.
3. **No other caveats.**

---

## 4. Conclusion

1. The existing cloud sync implementation in `E:\CashBookRelease\app\www\index.html` is defective: it writes to an unauthorized path, ignores vouchers and parties, contains dead simulation code, and masks all errors.
2. Desktop synchronization must be replaced with the bidirectional synchronization engine specified in `analysis.md`:
   - Upstream: Clean 12-key payload construction via `toFirestoreTransactionPayload()` with strict stripping of `_dirty`, `id`, and local metadata.
   - Downstream: Pulling `/users/{uid}/transactions` and merging via `fromFirestoreTransactionDoc()`.
   - Offline Queue: Durable FIFO queue in `localStorage` with automatic flush on network reconnection and 5-retry exponential backoff limit.
   - Real-time UI indicator reflecting the 4 defined design-token-compliant states.

---

## 5. Verification Method

1. **Automated Unit & Invariant Verification**:
   Run the master test suite:
   ```powershell
   node 'g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs'
   ```
   Verify 100% pass (125/125 tests) across all 4 tiers, specifically verifying `INV-SYNC-01`, `INV-SYNC-02`, `INV-SYNC-04`, and `INV-SYNC-06`.

2. **Schema Whitelist Verification**:
   Inspect `tests/tier1_feature_coverage.test.mjs` lines 231-335 and `tests/tier2_boundary_corner.test.mjs` lines 201-301 to ensure all transaction payloads contain only the 12 whitelisted keys and respect string boundaries (remark <= 500, partyName <= 80, category 1..60).

3. **Code Inspection**:
   Inspect `E:\CashBookRelease\app\www\index.html` lines 5405-5413 and 6092-6121 to confirm removal of mock simulations and replacement with the bidirectional sync engine.
