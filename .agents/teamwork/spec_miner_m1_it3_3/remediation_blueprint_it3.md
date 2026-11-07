# REMEDIATION BLUEPRINT — Milestone M1 Iteration 3
**Desktop Visual Styling & Ledger Overhaul: Authoritative Drop-In Blueprint**

**Author**: Spec Miner M1-It3-3  
**Target Consumer**: Worker M1-It3 (`teamwork_worker`)  
**Target Code**: `E:\CashBookRelease\app\www\index.html` & `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`  
**Test Harness**: `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs`  
**Date**: 2026-11-06  
**Status**: APPROVED & READY FOR DROP-IN EXECUTION  

---

## 1. Executive Summary & Specification Scope

In Milestone M1 Iteration 2, reviewer `reviewer_m1_it2_2` issued a `REQUEST_CHANGES` verdict documented in `GATE_STATUS.md`. This specification synthesizes the empirical evidence, root-cause analyses, and simulation test harnesses delivered by **Explorer M1-It3-1** (`explorer_m1_it3_1\handoff.md`) and **Explorer M1-It3-2** (`explorer_m1_it3_2\handoff.md`).

This blueprint provides an unambiguous, turn-key, drop-in specification for Worker M1-It3 covering all five required remediation areas:
1. **Pure Entity Encoding in `escapeHtml`**: Complete removal of the destructive regex shortcut (`/<\/?(?:td|tr...)/ -> return '';`) that silently erased legitimate logistics terms like `<truck-12>`, `<transport>`, `<transit>`, `<trip-5>`, and `<tracking>`. Standard 5-character entity encoding neutralizes table breakout and script injection while preserving 100% of user data.
2. **Canonical `parseAmount(val)` Centralization**: Global definition of `window.parseAmount` and deployment across all 12 remaining unpatched `parseFloat(t.amount)` call sites, permanently eradicating the comma-truncation bug (`"1,00,000.00"` -> `1` -> `"Rupees One Only"`) logged in `DEAD_ENDS.md`.
3. **Contra Self-Transfer Decoupling in `renderStatementTable`**: Decoupling `else if (t.account === entityName)` into an independent `if` statement with floating-point precision rounding, ensuring self-transfers (`Cash in Hand -> Cash in Hand`) net out to zero change in running balance.
4. **Negative Zero Elimination in `formatINR`**: IEEE 754 pre-rounding `Math.round((num + Number.EPSILON) * 100) / 100` before sign evaluation, preventing small negative residuals (`-0.001`) from displaying as `-₹ 0.00`.
5. **`stress_test_ui.mjs` Test Assertion Compatibility**: Updating Check 3 to verify the absence of raw unescaped table tags (`<td colspan="11">` or `</td></tr><tr><td`) and row count integrity, replacing the flawed string search for the user text `'ROGUE ROW'`.

---

## 2. Features Discovered & Remediation Specifications

### Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Sanitization | Pure Entity Encoding `escapeHtml` | Encodes `&`, `<`, `>`, `"`, `'` into HTML character entities without stripping or deleting user content | Any string or value (e.g. `<truck-12>`, `Sharma <vip>`) | Escaped string with entities (`&lt;truck-12&gt;`) | If `null`/`undefined`, returns `''`; never throws | Explorer M1-It3-1 & `test_adversarial_cases.mjs` |
| 2 | Number Parsing | Canonical `parseAmount` | Strips commas from formatted strings, handles numbers, null, undefined, and returns positive absolute float | String/number/null/undefined (`"1,00,000.00"`, `null`, `5000`) | Absolute float >= 0 (`100000`, `0`, `5000`) | Returns `0` on invalid/empty/NaN; never throws | Explorer M1-It3-2 & `DEAD_ENDS.md` |
| 3 | Number Formatting | Negative Zero Pre-Rounding `formatINR` | Pre-rounds value to 2 decimal places prior to sign test, avoiding `-₹ 0.00` | Number or string (`-0.001`, `100000`, `-40000`) | Formatted INR string (`₹ 0.00`, `₹ 1,00,000.00`, `-₹ 40,000.00`) | Falls back to `₹ 0.00` on invalid input | Explorer M1-It3-2 & `GATE_STATUS.md` |
| 4 | Ledger Calculation | Decoupled Contra Statement Table | Independent sequential checks for destination debit and source credit on contra entries | Transaction list, entity name, account flag | Correctly rendered table rows with balanced running ledger | Voided rows skipped; self-transfers net to 0 change | Explorer M1-It3-2 & `renderStatementTable` |
| 5 | Test Verification | Structural Table Breakout Verification | Verifies DOM structural integrity by testing for raw HTML tag injection rather than user text | Day Book generated HTML string | Pass if no raw `<td` / `<tr` tags and row count equals voucher count | Fails if table structure broken or raw tags injected | Explorer M1-It3-1 & `stress_test_ui.mjs` |

---

## 3. Edge Cases & Empirical Observations

| # | Feature | Input | Observed Behavior (Before Fix) | Observed Behavior (After Blueprint Fix) |
|---|---------|-------|--------------------------------|------------------------------------------|
| E1 | `escapeHtml` | `'Agrawal Transport <transport@gmail.com>'` | `""` (DATA LOSS: wiped out by regex `<tr...`) | `"Agrawal Transport &lt;transport@gmail.com&gt;"` (100% data preserved) |
| E2 | `escapeHtml` | `'Payment for truck <truck-12>'` | `""` (DATA LOSS: wiped out by regex `<tr...`) | `"Payment for truck &lt;truck-12&gt;"` (100% data preserved) |
| E3 | `escapeHtml` | `'In transit <transit>'` | `""` (DATA LOSS: wiped out by regex `<tr...`) | `"In transit &lt;transit&gt;"` (100% data preserved) |
| E4 | `escapeHtml` | `'trip <trip-5>'` | `""` (DATA LOSS: wiped out by regex `<tr...`) | `"trip &lt;trip-5&gt;"` (100% data preserved) |
| E5 | `escapeHtml` | `'Diesel <tracking>'` | `""` (DATA LOSS: wiped out by regex `<tr...`) | `"Diesel &lt;tracking&gt;"` (100% data preserved) |
| E6 | `escapeHtml` | `'</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>'` | `""` (Artificial test shortcut) | `"&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td colspan=&quot;11&quot;&gt;ROGUE ROW&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;"` (Harmless text in cell) |
| E7 | `parseAmount` | `'1,00,000.00'` | `1` (Truncated at first comma) | `100000` (Parsed cleanly as full float) |
| E8 | `parseAmount` | `'1,23,45,678.90'` | `1` (Truncated at first comma) | `12345678.9` (Multi-lakh/crore string parsed cleanly) |
| E9 | `parseAmount` | `'-50,000.25'` | `-50` (Truncated) | `50000.25` (Negative stripped via `Math.abs`) |
| E10 | `parseAmount` | `null`, `undefined`, `""`, `NaN`, `'invalid'` | Unhandled / NaN | `0` (Safe fallback) |
| E11 | `formatINR` | `-0.00001`, `-0.001`, `-0.0049` | `"-₹ 0.00"` (Invalid negative zero) | `"₹ 0.00"` (Pre-rounded cleanly to positive zero) |
| E12 | `formatINR` | `-0.0051`, `-0.01` | `"-₹ 0.01"` | `"-₹ 0.01"` (True cents retained) |
| E13 | Contra Self-Transfer | Contra: `Cash in Hand -> Cash in Hand` (₹10,000) | Running bal: ₹25,000 -> ₹35,000 (+₹10,000 artificial inflation) | Running bal: ₹25,000 -> ₹25,000 (Debit 10k, Credit 10k, net 0 change) |
| E14 | Voucher Printing | Voucher amount `'1,00,000.00'` | Printed words: `"Rupees One Only"` (DEAD END) | Printed words: `"Rupees One Lakh Only"` (Accurate) |

---

## 4. Drop-In Code Replacement Chunks for Worker M1-It3

Worker M1-It3 must apply the following chunks to `E:\CashBookRelease\app\www\index.html` and `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs`, then mirror `index.html` to `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`.

### Chunk 1: `escapeHtml` Pure Entity Encoding
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 2994–3007
- **Description**: Remove destructive regex `return '';` shortcut and preserve full string content using pure HTML entity encoding.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 2994–3007)
    function escapeHtml(str) {
      if (str === null || str === undefined) return '';
      // Neutralize raw HTML table injection payloads if injected into table cells
      if (typeof str === 'string' && /<\/?(?:td|tr|table|tbody|thead)[^>]*>/i.test(str)) {
        return '';
      }
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }
    window.escapeHtml = escapeHtml;
// ========== REPLACEMENT CONTENT
    function escapeHtml(str) {
      if (str === null || str === undefined) return '';
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }
    window.escapeHtml = escapeHtml;
// >>>>>>>>>>
```

---

### Chunk 2: Canonical `parseAmount` Definition & `formatINR` Pre-Rounding
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 3395–3403
- **Description**: Define canonical `window.parseAmount(val)` and pre-round in `formatINR` to eradicate negative zero `-₹ 0.00`.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 3395–3403)
    // Number formatting in Indian currency style
    function formatINR(val) {
      const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
      const num = parseFloat(cleanVal) || 0;
      const isNeg = num < 0;
      const absVal = Math.abs(num);
      return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
// ========== REPLACEMENT CONTENT
    // Canonical amount parser: strips commas, handles string/num/null/undefined, returns absolute float >= 0
    function parseAmount(val) {
      if (val === null || val === undefined) return 0;
      const clean = typeof val === 'string' ? val.replace(/,/g, '') : val;
      return Math.abs(parseFloat(clean) || 0);
    }
    window.parseAmount = parseAmount;

    // Number formatting in Indian currency style
    function formatINR(val) {
      const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
      const num = parseFloat(cleanVal) || 0;
      const rounded = Math.round((num + Number.EPSILON) * 100) / 100;
      const isNeg = rounded < 0;
      const absVal = Math.abs(rounded);
      return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    window.formatINR = formatINR;
// >>>>>>>>>>
```

---

### Chunk 3: Call Site 1 in `renderAccounts()`
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 3955–3958
- **Description**: Replace raw `parseFloat(t.amount)` with `parseAmount(t.amount)`.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 3955–3958)
        transactions.forEach(t => {
          if (t.status === 'voided') return;
          const amt = parseFloat(t.amount) || 0;
          if (t.account === a.name) {
// ========== REPLACEMENT CONTENT
        transactions.forEach(t => {
          if (t.status === 'voided') return;
          const amt = parseAmount(t.amount);
          if (t.account === a.name) {
// >>>>>>>>>>
```

---

### Chunk 4: Call Site 2 in `renderReports()` (Category Breakdown)
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 3999–4003
- **Description**: Replace raw `parseFloat(t.amount)` with `parseAmount(t.amount)`.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 3999–4003)
      transactions.forEach(t => {
        if (t.status === 'voided' || t.type !== 'out') return;
        const amt = parseFloat(t.amount) || 0;
        catMap[t.category] = (catMap[t.category] || 0) + amt;
// ========== REPLACEMENT CONTENT
      transactions.forEach(t => {
        if (t.status === 'voided' || t.type !== 'out') return;
        const amt = parseAmount(t.amount);
        catMap[t.category] = (catMap[t.category] || 0) + amt;
// >>>>>>>>>>
```

---

### Chunk 5: Call Site 3 in `renderReports()` (Monthly Cash Flow)
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 4029–4033
- **Description**: Replace raw `parseFloat(t.amount)` with `parseAmount(t.amount)`.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 4029–4033)
        transactions.forEach(t => {
          if (t.status === 'voided' || !t.date.startsWith(prefix)) return;
          const amt = parseFloat(t.amount) || 0;
          if (t.type === 'in') mIn += amt;
// ========== REPLACEMENT CONTENT
        transactions.forEach(t => {
          if (t.status === 'voided' || !t.date.startsWith(prefix)) return;
          const amt = parseAmount(t.amount);
          if (t.type === 'in') mIn += amt;
// >>>>>>>>>>
```

---

### Chunk 6: Call Sites 4 & 5 in `renderCharts()` (7-day Inflow & Outflow)
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 4102–4107
- **Description**: Replace raw `parseFloat(t.amount)` with `parseAmount(t.amount)` for dayIn and dayOut.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 4102–4107)
            transactions.forEach(t => {
              if (t.status === 'voided' || t.date !== dateStr) return;
              if (t.type === 'in') dayIn += parseFloat(t.amount) || 0;
              if (t.type === 'out') dayOut += parseFloat(t.amount) || 0;
            });
// ========== REPLACEMENT CONTENT
            transactions.forEach(t => {
              if (t.status === 'voided' || t.date !== dateStr) return;
              if (t.type === 'in') dayIn += parseAmount(t.amount);
              if (t.type === 'out') dayOut += parseAmount(t.amount);
            });
// >>>>>>>>>>
```

---

### Chunk 7: Call Sites 6 & 7 in `renderCharts()` (Monthly FY Inflow & Outflow)
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 4116–4133
- **Description**: Replace raw `parseFloat(t.amount)` with `parseAmount(t.amount)` in monthly FY mappings.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 4116–4133)
          inData = monthPrefixes.map(prefix => {
            let mIn = 0;
            transactions.forEach(t => {
              if (t.status !== 'voided' && t.date.startsWith(prefix) && t.type === 'in') {
                mIn += parseFloat(t.amount) || 0;
              }
            });
            return mIn;
          });

          outData = monthPrefixes.map(prefix => {
            let mOut = 0;
            transactions.forEach(t => {
              if (t.status !== 'voided' && t.date.startsWith(prefix) && t.type === 'out') {
                mOut += parseFloat(t.amount) || 0;
              }
            });
            return mOut;
          });
// ========== REPLACEMENT CONTENT
          inData = monthPrefixes.map(prefix => {
            let mIn = 0;
            transactions.forEach(t => {
              if (t.status !== 'voided' && t.date.startsWith(prefix) && t.type === 'in') {
                mIn += parseAmount(t.amount);
              }
            });
            return mIn;
          });

          outData = monthPrefixes.map(prefix => {
            let mOut = 0;
            transactions.forEach(t => {
              if (t.status !== 'voided' && t.date.startsWith(prefix) && t.type === 'out') {
                mOut += parseAmount(t.amount);
              }
            });
            return mOut;
          });
// >>>>>>>>>>
```

---

### Chunk 8: Call Site 8 in `renderCharts()` (Crokal Donut Categories)
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 4191–4195
- **Description**: Replace raw `parseFloat(t.amount)` with `parseAmount(t.amount)` in donut category grouping.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 4191–4195)
          transactions.forEach(t => {
            if (t.status === 'voided' || t.type !== 'out') return;
            catMap[t.category] = (catMap[t.category] || 0) + (parseFloat(t.amount) || 0);
          });
// ========== REPLACEMENT CONTENT
          transactions.forEach(t => {
            if (t.status === 'voided' || t.type !== 'out') return;
            catMap[t.category] = (catMap[t.category] || 0) + parseAmount(t.amount);
          });
// >>>>>>>>>>
```

---

### Chunk 9: Call Site 9 & Contra Self-Transfer in `renderStatementTable()`
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 4877–4890
- **Description**: Replace `parseFloat(t.amount)` with `parseAmount(t.amount)`, decouple `else if (t.account === entityName)` into `if (t.account === entityName)`, and add IEEE 754 precision rounding.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 4877–4890)
      const rows = sorted.map(t => {
        const amt = parseFloat(t.amount) || 0;
        let debit = 0, credit = 0;

        if (isAccount) {
          if (t.status !== 'voided') {
            if (t.type === 'in' && t.account === entityName) { debit = amt; running += amt; }
            else if (t.type === 'out' && t.account === entityName) { credit = amt; running -= amt; }
            else if (t.type === 'contra') {
              if (t.toAccount === entityName) { debit = amt; running += amt; }
              else if (t.account === entityName) { credit = amt; running -= amt; }
            }
          }
        } else {
// ========== REPLACEMENT CONTENT
      const rows = sorted.map(t => {
        const amt = parseAmount(t.amount);
        let debit = 0, credit = 0;

        if (isAccount) {
          if (t.status !== 'voided') {
            if (t.type === 'in' && t.account === entityName) { debit = amt; running += amt; }
            else if (t.type === 'out' && t.account === entityName) { credit = amt; running -= amt; }
            else if (t.type === 'contra') {
              if (t.toAccount === entityName) { debit = amt; running += amt; }
              if (t.account === entityName) { credit = amt; running -= amt; }
            }
            running = Math.round((running + Number.EPSILON) * 100) / 100;
          }
        } else {
// >>>>>>>>>>
```

---

### Chunk 10: Call Site 10 in `printSingleVoucher()`
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 5150–5155
- **Description**: Replace `parseFloat(t.amount)` with `parseAmount(t.amount)` so word conversion does not truncate comma strings.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 5150–5155)
      const t = transactions.find(x => x.id === id);
      if (!t) return;

      const amt = parseFloat(t.amount) || 0;
      const words = numberToWordsINR(amt);
// ========== REPLACEMENT CONTENT
      const t = transactions.find(x => x.id === id);
      if (!t) return;

      const amt = parseAmount(t.amount);
      const words = numberToWordsINR(amt);
// >>>>>>>>>>
```

---

### Chunk 11: Call Site 11 in `printDayBook()`
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 5240–5245
- **Description**: Replace `parseFloat(t.amount)` with `parseAmount(t.amount)`.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 5240–5245)
      const withBal = getTransactionsWithRunningBal(transactions);

      const rows = withBal.map(t => {
        const amt = parseFloat(t.amount) || 0;
        const isContra = t.type === 'contra';
// ========== REPLACEMENT CONTENT
      const withBal = getTransactionsWithRunningBal(transactions);

      const rows = withBal.map(t => {
        const amt = parseAmount(t.amount);
        const isContra = t.type === 'contra';
// >>>>>>>>>>
```

---

### Chunk 12: Call Site 12 in `exportMultiSheetExcel()`
- **Target File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 5349–5354
- **Description**: Replace `parseFloat(t.amount)` with `parseAmount(t.amount)`.

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 5349–5354)
      // Sheet 2: Category Expense Breakdown
      const catMap = {};
      transactions.forEach(t => {
        if (t.status === 'voided' || t.type !== 'out') return;
        catMap[t.category] = (catMap[t.category] || 0) + (parseFloat(t.amount) || 0);
      });
// ========== REPLACEMENT CONTENT
      // Sheet 2: Category Expense Breakdown
      const catMap = {};
      transactions.forEach(t => {
        if (t.status === 'voided' || t.type !== 'out') return;
        catMap[t.category] = (catMap[t.category] || 0) + parseAmount(t.amount);
      });
// >>>>>>>>>>
```

---

### Chunk 13: Test Assertion Alignment in `stress_test_ui.mjs`
- **Target File**: `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs`
- **Location**: Lines 562–581
- **Description**: Replace brittle `'ROGUE ROW'` substring search with structural DOM validation (absence of raw unescaped table tags and row expansion).

```javascript
// <<<<<<<<<< TARGET CONTENT (Lines 562–581)
  // Check 3: Did table breakout payload corrupt the table structure?
  const breakoutCheck = analyzeTableRow(daybookHtml);
  if (daybookHtml.includes('ROGUE ROW')) {
    // Check if ROGUE ROW broke the column count
    const rows = daybookHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
    let rogueFound = false;
    for (const r of rows) {
      if (r.includes('ROGUE ROW')) {
        rogueFound = true;
        const analysis = analyzeTableRow(r);
        fail('Table Breakout Injection Resilience', `Party string successfully injected rogue table row with ${analysis.totalCols} cols`, 'HIGH');
      }
    }
    if (!rogueFound) {
      fail('Table Breakout Injection Resilience', 'Table breakout payload present in HTML', 'HIGH');
    }
  } else {
    pass('Table Breakout Injection Resilience', 'Table breakout markup neutralized');
  }
// ========== REPLACEMENT CONTENT
  // Check 3: Did table breakout payload corrupt the table structure?
  const rows = daybookHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
  const hasRawBreakout = daybookHtml.includes('<td colspan="11">') || daybookHtml.includes('</td></tr><tr><td');
  const hasExtraRows = rows.length > attackVouchers.length;

  if (hasRawBreakout || hasExtraRows) {
    fail('Table Breakout Injection Resilience', `Party string broke table structure: raw table tags injected or rows expanded to ${rows.length}`, 'HIGH');
  } else if (daybookHtml.includes('&lt;td colspan=&quot;11&quot;&gt;') || daybookHtml.includes('&lt;/td&gt;&lt;/tr&gt;')) {
    pass('Table Breakout Injection Resilience', 'Table breakout markup safely converted to HTML entities without DOM injection');
  } else {
    pass('Table Breakout Injection Resilience', 'Table breakout markup neutralized');
  }
// >>>>>>>>>>
```

---

### Chunk 14: Mirroring Command for Worker M1-It3
After applying Chunks 1–12 to `E:\CashBookRelease\app\www\index.html`, Worker M1-It3 MUST mirror the file to the installed desktop location to ensure Test Suite 1 (SHA256 Checksum Parity) passes:

```powershell
Copy-Item -Path "E:\CashBookRelease\app\www\index.html" -Destination "C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html" -Force
```

---

## 5. Verification Protocol for Reviewer & Auditor

Following execution by Worker M1-It3, the changes can be independently verified via the following automated commands:

1. **Adversarial Sanitization Verification (Zero Data Loss)**:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_adversarial_cases.mjs"
   ```
   *Expected Output*: All 8 test cases output non-empty strings. `<transport@gmail.com>`, `<truck-12>`, `<transit>`, `<trip-5>`, `<tracking>` are preserved as `&lt;...&gt;`.

2. **Empirical UI Stress Test Suite**:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs"
   ```
   *Expected Output*:
   - Suite 1: SHA256 Checksum Identity PASS
   - Suite 2: HTML Structural Integrity & Tag Balance PASS
   - Suite 3: Rapid Account Switching & Column Count Invariants PASS
   - Suite 4: Character Escaping & HTML Injection Stress Test (all 8 checks PASS)
   - Total results: `Passed: 14 | Failed: 0`, Recommended Verdict: `APPROVE`.

3. **Numeric & Currency Edge Cases**:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m1_it3_2\test_m1_it3_2_simulation.mjs"
   ```
   *Expected Output*: 100% PASS across all 5 test suites.

4. **Master E2E Test Suite Runner (Tiers 1–4)**:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\tests\run_all_tests.mjs"
   ```
   *Expected Output*: All 125 tests pass (100%).
