# DISPATCH — Worker M1-It3

## Mission
Apply the drop-in remediation chunks from `remediation_blueprint_it3.md` to `E:\CashBookRelease\app\www\index.html` and `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs`, mirror `index.html` to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`, and execute all verification test suites.

## Mandatory Inputs & Constraints
- Authoritative User Request: `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
- Authoritative Blueprint: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m1_it3_3\remediation_blueprint_it3.md`
- Dead Ends Log: `g:\My Drive\project\Cash Book\.agents\teamwork\DEAD_ENDS.md`
- Gate Status: `g:\My Drive\project\Cash Book\.agents\teamwork\orchestrator\GATE_STATUS.md`
- Exclusive Write Ownership:
  - `E:\CashBookRelease\app\www\index.html`
  - `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`
  - `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs`

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Tasks to Execute
1. Read `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m1_it3_3\remediation_blueprint_it3.md` § 4.
2. Apply Chunks 1 through 12 to `E:\CashBookRelease\app\www\index.html`:
   - Chunk 1: Pure entity encoding in `escapeHtml` (lines 2994–3007).
   - Chunk 2: Canonical `parseAmount` definition & `formatINR` pre-rounding (lines 3395–3403).
   - Chunk 3: Call Site 1 in `renderAccounts()` (line 3957).
   - Chunk 4: Call Site 2 in `renderReports()` (line 4001).
   - Chunk 5: Call Site 3 in `renderReports()` (line 4031).
   - Chunk 6: Call Sites 4 & 5 in `renderCharts()` (lines 4104–4105).
   - Chunk 7: Call Sites 6 & 7 in `renderCharts()` (lines 4119 & 4129).
   - Chunk 8: Call Site 8 in `renderCharts()` (line 4193).
   - Chunk 9: Call Site 9 & Contra self-transfer in `renderStatementTable()` (lines 4877–4890).
   - Chunk 10: Call Site 10 in `printSingleVoucher()` (line 5153).
   - Chunk 11: Call Site 11 in `printDayBook()` (line 5243).
   - Chunk 12: Call Site 12 in `exportMultiSheetExcel()` (line 5352).
3. Apply Chunk 13 to `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs` (lines 562–581).
4. Apply Chunk 14: Mirror `E:\CashBookRelease\app\www\index.html` to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html` using:
   ```powershell
   Copy-Item -Path "E:\CashBookRelease\app\www\index.html" -Destination "C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html" -Force
   ```
5. Run all 5 verification suites using `run_command`:
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_adversarial_cases.mjs"`
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs"`
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m1_it3_2\test_m1_it3_2_simulation.mjs"`
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_format_inr.mjs"`
   - `node "g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs"`
6. Document commands, outputs, and SHA256 checksums in `g:\My Drive\project\Cash Book\.agents\teamwork\worker_m1_it3\handoff.md`. Send completion message when done.

## 2026-11-06T11:50:04Z
[Message] timestamp=2026-11-06T11:50:04Z sender=e0fececc-c4b3-489c-b697-38e3390bcb51 priority=MESSAGE_PRIORITY_HIGH content=You are Worker M1-It3 for Milestone M1 Iteration 3 of the Cash Book project.
Your working directory is: g:\My Drive\project\Cash Book\.agents\teamwork\worker_m1_it3\
Read your task description in: g:\My Drive\project\Cash Book\.agents\teamwork\worker_m1_it3\DISPATCH.md
MANDATORY: Read the authoritative user request first at: g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md
Also read:
- g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m1_it3_3\remediation_blueprint_it3.md
- g:\My Drive\project\Cash Book\.agents\teamwork\DEAD_ENDS.md
- g:\My Drive\project\Cash Book\.agents\teamwork\orchestrator\GATE_STATUS.md

You have exclusive write ownership of:
- E:\CashBookRelease\app\www\index.html
- C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html
- g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs
