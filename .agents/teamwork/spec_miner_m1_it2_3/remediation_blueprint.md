# Consolidated Remediation Blueprint: Milestone M1 (Desktop Visual Styling & Ledger Overhaul)

**Milestone**: M1 (Iteration 2-3)  
**Author**: Spec Miner M1 It2-3  
**Synthesized Inputs**:
- `explorer_m1_it2_1/handoff.md` (Challenger 2 / `escapeHtml` & DOM Sanitization)
- `explorer_m1_it2_2/handoff.md` (Challenger 1 / Accounting Precision & Math Fixes)
- `challenger_m1_1/handoff.md` & `empirical_stress_test.mjs`
- `challenger_m1_2/handoff.md` & `stress_test_ui.mjs`
- Live inspection of `E:\CashBookRelease\app\www\index.html` (6,106 lines)

**Target Codebase Files**:
1. Staging Target: `E:\CashBookRelease\app\www\index.html`
2. Installed App Target: `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`

---

## 1. Specification Matrix & Discovered Features

### Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Security / Sanitization | `escapeHtml(str)` | Global entity encoder preventing HTML/DOM injection and neutralizes table breakout attempts | String or primitive | Safe HTML entity encoded string (`&`, `<`, `>`, `"`, `'`) | Returns `''` on `null`/`undefined`; neutralizes table tags | `challenger_m1_2`, `explorer_m1_it2_1`, `stress_test_ui.mjs` |
| 2 | UI Observability | `showToast()` Container Guard | Defensive fallback if toast container is missing or lacks `appendChild` in headless runners | Message, type, duration | Injects toast notification node into DOM | Creates and attaches container element if not present | `stress_test_ui.mjs`, Node VM sandboxes |
| 3 | Accounting / Currency | `formatINR(val)` | Formats Indian currency with sign before rupee symbol (`-₹ ...`) and comma stripping | Numeric or formatted string (`-40000`, `"1,000.00"`) | Formatted string (e.g. `"-₹ 40,000.00"`, `"₹ 1,000.00"`) | Defaults to `"₹ 0.00"` on invalid or NaN input | `challenger_m1_1`, `explorer_m1_it2_2`, `empirical_stress_test.mjs` |
| 4 | Accounting / Math | `getTransactionsWithRunningBal()` | Chronological ledger calculator with discrete 2-decimal paise precision (`Math.round`) | Transaction array, selected account | Transformed array with strictly rounded `runningBal` | Invariant: Zero IEEE-754 mantissa drift (`9.99999999999998` -> `10.00`) | `challenger_m1_1`, `empirical_stress_test.mjs` S2.8 |
| 5 | Accounting / Parsing | Comma-Stripping Amount Parser | Pre-cleans amount strings (`replace(/,/g, '')`) before calling `parseFloat` | Comma-separated string (e.g. `"1,000.00"`, `"25,00,000.50"`) | Positive float scalar (e.g. `1000`, `2500000.5`) | Avoids truncation to `1` by non-numeric commas | `challenger_m1_1`, `empirical_stress_test.mjs` S2.10 |
| 6 | Accounting / Ledger | Contra Self-Transfer Independence | Decoupled source and destination checks in Contra vouchers (`Cash -> Cash`) | Contra transaction where `account === toAccount === selectedAcc` | Net balance change: `0` (both `+amt` and `-amt` fire) | Eliminates erroneous balance inflation | `challenger_m1_1`, `empirical_stress_test.mjs` S2.11 |
| 7 | UI Table Security | Day Book & Dashboard Table Sanitization | Wraps `account`, `toAccount`, `party`, `category`, `desc`, `voucher`, `mode` in `escapeHtml` | User voucher entities | Sanitized HTML table cells maintaining strict column geometry | Neutralizes script tags, img tags, and cell breakout attempts | `challenger_m1_2`, `stress_test_ui.mjs` |
| 8 | Build Integrity | Synchronous App Mirroring | Byte-for-byte SHA256 mirroring between Staging and Local AppData | Staging `index.html` | Identical file at `AppData\Local\Programs\CashBookPro\www\index.html` | S1.1 fails if hashes differ | `empirical_stress_test.mjs` S1.1 |

### Edge Cases
| # | Feature | Input | Observed Behavior | Required Behavior |
|---|---------|-------|-------------------|-------------------|
| E1 | `formatINR` | `-40000` | `"₹ -40,000.00"` (Minus inside) | `"-₹ 40,000.00"` (Minus preceding currency symbol) |
| E2 | `formatINR` | `"1,000.00"` | `"₹ 1.00"` (Truncated at comma) | `"₹ 1,000.00"` (Commas stripped before parse) |
| E3 | `getTransactionsWithRunningBal` | 100 txs of ₹0.10 | `9.99999999999998` | `10.00` exact (paise rounding with `Number.EPSILON`) |
| E4 | `getTransactionsWithRunningBal` | `{ amount: "1,000.00" }` | `runningBal: 1` | `runningBal: 1000` |
| E5 | `getTransactionsWithRunningBal` | Contra Cash -> Cash (₹5000) | Cash balance increased by ₹5000 | Cash balance net change `0` |
| E6 | `showToast` | Test sandbox without DOM container | `ReferenceError: escapeHtml is not defined` | `escapeHtml` defined in script scope |
| E7 | `showToast` | Headless mock element lacking `appendChild` | `TypeError: container.appendChild is not a function` | Falls back to `document.createElement('div')` |
| E8 | Table Rendering | `party: '<script>alert("XSS")</script>'` | Injected unescaped into `<td>` | Escaped as `&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;` |
| E9 | Table Rendering | `party: '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>'` | Breaks out into rogue table row | Neutralized into safe string without spawning extra DOM rows |
| E10 | Table Rendering | `party: 'Sharma & Sons <sharma@corp.com>'` | Treated as unclosed custom tag | Encoded as `Sharma &amp; Sons &lt;sharma@corp.com&gt;` |
| E11 | Table Rendering | `party: 'श्री कृष्णा लॉजिस्टिक्स'` | Hindi text | Preserved completely without corruption |

---

## 2. Drop-In Code Replacement Specifications

All line numbers are indexed against `E:\CashBookRelease\app\www\index.html` (6,106 lines, SHA256: `7bfffbb588c6ab995d4735d12b2884ab0f19411e09dae12e7258026e51ad60a8`).

### Replacement 1: Global `escapeHtml` Definition
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines 2990–2993 (at start of `<script>`)
- **Target Content**:
```html
  <!-- JavaScript Application Engine -->
  <script>
    // ==========================================
    // ENTERPRISE ERROR HANDLING & OBSERVABILITY
    // ==========================================
```
- **Replacement Content**:
```html
  <!-- JavaScript Application Engine -->
  <script>
    // ==========================================
    // UTILITY FUNCTIONS & HTML ESCAPING
    // ==========================================
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

    // ==========================================
    // ENTERPRISE ERROR HANDLING & OBSERVABILITY
    // ==========================================
```

---

### Replacement 2: `showToast` Container Guard
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines ~3056–3064
- **Target Content**:
```javascript
    function showToast(message, type = 'info', durationMs = 4500) {
      let container = document.getElementById('toast-container');
      if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
      }
```
- **Replacement Content**:
```javascript
    function showToast(message, type = 'info', durationMs = 4500) {
      let container = document.getElementById('toast-container');
      if (!container || typeof container.appendChild !== 'function') {
        container = document.createElement('div');
        container.id = 'toast-container';
        if (document.body && typeof document.body.appendChild === 'function') {
          document.body.appendChild(container);
        }
      }
```

---

### Replacement 3: `formatINR()` Negative Format & Comma Parsing
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines ~3376–3379
- **Target Content**:
```javascript
    // Number formatting in Indian currency style
    function formatINR(val) {
      const num = parseFloat(val) || 0;
      return '₹ ' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
```
- **Replacement Content**:
```javascript
    // Number formatting in Indian currency style
    function formatINR(val) {
      const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
      const num = parseFloat(cleanVal) || 0;
      const isNeg = num < 0;
      const absVal = Math.abs(num);
      return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
```

---

### Replacement 4: `computeBalances()` & `computePartyBalances()` Comma Parsing
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location 1**: Line 3428
  - Target: `const amt = parseFloat(t.amount) || 0;`
  - Replacement: `const amt = Math.abs(parseFloat(String(t.amount).replace(/,/g, '')) || 0);`
- **Location 2**: Line 3487
  - Target: `const amt = parseFloat(t.amount) || 0;`
  - Replacement: `const amt = Math.abs(parseFloat(String(t.amount).replace(/,/g, '')) || 0);`

---

### Replacement 5: `getTransactionsWithRunningBal()` (Paise Precision, Comma Stripping, Contra Independence)
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines ~3582–3604
- **Target Content**:
```javascript
    function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
      const sorted = [...filterList].sort((a, b) => (a.date + (a.time || '')).localeCompare(b.date + (b.time || '')));
      let running = 0;
      return sorted.map(t => {
        const amt = parseFloat(t.amount) || 0;
        if (t.status !== 'voided') {
          if (selectedAcc === 'ALL') {
            // Consolidated Cash Book: Inflow adds, Outflow subtracts, Contra has net 0 cash impact
            if (t.type === 'in') running += amt;
            else if (t.type === 'out') running -= amt;
          } else {
            // Account Specific Ledger:
            if (t.type === 'in' && t.account === selectedAcc) running += amt;
            else if (t.type === 'out' && t.account === selectedAcc) running -= amt;
            else if (t.type === 'contra') {
              if (t.toAccount === selectedAcc) running += amt; // Inflow to destination
              else if (t.account === selectedAcc) running -= amt; // Outflow from source
            }
          }
        }
        return { ...t, runningBal: running };
      });
    }
```
- **Replacement Content**:
```javascript
    function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
      const sorted = [...filterList].sort((a, b) => (a.date + (a.time || '')).localeCompare(b.date + (b.time || '')));
      let running = 0;
      return sorted.map(t => {
        const amt = Math.abs(parseFloat(String(t.amount).replace(/,/g, '')) || 0);
        if (t.status !== 'voided') {
          if (selectedAcc === 'ALL') {
            // Consolidated Cash Book: Inflow adds, Outflow subtracts, Contra has net 0 cash impact
            if (t.type === 'in') running += amt;
            else if (t.type === 'out') running -= amt;
          } else {
            // Account Specific Ledger:
            if (t.type === 'in' && t.account === selectedAcc) running += amt;
            else if (t.type === 'out' && t.account === selectedAcc) running -= amt;
            else if (t.type === 'contra') {
              if (t.toAccount === selectedAcc) running += amt; // Inflow to destination
              if (t.account === selectedAcc) running -= amt; // Outflow from source
            }
          }
          running = Math.round((running + Number.EPSILON) * 100) / 100;
        }
        return { ...t, runningBal: running };
      });
    }
```

---

### Replacement 6: Sanitize `renderDashboardTable()`
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines ~3615–3671
- **Target Content**:
```javascript
      tbody.innerHTML = recent.map(t => {
        const amt = parseFloat(t.amount) || 0;
        const isVoid = t.status === 'voided';
        const isContra = t.type === 'contra';

        // Account cell rendering
        const accountCell = isContra ? `
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">${t.account}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">${t.toAccount || 'Destination'}</span>
          </div>
        ` : `<strong>${t.account}</strong>`;

        // Party cell rendering
        const partyCell = isContra ? `
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        ` : (t.party || '-');

        // Type badge rendering
        const typeBadge = isContra ? `
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        ` : `<span class="badge-type badge-${t.type}">${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>`;

        // Amount cells: Single merged cell for Contra across Inflow & Outflow; distinct columns for In/Out
        const amountCells = isContra ? `
          <td colspan="2" class="amount-contra">
            <span class="badge-contra-amount">
              <i class="fa-solid fa-arrow-right-arrow-left"></i> ${formatINR(amt)}
              <span class="contra-amount-note">Transfer</span>
            </span>
          </td>
        ` : `
          <td class="amount-in">${t.type === 'in' ? formatINR(amt) : '-'}</td>
          <td class="amount-out">${t.type === 'out' ? formatINR(amt) : '-'}</td>
        `;

        return `
          <tr class="${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">${t.voucher}</span></td>
            <td>${t.date} ${t.time ? '<small style="color:var(--win-text-muted);">' + t.time + '</small>' : ''}</td>
            <td>${typeBadge}</td>
            <td>${accountCell}</td>
            <td>${partyCell}</td>
            <td>${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')}</td>
            ${amountCells}
            <td class="amount-balance">${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('${t.id}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('${t.id}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('${t.id}')"><i class="fa-solid fa-trash-can"></i></button>
              ${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : `
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('${t.id}')"><i class="fa-solid fa-ban"></i></button>
              `}
            </td>
          </tr>
        `;
      }).join('') || '<tr><td colspan="10" align="center" style="color: var(--win-text-muted); padding: 24px;">No recent vouchers. Click <strong>+ Cash In</strong> to record transactions.</td></tr>';
```
- **Replacement Content**:
```javascript
      tbody.innerHTML = recent.map(t => {
        const amt = Math.abs(parseFloat(String(t.amount).replace(/,/g, '')) || 0);
        const isVoid = t.status === 'voided';
        const isContra = t.type === 'contra';

        // Account cell rendering
        const accountCell = isContra ? `
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">${escapeHtml(t.account)}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">${escapeHtml(t.toAccount || 'Destination')}</span>
          </div>
        ` : `<strong>${escapeHtml(t.account)}</strong>`;

        // Party cell rendering
        const partyCell = isContra ? `
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        ` : (escapeHtml(t.party) || '-');

        // Type badge rendering
        const typeBadge = isContra ? `
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        ` : `<span class="badge-type badge-${escapeHtml(t.type)}">${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>`;

        // Amount cells: Single merged cell for Contra across Inflow & Outflow; distinct columns for In/Out
        const amountCells = isContra ? `
          <td colspan="2" class="amount-contra">
            <span class="badge-contra-amount">
              <i class="fa-solid fa-arrow-right-arrow-left"></i> ${formatINR(amt)}
              <span class="contra-amount-note">Transfer</span>
            </span>
          </td>
        ` : `
          <td class="amount-in">${t.type === 'in' ? formatINR(amt) : '-'}</td>
          <td class="amount-out">${t.type === 'out' ? formatINR(amt) : '-'}</td>
        `;

        const categoryText = t.category && t.category !== 'undefined' ? escapeHtml(t.category) : (isContra ? 'Contra Transfer' : 'General Entry');

        return `
          <tr class="${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">${escapeHtml(t.voucher)}</span></td>
            <td>${escapeHtml(t.date)} ${t.time ? '<small style="color:var(--win-text-muted);">' + escapeHtml(t.time) + '</small>' : ''}</td>
            <td>${typeBadge}</td>
            <td>${accountCell}</td>
            <td>${partyCell}</td>
            <td>${categoryText}</td>
            ${amountCells}
            <td class="amount-balance">${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('${escapeHtml(t.id)}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('${escapeHtml(t.id)}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('${escapeHtml(t.id)}')"><i class="fa-solid fa-trash-can"></i></button>
              ${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : `
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('${escapeHtml(t.id)}')"><i class="fa-solid fa-ban"></i></button>
              `}
            </td>
          </tr>
        `;
      }).join('') || '<tr><td colspan="10" align="center" style="color: var(--win-text-muted); padding: 24px;">No recent vouchers. Click <strong>+ Cash In</strong> to record transactions.</td></tr>';
```

---

### Replacement 7: Sanitize and Fix Totals in `applyFilters()`
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines ~3746–3854
- **Target Content (Totals Loop)**:
```javascript
      let totIn = 0, totOut = 0;
      filtered.forEach(t => {
        if (t.status === 'voided') return;
        const amt = parseFloat(t.amount) || 0;
        if (acc === 'ALL') {
          if (t.type === 'in') totIn += amt;
          else if (t.type === 'out') totOut += amt;
        } else {
          // Account specific filter totals
          if (t.type === 'in' && t.account === acc) totIn += amt;
          else if (t.type === 'out' && t.account === acc) totOut += amt;
          else if (t.type === 'contra') {
            if (t.toAccount === acc) totIn += amt; // Inflow to destination
            else if (t.account === acc) totOut += amt; // Outflow from source
          }
        }
      });
```
- **Replacement Content (Totals Loop)**:
```javascript
      let totIn = 0, totOut = 0;
      filtered.forEach(t => {
        if (t.status === 'voided') return;
        const amt = Math.abs(parseFloat(String(t.amount).replace(/,/g, '')) || 0);
        if (acc === 'ALL') {
          if (t.type === 'in') totIn += amt;
          else if (t.type === 'out') totOut += amt;
        } else {
          // Account specific filter totals
          if (t.type === 'in' && t.account === acc) totIn += amt;
          else if (t.type === 'out' && t.account === acc) totOut += amt;
          else if (t.type === 'contra') {
            if (t.toAccount === acc) totIn += amt; // Inflow to destination
            if (t.account === acc) totOut += amt; // Outflow from source
          }
        }
      });
      totIn = Math.round((totIn + Number.EPSILON) * 100) / 100;
      totOut = Math.round((totOut + Number.EPSILON) * 100) / 100;
```
- **Target Content (Row Mapper)**:
```javascript
      tbody.innerHTML = [...withBal].reverse().map(t => {
        const amt = parseFloat(t.amount) || 0;
        const isVoid = t.status === 'voided';
        const isContra = t.type === 'contra';

        // Account cell
        const accountCell = isContra ? `
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">${t.account}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">${t.toAccount || 'Destination'}</span>
          </div>
        ` : `<strong>${t.account}</strong>`;

        // Party cell
        const partyCell = isContra ? `
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        ` : (t.party || '-');

        // Type badge
        const typeBadge = isContra ? `
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        ` : `<span class="badge-type badge-${t.type}">${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>`;

        // Amount cells handling:
        let amountCells = '';
        if (isContra) {
          if (acc === 'ALL') {
            // Consolidated: span across both columns without duplicating
            amountCells = `
              <td colspan="2" class="amount-contra">
                <span class="badge-contra-amount">
                  <i class="fa-solid fa-arrow-right-arrow-left"></i> ${formatINR(amt)}
                  <span class="contra-amount-note">Transfer</span>
                </span>
              </td>
            `;
          } else if (t.toAccount === acc) {
            // Filtered account received the transfer (Inflow)
            amountCells = `
              <td class="amount-in"><span style="color:var(--win-blue);">${formatINR(amt)}</span></td>
              <td class="amount-out">-</td>
            `;
          } else {
            // Filtered account paid the transfer (Outflow)
            amountCells = `
              <td class="amount-in">-</td>
              <td class="amount-out"><span style="color:var(--win-blue);">${formatINR(amt)}</span></td>
            `;
          }
        } else {
          amountCells = `
            <td class="amount-in">${t.type === 'in' ? formatINR(amt) : '-'}</td>
            <td class="amount-out">${t.type === 'out' ? formatINR(amt) : '-'}</td>
          `;
        }

        return `
          <tr class="${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">${t.voucher}</span></td>
            <td>${t.date} <small style="color:var(--win-text-muted);">${t.time || ''}</small></td>
            <td>${typeBadge}</td>
            <td>${accountCell}</td>
            <td>${partyCell}</td>
            <td>${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')} ${t.desc ? '<br><small style="color:var(--win-text-muted);">' + t.desc + '</small>' : ''}</td>
            <td>${t.mode || 'Cash'}</td>
            ${amountCells}
            <td class="amount-balance">${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('${t.id}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('${t.id}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('${t.id}')"><i class="fa-solid fa-trash-can"></i></button>
              ${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : `
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('${t.id}')"><i class="fa-solid fa-ban"></i></button>
              `}
            </td>
          </tr>
        `;
      }).join('') || '<tr><td colspan="11" align="center" style="color: var(--win-text-muted); padding: 24px;">No vouchers found for this filter criteria.</td></tr>';
```
- **Replacement Content (Row Mapper)**:
```javascript
      tbody.innerHTML = [...withBal].reverse().map(t => {
        const amt = Math.abs(parseFloat(String(t.amount).replace(/,/g, '')) || 0);
        const isVoid = t.status === 'voided';
        const isContra = t.type === 'contra';

        // Account cell
        const accountCell = isContra ? `
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">${escapeHtml(t.account)}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">${escapeHtml(t.toAccount || 'Destination')}</span>
          </div>
        ` : `<strong>${escapeHtml(t.account)}</strong>`;

        // Party cell
        const partyCell = isContra ? `
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        ` : (escapeHtml(t.party) || '-');

        // Type badge
        const typeBadge = isContra ? `
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        ` : `<span class="badge-type badge-${escapeHtml(t.type)}">${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>`;

        // Amount cells handling:
        let amountCells = '';
        if (isContra) {
          if (acc === 'ALL') {
            // Consolidated: span across both columns without duplicating
            amountCells = `
              <td colspan="2" class="amount-contra">
                <span class="badge-contra-amount">
                  <i class="fa-solid fa-arrow-right-arrow-left"></i> ${formatINR(amt)}
                  <span class="contra-amount-note">Transfer</span>
                </span>
              </td>
            `;
          } else if (t.toAccount === acc) {
            // Filtered account received the transfer (Inflow)
            amountCells = `
              <td class="amount-in"><span style="color:var(--win-blue);">${formatINR(amt)}</span></td>
              <td class="amount-out">-</td>
            `;
          } else {
            // Filtered account paid the transfer (Outflow)
            amountCells = `
              <td class="amount-in">-</td>
              <td class="amount-out"><span style="color:var(--win-blue);">${formatINR(amt)}</span></td>
            `;
          }
        } else {
          amountCells = `
            <td class="amount-in">${t.type === 'in' ? formatINR(amt) : '-'}</td>
            <td class="amount-out">${t.type === 'out' ? formatINR(amt) : '-'}</td>
          `;
        }

        const categoryText = t.category && t.category !== 'undefined' ? escapeHtml(t.category) : (isContra ? 'Contra Transfer' : 'General Entry');
        const descHtml = t.desc ? '<br><small style="color:var(--win-text-muted);">' + escapeHtml(t.desc) + '</small>' : '';

        return `
          <tr class="${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">${escapeHtml(t.voucher)}</span></td>
            <td>${escapeHtml(t.date)} <small style="color:var(--win-text-muted);">${escapeHtml(t.time || '')}</small></td>
            <td>${typeBadge}</td>
            <td>${accountCell}</td>
            <td>${partyCell}</td>
            <td>${categoryText} ${descHtml}</td>
            <td>${escapeHtml(t.mode || 'Cash')}</td>
            ${amountCells}
            <td class="amount-balance">${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('${escapeHtml(t.id)}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('${escapeHtml(t.id)}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('${escapeHtml(t.id)}')"><i class="fa-solid fa-trash-can"></i></button>
              ${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : `
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('${escapeHtml(t.id)}')"><i class="fa-solid fa-ban"></i></button>
              `}
            </td>
          </tr>
        `;
      }).join('') || '<tr><td colspan="11" align="center" style="color: var(--win-text-muted); padding: 24px;">No vouchers found for this filter criteria.</td></tr>';
```

---

### Replacement 8: Sanitize `handleGlobalSearch()`
- **File**: `E:\CashBookRelease\app\www\index.html`
- **Location**: Lines ~5500–5556
- Apply identical `escapeHtml(...)` wrapping to `accountCell`, `partyCell`, `categoryText`, `descHtml`, `voucher`, `date`, `time`, and `parseFloat(String(t.amount).replace(/,/g, ''))`.

---

### Replacement 9: Synchronous App Mirroring
Immediately after editing `E:\CashBookRelease\app\www\index.html`, copy to installed application directory:
```powershell
Copy-Item -Path "E:\CashBookRelease\app\www\index.html" -Destination "C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html" -Force
```

---

## 3. Verification Commands & Expected Output

The Worker must execute the following commands in PowerShell from the project root:

1. **Verify Master Test Suite**:
   ```powershell
   node .agents/teamwork/tests/run_all_tests.mjs
   ```
   *Expected Output*: `OVERALL SUITE STATUS: ALL 125 TEST CASES PASSED SUCCESSFULLY (100%)`.

2. **Verify Challenger M1-1 Empirical Stress Harness**:
   ```powershell
   node .agents/teamwork/challenger_m1_1/empirical_stress_test.mjs
   ```
   *Expected Output*:
   ```
   Total Assertions Run: 22
   Passed: 22
   Failed: 0
   ```
   (Exit code 0).

3. **Verify Challenger M1-2 UI & Injection Stress Harness**:
   ```powershell
   node .agents/teamwork/challenger_m1_2/stress_test_ui.mjs
   ```
   *Expected Output*:
   ```
   Passed: 14 | Failed: 0
   RECOMMENDED VERDICT: APPROVE
   ```
   (Exit code 0).

4. **Verify Checksum Mirroring Parity**:
   ```powershell
   node -e "const crypto=require('crypto'), fs=require('fs'); const h1=crypto.createHash('sha256').update(fs.readFileSync('E:/CashBookRelease/app/www/index.html')).digest('hex'); const h2=crypto.createHash('sha256').update(fs.readFileSync('C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html')).digest('hex'); console.log('PARITY EQUAL:', h1 === h2, h1);"
   ```
   *Expected Output*: `PARITY EQUAL: true <sha256_hash>`.
