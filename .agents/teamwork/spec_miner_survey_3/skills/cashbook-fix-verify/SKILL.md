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
