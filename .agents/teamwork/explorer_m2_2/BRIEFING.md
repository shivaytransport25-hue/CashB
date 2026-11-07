# BRIEFING — 2026-11-06T12:15:00Z

## Mission
Investigate Cloud Firestore sync in `index.html`, audit strict compliance with `firestore.rules` 12-key whitelist for `/users/{uid}/transactions/{txId}`, and formulate bidirectional sync and offline queueing.

## 🔒 My Identity
- Archetype: explorer
- Roles: explorer, analyst
- Working directory: g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\
- Original parent: e0fececc-c4b3-489c-b697-38e3390bcb51
- Milestone: M2 (Desktop Google Sign-In & Firestore Sync)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Audit strict compliance with firestore.rules 12-key whitelist for /users/{uid}/transactions/{txId}
- Formulate bidirectional sync protocol & offline queueing for desktop

## Current Parent
- Conversation ID: e0fececc-c4b3-489c-b697-38e3390bcb51
- Updated: 2026-11-06T12:07:22Z

## Investigation State
- **Explored paths**:
  - `g:\My Drive\project\Cash Book\firestore.rules`
  - `E:\CashBookRelease\app\www\index.html` (lines 1-50, 540-575, 1695-1730, 2340-2380, 3290-3355, 4535-4555, 4690-4810, 4830-4865, 5395-5430, 5840-6146)
  - `G:\My Drive\project\Cash Book\app\src\main\java\com\example\data\model\LedgerModels.kt`
  - `G:\My Drive\project\Cash Book\app\src\main\java\com\example\data\repository\CashBookRepository.kt`
  - `G:\My Drive\project\Cash Book\cash_book_system\lib\database\firestore_ledger_schema.dart`
  - `g:\My Drive\project\Cash Book\.agents\teamwork\tests\` (all tiers 1-4, test_helpers.mjs, run_all_tests.mjs)
  - `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\spec_requirements.md`
- **Key findings**:
  - `firestore.rules` has a global catch-all deny and only 3 subcollections under `/users/{userId}`:
    1. `/users/{userId}/cashbooks/{bookId}` (6-key whitelist: `['userId', 'name', 'description', 'currencySymbol', 'createdAt', 'updatedAt']`)
    2. `/users/{userId}/parties/{partyId}` (7-key whitelist: `['userId', 'name', 'phone', 'role', 'notes', 'createdAt', 'updatedAt']`, role in `['Customer', 'Supplier', 'Staff']`)
    3. `/users/{userId}/transactions/{txId}` (12-key whitelist: `['userId', 'bookId', 'type', 'amount', 'remark', 'category', 'paymentMode', 'partyId', 'partyName', 'transactionDate', 'createdAt', 'updatedAt']`)
  - Existing `index.html` sync is critically broken:
    - Attempted to write to `/users/{uid}` directly, which is blocked by security rules.
    - Zero transactions, parties, or cashbooks were ever synced to Firestore.
    - Two conflicting declarations of `syncWithCloud()` (line 5405 and line 6092).
    - Unimplemented placeholder `syncTransactionToFirestore` checked at line 4741.
    - Local transactions contain ~14 fields that violate the 12-key whitelist if passed unmapped.
    - Payment mode enum and transaction type enum mismatches require normalization.
  - Complete test invariants validated against Tiers 1-4 (INV-SYNC-01 to INV-SYNC-06).
- **Unexplored areas**: None within scope of M2-2.

## Key Decisions Made
- Fully specified the two-way transformation layer between local rich voucher models and Firestore 12-key canonical documents.
- Designed durable offline queue (`cashbook_sync_queue`) with exponential backoff (max 5 retries per B11.5).
- Designed UI sync status indicator states adhering to M1 design system tokens.

## Artifact Index
- DISPATCH.md — Task instructions and updates
- BRIEFING.md — Agent situational memory
- progress.md — Liveness heartbeat
- analysis.md — Full deep-dive analysis and architectural formulation
- handoff.md — 5-component handoff report
