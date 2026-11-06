---
name: cashbook-fix-verify
description: 'Use when fixing Cash Book Pro bugs, implementing accounting features, validating money logic, or preparing a release. Covers investigation, safe implementation, Flutter verification, and clear reporting for financial data changes.'
argument-hint: 'Bug fix, data migration, release validation, or accounting workflow'
user-invocable: true
disable-model-invocation: false
---

# Cash Book Pro Fix and Verification Workflow

## When to Use
- Investigate a bug in the Flutter app, Firestore sync layer, or SQLite data model
- Implement or adjust accounting rules, transfers, reversals, or reports
- Validate money math, date handling, balances, and audit behavior before release
- Sanity-check a feature or release package before shipping

## Core Principles
1. Read the relevant code before changing anything.
2. Reproduce the issue or locate the exact calculation path before fixing it.
3. Keep changes small, clear, and reversible.
4. Treat user financial data as protected information; backups and rollback plans are required for risky changes.
5. Verify with real commands and test output; never claim success without evidence.
6. End with a concise summary of what changed and what remains unverified.

## Procedure

### 1. Write the task brief
Before coding, state:
- Goal
- Issue type (bug, feature, release, data migration, audit fix)
- Effort and risk level
- Known facts from the repo or user request
- The exact behavior that counts as done
- A short implementation plan

For any risky data change, stop and confirm the plan before moving forward.

### 2. Gather evidence
Use the actual code paths and project context to answer:
- Which file owns the business logic?
- Is the issue in Flutter UI, accounting logic, SQLite, or Firestore sync?
- Are amounts stored as integer paise, dates in UTC, and transfers as linked entries?

Do not guess file names or APIs while editing.

### 3. Reproduce and isolate
- Verify the failing path with the smallest test or app flow available.
- Confirm whether the root cause is UI-only or a deeper accounting/data issue.
- Capture the exact wrong behavior before changing code.

### 4. Implement one root-cause fix
- Keep the patch narrow and aligned with existing project patterns.
- Do not mix unrelated cleanup into the same change.
- Preserve accounting integrity, audit trails, and multi-device sync assumptions.
- For money logic, prefer integer-based calculations over float-based operations.

### 5. Validate with real checks
Run the smallest relevant verification that proves the fix:
- targeted unit tests for financial engines or transaction rules
- flutter test for impacted domain behavior
- build or lint checks if the change touches runtime or release paths
- manual sanity check for the user-facing flow if automation is not enough

For financial logic, explicitly confirm:
- totals match ledger entries
- account transfers stay balanced
- reversal/audit records remain consistent
- no floating-point rounding regression appears

### 6. Report the result honestly
Include:
- what changed
- why it changed
- what was verified with real output
- any remaining unverified risks or follow-up steps

Never say a fix is complete unless the verification actually passed.

## Decision Points
- If the issue is in money math: validate paise precision and balance calculations before shipping.
- If the issue involves data migration, sync rules, or deletion: require a backup and rollback plan.
- If the bug appears only in UI: still trace the underlying data path and avoid masking a deeper issue.
- If evidence is missing: do not assume the cause; investigate one more layer before editing.

## Completion Criteria
A task is complete when all of the following are true:
- Root cause is identified and addressed
- Changes stay within the affected scope
- Relevant tests or checks have real output and pass
- Financial integrity rules remain intact
- The report states what was verified and what was not

## Cash Book Project Checks
Use these checks whenever working in this repo:
- Confirm amounts are stored and calculated in integer paise, not floating point
- Validate transfer, debit, and credit integrity for linked transactions
- Check reversal and audit trail behavior before finalizing a fix
- Review Firestore or local SQLite assumptions for offline-first sync
- Verify the relevant app/build path still works for the target platform

## Example Prompts to Use This Skill
- Fix the balance mismatch in the cash book report.
- Investigate why a transfer is creating a duplicate ledger entry.
- Validate the paise conversion logic before the release build.
- Review the sync queue logic for a stale Firestore update.
- Prepare a safe release check for the Android/iOS build.
