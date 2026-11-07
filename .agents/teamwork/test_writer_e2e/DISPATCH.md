# Dispatch: E2E Testing Track Lead (teamwork_test_writer)
Task: Build opaque-box requirement-driven test infrastructure and test suite (Tiers 1-4) covering all features in PROJECT.md Feature Inventory. Publish TEST_INFRA.md and TEST_READY.md upon completion.
Scope:
- Tier 1: Feature Coverage (>=5 per feature)
- Tier 2: Boundary & Corner Cases (>=5 per feature)
- Tier 3: Cross-Feature Combinations (pairwise coverage)
- Tier 4: Real-World Accounting Scenarios
Target test directory: `g:\My Drive\project\Cash Book\.agents\teamwork\tests\`
Original Request: `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
Project Index: `g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md`
Specifications: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\spec_requirements.md`


## 2026-11-06T10:23:25Z
You are E2E Testing Track Lead (Test Writer).
Your working directory is: g:\My Drive\project\Cash Book\.agents\teamwork\test_writer_e2e\
You MUST read:
1. `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
2. `g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md`
3. `g:\My Drive\project\Cash Book\.agents\teamwork\test_writer_e2e\DISPATCH.md`
4. `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\spec_requirements.md`

Your objective:
Design and build the comprehensive E2E test suite (Tiers 1-4) in `g:\My Drive\project\Cash Book\.agents\teamwork\tests\`:
1. Design E2E Test Infrastructure:
   - Create `TEST_INFRA.md` at project root (`g:\My Drive\project\Cash Book\TEST_INFRA.md` or in `.agents/teamwork/TEST_INFRA.md`).
   - Implement an automated test runner script (e.g. Node.js or Python) that runs standalone, exercises and verifies both Visual styling invariants (no neon glows/AI gradients/unaligned numbers) and Auth/Sync invariants (real Google Sign-In, session persistence across restarts, Firestore schema compliance).
2. Generate Test Cases:
   - Tier 1: Feature Coverage (>=5 test cases per feature in PROJECT.md Feature Inventory F1-F11)
   - Tier 2: Boundary & Corner Cases (>=5 test cases per feature: extreme currency amounts, negative/contra values, long remarks, special chars, offline/network errors, missing fields)
   - Tier 3: Cross-Feature Combinations (pairwise coverage of features, e.g., Google Sign-In + Voucher Add + Offline cache + Sync + Session reload)
   - Tier 4: Real-World Accounting Scenarios (>=5 multi-voucher commercial day book workloads, cash flow calculations, Contra transfers, balance sheet checks)
3. Output `TEST_READY.md` summarizing runner commands, pass/fail semantics, and feature checklist.
4. Deliver reports to `g:\My Drive\project\Cash Book\.agents\teamwork\test_writer_e2e\handoff.md`.
Maintain `progress.md` in your directory and send a message when complete.
