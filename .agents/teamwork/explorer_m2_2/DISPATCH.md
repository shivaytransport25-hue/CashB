# DISPATCH — Explorer M2-2

## Mission
Investigate the Cloud Firestore synchronization architecture in `E:\CashBookRelease\app\www\index.html`, audit strict compliance with `firestore.rules` 12-key whitelist for `/users/{uid}/transactions/{txId}`, and formulate the bidirectional sync engine for desktop.

## Mandatory Inputs & Context
- Authoritative User Request: `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
- Project Index: `g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md`
- Target Code: `E:\CashBookRelease\app\www\index.html`
- Firestore Security Rules: `firestore.rules` in workspace root
- Firebase Project: `gen-lang-client-0037390170`

## Objectives
1. Locate all Firestore sync functions in `index.html` (search for `firestore`, `collection`, `getDocs`, `setDoc`, `addDoc`, `onSnapshot`, `sync`, `upload`, `download`).
2. Audit the schema currently used in `index.html` against `firestore.rules`:
   - Target collection: `/users/{userId}/transactions/{transactionId}`
   - Exact 12 allowed keys:
     `['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`
   - Value constraints: `userId == request.auth.uid`, `type` in `['CASH_IN', 'CASH_OUT', 'CONTRA']`, `amount > 0`, `paymentMode` in `['CASH', 'BANK', 'ONLINE']`.
   - Cashbooks collection: `/users/{userId}/cashbooks/{bookId}`
   - Parties collection: `/users/{userId}/parties/{partyId}`
3. Analyze local data storage: where are vouchers and parties currently saved locally (`localStorage`, `indexedDB`, in-memory array `transactions`)?
4. Formulate bidirectional sync protocol:
   - Upload local changes (new/edited/deleted vouchers) to Firestore under the authenticated user's UID.
   - Download remote vouchers and merge with local ledger without duplicate entries.
   - Offline resilience: gracefully queue operations if offline and sync when connected.
   - UI sync indicator: show clean sync status dot and last synced timestamp in accordance with fintech daylight/dark design system.
5. Write detailed analysis to `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\analysis.md` and deliver handoff to `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\handoff.md`.

## 2026-11-06T12:07:22Z
[Message] sender=e0fececc-c4b3-489c-b697-38e3390bcb51
Investigate Cloud Firestore sync in `index.html`. Check current collection paths, data mapping, and audit strict compliance with `firestore.rules` 12-key whitelist for `/users/{uid}/transactions/{txId}`:
`['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`
and enum constraints (`CASH_IN`, `CASH_OUT`, `CONTRA`, etc.). Formulate the bidirectional synchronization engine and offline queueing.
Deliver your analysis to `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\analysis.md` and handoff report to `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\handoff.md`. Send a message when done.

## 2026-11-06T12:14:41Z
[Message] sender=57497ef5-ca5f-4437-94f5-731a8ea8db5d (Spec Miner M2-3)
**Context**: Coordinating on Milestone M2 technical blueprint synthesis
**Content**: Hello Explorer M2-2, I am Spec Miner M2-3. I am preparing the blueprint synthesis for Firestore Sync compliance with `firestore.rules`. I have analyzed the 12-key whitelist for `/users/{userId}/transactions/{txId}`, the 6-key whitelist for `/cashbooks`, the 7-key whitelist for `/parties`, enum value constraints (`CASH_IN`/`CASH_OUT`/`TRANSFER`, paymentModes), and `tier1_feature_coverage.test.mjs` test assertions. Please share your analysis/handoff when ready or let me know if you need any input.
**Action**: Notify when handoff.md is ready or share key findings.
