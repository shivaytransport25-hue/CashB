# Handoff Report — Explorer M1-It3-1

**Milestone:** M1 Iteration 3  
**Role:** Teamwork Explorer (Read-Only Investigation)  
**Target:** `escapeHtml` sanitization & `stress_test_ui.mjs` test alignment  

---

## 1. Observation

1. **Destructive Regex Shortcut in `index.html`:**  
   In `E:\CashBookRelease\app\www\index.html` (lines 2994–3007) and installed `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html` (lines 2994–3007):
   ```javascript
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
   ```
   Directly observed:
   - Line 2997 regex `/<\/?(?:td|tr|table|tbody|thead)[^>]*>/i` lacks word boundaries `\b` after `tr` and `table`.
   - Line 2998 executes `return '';`, returning an empty string and discarding input.

2. **Adversarial Test Data Destruction:**  
   Running `node "g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_adversarial_cases.mjs"` yielded:
   ```
   Input:  Agrawal Transport <transport@gmail.com>
   Output: ""
   ---
   Input:  Payment for truck <truck-12>
   Output: ""
   ---
   Input:  In transit <transit>
   Output: ""
   ---
   Input:  trip <trip-5>
   Output: ""
   ---
   Input:  Diesel <tracking>
   Output: ""
   ---
   Input:  Regular Party Name
   Output: "Regular Party Name"
   ---
   Input:  <script>alert(1)</script>
   Output: "&lt;script&gt;alert(1)&lt;/script&gt;"
   ---
   Input:  </td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>
   Output: ""
   ```
   Verbatim observation: 5 out of 8 realistic transport and logistics accounting terms were erased to `""`.

3. **Flawed Test Assertion in `stress_test_ui.mjs`:**  
   In `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs` (lines 563–579):
   ```javascript
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
   ```
   Directly observed:
   - Line 564 tests string inclusion `if (daybookHtml.includes('ROGUE ROW'))`.
   - Lines 570–572 unconditionally call `fail(...)` whenever a row `r` contains `'ROGUE ROW'`, even if `analysis.totalCols === 11` and the row is a normal transaction row.
   - If `escapeHtml` performs pure entity escaping, `'ROGUE ROW'` is retained as safe plain text (`&lt;td...&gt;ROGUE ROW&lt;/td&gt;`), which triggers line 564 and falsely fails the test.

---

## 2. Logic Chain

1. **From Observation 1 to Regex & Shortcut Defect:**  
   Because `tr` in `(?:td|tr|table...)` lacks a word boundary `\b`, it matches the character sequence `<tr` in `<truck-12>`, `<transit>`, `<transport@gmail.com>`, `<trip-5>`, and `<tracking>`. Because line 2998 returns `''`, any string containing those legitimate transport remarks is completely erased.

2. **From Observation 2 to Functional Inadequacy:**  
   `escapeHtml` is an escaping function, not a sanitizer/filter. Its role is to encode characters (`&`, `<`, `>`, `"`, `'`) into entities (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&#39;`) so that the browser parser treats them as text character tokens rather than tag tokens. Deleting user text violates core accounting integrity and user trust.

3. **From Browser HTML Parser Specification to Structural Neutralization:**  
   When `'</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>'` is passed through standard pure entity encoding:
   `&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td colspan=&quot;11&quot;&gt;ROGUE ROW&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;`
   When placed into a `<td>...</td>` cell, the HTML tokenization stage processes `&lt;` as character data. The browser parser cannot tokenize or instantiate a `<tr>` or `<td>` element from character data. Tag balance is 100% maintained, no rows or cells are added, and the user's input is visible as harmless text.

4. **From Observation 3 to Test Suite Alignment:**  
   Check 1 (`<script>`), Check 2 (`<img>`), Check 4 (`<sharma@corp.com>`), and Check 5 (`<urgent>`) in `stress_test_ui.mjs` all test for the presence of **raw unescaped tags**, not harmless user text. Check 3 was inconsistent by testing for the text substring `'ROGUE ROW'`. Updating Check 3 to check for raw unescaped table tags (`<td colspan="11">` or `</td></tr><tr><td`) and extra rows aligns Check 3 with the rest of the test suite and eliminates the false failure without compromising vulnerability detection.

---

## 3. Caveats

1. **Mirroring Requirement:** Both `E:\CashBookRelease\app\www\index.html` and installed `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html` must be modified identically to pass Test Suite 1 (SHA256 identity check).
2. **Coordinated Update:** The implementer (worker) must apply the drop-in replacement to `index.html` and update `stress_test_ui.mjs` lines 562–581 in the same iteration to ensure `stress_test_ui.mjs` passes with 0 failures.
3. **No other regex shortcuts:** No other `return '';` shortcuts exist in `index.html`; `escapeHtml` at lines 2994–3007 is the sole location.

---

## 4. Conclusion

The regex shortcut (`return '';`) must be removed entirely and replaced with standard pure HTML entity encoding. Concurrently, `stress_test_ui.mjs` line 564 must be updated to assert the absence of raw unescaped table breakout tags and extra table rows rather than the presence of the text `'ROGUE ROW'`.

### Exact Drop-In Replacements

#### Target 1: `E:\CashBookRelease\app\www\index.html` (lines 2994–3007) and installed `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html` (lines 2994–3007)
**Replace:**
```javascript
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
```
**With:**
```javascript
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
```

#### Target 2: `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs` (lines 562–581)
**Replace:**
```javascript
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
```
**With:**
```javascript
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
```

---

## 5. Verification Method

To independently verify the implementation:

1. **Verify Adversarial Cases (No Data Loss):**
   Execute:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_adversarial_cases.mjs"
   ```
   **Pass Condition:** All transport test cases (`<transport@gmail.com>`, `<truck-12>`, `<transit>`, `<trip-5>`, `<tracking>`) and rogue breakout payload must return non-empty entity-encoded strings (`&lt;...&gt;`). No input string should be erased to `""`.

2. **Verify UI Stress Test (No Regression, Zero Failures):**
   Execute:
   ```powershell
   node "g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs"
   ```
   **Pass Condition:**
   - Test Suite 1: SHA256 Checksum Identity PASS
   - Test Suite 2: Tag balance PASS
   - Test Suite 3: Account switching PASS
   - Test Suite 4: All 8 checks PASS (including Table Breakout Injection Resilience)
   - Total results: `Passed: 14 | Failed: 0`, Recommended Verdict: `APPROVE`.

3. **Invalidation Conditions:**
   - Any transport term produces an empty string `""`.
   - `stress_test_ui.mjs` fails on Check 3.
   - Raw `<td` or `<tr>` elements from party payloads break into the DOM.
