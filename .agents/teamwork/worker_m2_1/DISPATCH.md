# DISPATCH — Worker M2-1

## Mission
Implement Desktop Google Sign-In, Session Persistence, and Bidirectional Cloud Firestore Synchronization in `E:\CashBookRelease\app\www\index.html` according to the authoritative blueprint `desktop_auth_sync_blueprint.md`. Eradicate all mock fallbacks (`usr_google_rajesh_patel`), mirror to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`, and verify all test suites.

## Mandatory Inputs & Constraints
- Authoritative User Request: `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
- Authoritative Blueprint: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md`
- Dead Ends Log: `g:\My Drive\project\Cash Book\.agents\teamwork\DEAD_ENDS.md`
- Project Index: `g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md`
- Exclusive Write Ownership:
  - `E:\CashBookRelease\app\www\index.html`
  - `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Tasks to Execute
1. Read `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md` thoroughly (especially § 2, 4, 5, 6, 7, 8, and 10).
2. Purge obsolete dummy `syncWithCloud()` at lines 5405–5413 of `E:\CashBookRelease\app\www\index.html`.
3. Implement `syncTransactionToFirestore(savedTx, action)` and hook into voucher save/delete.
4. Replace the Auth & Sync block (around lines 5886–6121):
   - Configure Firebase Auth with `setPersistence(firebase.auth.Auth.Persistence.LOCAL)`.
   - Setup `onAuthStateChanged` listener managing user session, fast-boot cache (`cashbook_auth_user`), and 55-minute silent token refresh (`F4.3`).
   - Implement authentic Google OAuth credential redemption via `signInWithCredential()` using Web Client ID `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`.
   - Permanently eradicate `usr_google_rajesh_patel` (INV-AUTH-01) from line 5980 and all dummy fallbacks. On cancellation or failure, keep user on login gate; NEVER inject mock profiles.
   - Implement `toFirestoreTransactionPayload()` strictly outputting the 12 whitelisted keys and normalized enums (`CASH_IN`, `CASH_OUT`, `TRANSFER`, `Cash`, `Bank Transfer`, `UPI`, `Cheque`, `Card`).
   - Implement `fromFirestoreTransactionDoc()` downstream normalizer.
   - Implement `toFirestoreCashBookPayload()` (6 keys) and `toFirestorePartyPayload()` (7 keys).
   - Implement real `syncWithCloud(showToast)`: push dirty transactions and pull remote transactions from `/users/{uid}/transactions`.
   - Implement `processSyncQueue()` with FIFO queue, exponential backoff, and 5-retry limit (`B11.5`).
   - Implement `updateSyncStatusBadge(state)` using M1 enterprise fintech design tokens.
5. Mirror `E:\CashBookRelease\app\www\index.html` to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html` using:
   ```powershell
   Copy-Item -Path "E:\CashBookRelease\app\www\index.html" -Destination "C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html" -Force
   ```
6. Run all verification test suites:
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs"`
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs"`
   - `powershell -Command "Select-String -Path 'E:\CashBookRelease\app\www\index.html' -Pattern 'usr_google_rajesh_patel'"` (Must be 0)
7. Document all commands, code changes, and SHA256 hashes in `g:\My Drive\project\Cash Book\.agents\teamwork\worker_m2_1\handoff.md` and send a completion message.


## 2026-11-06T12:22:16Z
[Message] timestamp=2026-11-06T12:22:16Z sender=e0fececc-c4b3-489c-b697-38e3390bcb51 priority=MESSAGE_PRIORITY_HIGH content=You are Worker M2-1 for Milestone M2 (Desktop Google Sign-In & Firestore Sync).
Your working directory is: g:\My Drive\project\Cash Book\.agents\teamwork\worker_m2_1\
Read your task description in: g:\My Drive\project\Cash Book\.agents\teamwork\worker_m2_1\DISPATCH.md
MANDATORY: Read the authoritative user request first at: g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md
Also read:
- g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md
- g:\My Drive\project\Cash Book\.agents\teamwork\DEAD_ENDS.md
- g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md

You have exclusive write ownership of:
- E:\CashBookRelease\app\www\index.html
- C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html
