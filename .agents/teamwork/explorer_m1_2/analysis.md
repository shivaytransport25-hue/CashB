# Desktop Ledger Table Presentation & Contra Voucher Strategy Report

- **Explorer**: Explorer M1-2 (Desktop Ledger & Contra Layout Explorer)
- **Target File**: `E:\CashBookRelease\app\www\index.html` (and installed mirror `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`)
- **Milestone**: Milestone M1 (UI/UX Restructuring & Visual Polish) — Task M1-2
- **Date**: 2026-11-06
- **Status**: Complete — Actionable Implementation Blueprint for Worker

---

## 1. Executive Summary

This report establishes an exact engineering fix strategy for the desktop ledger data presentation and Contra voucher handling in `index.html`. 

During investigation of `E:\CashBookRelease\app\www\index.html` and review of the visual master reference (`media_1793958737050_b0f8f00b.jpg`), two critical defects were diagnosed:
1. **Ledger Table Presentation Weaknesses**:
   - Numeric figures lack `font-variant-numeric: tabular-nums` and rely on an outdated monospace stack (`'Consolas', 'Courier New'`), causing vertical character misalignment, jitter during calculation/scrolling, and indistinguishable zeroes.
   - Table dividers (`rgba(38, 53, 79, 0.6)`) lack crisp 1px hairline definition, fail WCAG contrast guidelines, and lack standard tokenization (`--color-border-hairline: #E2E8F0` / `#1E293B`).
   - Tables lack subtle alternating enterprise zebra striping, making horizontal visual tracking prone to errors across 10+ columns.
2. **Contra Voucher Ambiguity & Misrepresentation**:
   - In both `renderDashboardTable()` (`lines 3224-3225`) and `applyFilters()` (`lines 3342-3343`), Contra vouchers currently print the transfer amount in light blue (`#60a5fa`) inside **BOTH** the `INFLOW (+)` and `OUTFLOW (-)` columns on the same row.
   - This duplicates the voucher amount, visually inflating perceived cash turnover by 200%, conflicting with top-level summary totals, and misrepresenting internal transfers as external income and expenditure.
   - In `handleGlobalSearch()` (`lines 5009-5010`) and `printDayBook()` (`lines 4723-4724`), Contra amounts are rendered as `-` in both columns, hiding the voucher's value entirely.
   - In the Account column, source and destination accounts are merely concatenated with a raw arrow string (`SBI Current A/c ➔ Cash in Hand`) without indicating debit/credit roles, while Party Name redundantly repeats `"Contra Transfer"`.

This report provides the complete replacement CSS, HTML, and JavaScript blocks ready for direct application by the Worker.

---

## 2. In-Depth Technical Observations

### 2.1 CSS Variables & Typography
- **Monospace Font Variable** (`index.html:95`):
  ```css
  --win-mono: 'Consolas', 'Courier New', monospace;
  ```
  *Defect*: Lacks `JetBrains Mono` priority and fails to declare OpenType tabular feature settings (`tnum` and slashed zero `zero`).
- **Missing Hairline Border Tokens**:
  `:root` defines `--win-border: #26354f;` and `--win-border-light: #384d72;`, but does NOT define `--color-border-hairline` or `--win-border-hairline` as mandated by `spec_requirements.md`.
- **Amount Classes** (`index.html:888-890`):
  ```css
  .amount-in { color: var(--win-green); font-family: var(--win-mono); font-weight: 700; text-align: right; }
  .amount-out { color: var(--win-red); font-family: var(--win-mono); font-weight: 700; text-align: right; }
  .amount-balance { color: #f1f5f9; font-family: var(--win-mono); font-weight: 700; text-align: right; }
  ```
  *Defect*: Missing `font-variant-numeric: tabular-nums` and `font-feature-settings: "tnum" 1, "zero" 1`.

### 2.2 Table Borders, Padding & Row Striping
- **Table Definition** (`index.html:836-863`):
  ```css
  .data-table th {
    background: #0d1522;
    color: #94a3b8;
    font-weight: 600;
    padding: 8px 10px;
    border-bottom: 1px solid var(--win-border);
    position: sticky;
    top: 0;
    z-index: 10;
    text-transform: uppercase;
    font-size: 11px;
    letter-spacing: 0.3px;
  }
  .data-table td {
    padding: 8px 10px;
    border-bottom: 1px solid rgba(38, 53, 79, 0.6);
    color: #e2e8f0;
    white-space: nowrap;
  }
  .data-table tr:hover {
    background: rgba(245, 158, 11, 0.05);
  }
  ```
  *Defects*:
  1. `border-bottom: 1px solid rgba(38, 53, 79, 0.6)` is blurry and dark on dark theme, and `.data-table td { border-bottom-color: #e2e8f0; }` in light theme lacks crispness.
  2. Zero alternating row striping (`nth-child(even)` is omitted entirely).
  3. Padding of `8px 10px` is cramped for monetary figures containing currency symbols and decimal paise.

### 2.3 Contra Voucher Ambiguity Analysis
- **Current Rendering Code**:
  - `renderDashboardTable()` (`lines 3224-3225`):
    ```javascript
    <td class="amount-in">${t.type === 'in' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
    <td class="amount-out">${t.type === 'out' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
    ```
  - `applyFilters()` (`lines 3342-3343`):
    ```javascript
    <td class="amount-in">${t.type === 'in' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
    <td class="amount-out">${t.type === 'out' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
    ```
  - `handleGlobalSearch()` (`lines 5009-5010`):
    ```javascript
    <td class="amount-in">${t.type === 'in' ? formatINR(amt) : '-'}</td>
    <td class="amount-out">${t.type === 'out' ? formatINR(amt) : '-'}</td>
    ```
  - `printDayBook()` (`lines 4723-4724`):
    ```javascript
    <td align="right">${t.type === 'in' ? formatINR(amt) : '-'}</td>
    <td align="right">${t.type === 'out' ? formatINR(amt) : '-'}</td>
    ```
- **Accounting & UI Conflicts**:
  1. **Visual Double-Count**: Showing ₹ 30,000 under Inflow AND ₹ 30,000 under Outflow gives the illusion that ₹ 60,000 in gross volume took place, or that cash came into the business from outside and left the business to an outside party.
  2. **Mismatch with Totals**: `computeBalances()` (`lines 3046-3050`) specifically skips Contra entries when computing `totalIn` and `totalOut`:
     ```javascript
     } else if (t.type === 'contra') {
       accountBalances[t.account] = (accountBalances[t.account] || 0) - amt;
       accountBalances[t.toAccount] = (accountBalances[t.toAccount] || 0) + amt;
     }
     ```
     Therefore, `daybook-total-in` and `daybook-total-out` at the top of the screen DO NOT include Contra amounts. When a user or auditor sums the visible numbers in the table column, the sum differs from the header totals, triggering audit confusion.
  3. **Search & Print Discrepancy**: Search and print views render `-`, creating behavioral inconsistency across tabs.

---

## 3. Authoritative UI/UX Design System Specification

### 3.1 Design Tokens & Fonts
- **Monospace Stack**:
  ```css
  --win-mono: 'JetBrains Mono', 'Consolas', monospace;
  ```
- **Hairline Divider Tokens**:
  - Dark Theme:
    ```css
    --win-border-hairline: #1e293b;
    --color-border-hairline: #1e293b;
    ```
  - Light Theme (`body[data-theme="light"]`):
    ```css
    --win-border-hairline: #e2e8f0;
    --color-border-hairline: #e2e8f0;
    ```
- **Tabular Numerals Standard**:
  All financial figures in tables, balance cards, summaries, and inputs must use:
  ```css
  font-family: var(--win-mono);
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum" 1, "zero" 1;
  ```

### 3.2 Enterprise Alternating Row Striping
- **Dark Mode**:
  - Base Row: `background: var(--win-surface);` (`#111a28`)
  - Even Row (`tbody tr:nth-child(even)`): `background: rgba(255, 255, 255, 0.018);` (subtle slate distinction)
  - Hover Row (`tbody tr:hover`): `background: rgba(217, 119, 6, 0.06);` (subtle enterprise amber highlight)
- **Light Mode**:
  - Base Row: `background: #ffffff;`
  - Even Row (`tbody tr:nth-child(even)`): `background: #f8fafc;` (Slate 50)
  - Hover Row (`tbody tr:hover`): `background: #f1f5f9;` (Slate 100)
- **Dividers**:
  `border-bottom: 1px solid var(--win-border-hairline);` on both `th` and `td`.
- **Padding**:
  `padding: 9px 12px;` for comfortable data density matching standard 1080p/1440p displays.

---

## 4. Contra Voucher Resolution Strategy

### 4.1 UI Principles
1. **Never Duplicate Amounts Across Inflow & Outflow**:
   In a consolidated ledger view (Day Book or Dashboard), an inter-account transfer has net-zero impact on the firm's liquid cash. Printing the amount in both columns is an anti-pattern.
2. **Provide Prominent Visual Differentiation**:
   Contra vouchers must be immediately identifiable via:
   - A distinct blue badge (`badge-contra`) with transfer icon `⇄`.
   - A structured routing pill in the Account column (`[Source Account] ➔ [Destination Account]`).
   - Clear "Internal Transfer" tag in the Party column.
3. **Consolidated View vs Single-Account Filter View**:
   - **Consolidated View (`filter-account === 'ALL'` or Dashboard)**:
     Merge the `INFLOW (+)` and `OUTFLOW (-)` columns into a single `colspan="2"` cell displaying:
     `<td colspan="2" class="amount-contra"><span class="badge-contra-amount"><i class="fa-solid fa-arrow-right-arrow-left"></i> ₹ 30,000.00 <small>Transfer</small></span></td>`
     *Why this is the optimal design*:
     - The exact voucher amount is displayed in the amount region of the table.
     - It does NOT sit under Inflow alone (which would misrepresent income).
     - It does NOT sit under Outflow alone (which would misrepresent expense).
     - It does NOT duplicate the amount in two cells.
     - HTML column alignment is strictly maintained (total span = 10 cols on Dashboard, 11 cols on Day Book).
   - **Single-Account View (`filter-account !== 'ALL'`)**:
     When the user explicitly filters the Day Book to inspect a single account (e.g. `SBI Current A/c`):
     - If the selected account is the **Source (`t.account === filterAcc`)**:
       `INFLOW (+)`: `-`
       `OUTFLOW (-)`: `<span class="amount-out-transfer" style="color: var(--win-blue); font-family: var(--win-mono); font-weight: 700;">₹ 30,000.00</span>` (money departed this account)
     - If the selected account is the **Destination (`t.toAccount === filterAcc`)**:
       `INFLOW (+)`: `<span class="amount-in-transfer" style="color: var(--win-blue); font-family: var(--win-mono); font-weight: 700;">₹ 30,000.00</span>` (money arrived in this account)
       `OUTFLOW (-)`: `-`

---

## 5. Exact Replacement Code Blocks for Worker

### Block 1: CSS Tokens & Table Styles (Replace lines 832–891 & add light-mode overrides)

```css
/* ==========================================================================
   ENTERPRISE FINTECH DATA TABLE & CONTRA VOUCHER STYLING
   Enforces: tabular numbers, JetBrains Mono font stack, 1px hairline dividers,
   enterprise zebra striping, and distinct contra transfer presentation.
   ========================================================================== */

:root {
  --win-mono: 'JetBrains Mono', 'Consolas', monospace;
  --win-border-hairline: #1e293b;
  --color-border-hairline: #1e293b;
}

body[data-theme="light"] {
  --win-mono: 'JetBrains Mono', 'Consolas', monospace;
  --win-border-hairline: #e2e8f0;
  --color-border-hairline: #e2e8f0;
}

.data-table-wrapper {
  max-height: 520px;
  overflow-y: auto;
  border: 1px solid var(--win-border-hairline);
  border-radius: 6px;
  background: var(--win-surface);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  text-align: left;
}

.data-table th {
  background: #0f172a;
  color: #94a3b8;
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  padding: 9px 12px;
  border-bottom: 1px solid var(--win-border-hairline);
  position: sticky;
  top: 0;
  z-index: 10;
  white-space: nowrap;
}

.data-table td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--win-border-hairline);
  color: #e2e8f0;
  white-space: nowrap;
  vertical-align: middle;
}

/* Subtle Enterprise Zebra Striping */
.data-table tbody tr:nth-child(even) {
  background: rgba(255, 255, 255, 0.018);
}

.data-table tbody tr:hover {
  background: rgba(217, 119, 6, 0.06);
}

.data-table tr.row-voided td {
  opacity: 0.45;
  text-decoration: line-through;
}

/* Monospace Tabular Figures Standard */
.amount-in,
.amount-out,
.amount-balance,
.amount-contra,
.mono-amount,
.badge-voucher,
.denom-input,
.voucher-table td {
  font-family: var(--win-mono);
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum" 1, "zero" 1;
}

.amount-in {
  color: var(--win-green);
  font-weight: 700;
  text-align: right;
}

.amount-out {
  color: var(--win-red);
  font-weight: 700;
  text-align: right;
}

.amount-balance {
  color: #f1f5f9;
  font-weight: 700;
  text-align: right;
}

/* Voucher Badge */
.badge-voucher {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
  background: var(--win-surface-2);
  border: 1px solid var(--win-border);
  color: #93c5fd;
  font-weight: 600;
  display: inline-block;
}

/* Semantic Type Badges */
.badge-type {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  letter-spacing: 0.3px;
}

.badge-in {
  background: var(--win-green-bg);
  color: var(--win-green);
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.badge-out {
  background: var(--win-red-bg);
  color: var(--win-red);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.badge-contra {
  background: rgba(30, 58, 95, 0.35);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.4);
}

/* Contra Route Presentation in Account Cell */
.contra-route-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  font-size: 11.5px;
}

.contra-route-src {
  font-weight: 600;
  color: var(--win-text);
}

.contra-route-arrow {
  color: #60a5fa;
  font-size: 10px;
}

.contra-route-dst {
  font-weight: 600;
  color: #93c5fd;
}

/* Contra Internal Party Tag */
.contra-internal-tag {
  color: var(--win-text-muted);
  font-size: 11.5px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-style: italic;
}

/* Contra Amount Pill in Merged Table Cell */
.amount-contra {
  text-align: center !important;
}

.badge-contra-amount {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 11.5px;
  font-weight: 700;
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
  border: 1px dashed rgba(59, 130, 246, 0.35);
}

.contra-amount-note {
  font-size: 9.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  opacity: 0.85;
  background: rgba(59, 130, 246, 0.2);
  padding: 1px 4px;
  border-radius: 3px;
}

/* Action Buttons in Table Rows */
.row-btn {
  background: transparent;
  border: 1px solid var(--win-border-hairline);
  border-radius: 4px;
  color: var(--win-text-muted);
  cursor: pointer;
  padding: 3px 7px;
  font-size: 11px;
  transition: all 0.15s ease-in-out;
}

.row-btn:hover {
  background: var(--win-surface-2);
  color: #ffffff;
  border-color: var(--win-border-light);
}

.row-btn.btn-edit:hover {
  color: var(--win-accent);
  border-color: var(--win-accent);
  background: rgba(245, 158, 11, 0.15);
}

.row-btn.btn-del:hover {
  color: var(--win-red);
  border-color: var(--win-red);
  background: var(--win-red-bg);
}

.row-btn.btn-void:hover {
  color: var(--win-red);
  border-color: var(--win-red);
}

/* Light Theme Overrides */
body[data-theme="light"] .data-table-wrapper {
  background: #ffffff;
  border-color: #e2e8f0;
}

body[data-theme="light"] .data-table th {
  background: #f1f5f9;
  color: #334155;
  border-bottom-color: #cbd5e1;
}

body[data-theme="light"] .data-table td {
  color: #0f172a;
  border-bottom-color: #e2e8f0;
}

body[data-theme="light"] .data-table tbody tr:nth-child(even) {
  background: #f8fafc;
}

body[data-theme="light"] .data-table tbody tr:hover {
  background: #f1f5f9;
}

body[data-theme="light"] .badge-voucher {
  background: #f1f5f9;
  color: #334155;
  border-color: #cbd5e1;
}

body[data-theme="light"] .badge-in {
  background: #ecfdf5;
  color: #047857 !important;
  border-color: #059669;
}

body[data-theme="light"] .badge-out {
  background: #fef2f2;
  color: #b91c1c !important;
  border-color: #dc2626;
}

body[data-theme="light"] .badge-contra {
  background: #eff6ff;
  color: #1d4ed8 !important;
  border-color: #2563eb;
}

body[data-theme="light"] .contra-route-pill {
  background: #eff6ff;
  border-color: #bfdbfe;
}

body[data-theme="light"] .contra-route-src {
  color: #1e293b;
}

body[data-theme="light"] .contra-route-arrow {
  color: #2563eb;
}

body[data-theme="light"] .contra-route-dst {
  color: #1d4ed8;
}

body[data-theme="light"] .badge-contra-amount {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #93c5fd;
}

body[data-theme="light"] .contra-amount-note {
  background: #dbeafe;
  color: #1d4ed8;
}

body[data-theme="light"] .amount-in {
  color: #047857;
}

body[data-theme="light"] .amount-out {
  color: #b91c1c;
}

body[data-theme="light"] .amount-balance {
  color: #0f172a;
}

body[data-theme="light"] .row-btn {
  border-color: #cbd5e1;
  color: #475569;
}

body[data-theme="light"] .row-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}
```

---

### Block 2: Account-Aware Running Balance Calculation (Replace lines 3190–3202)

```javascript
    // Calculate running balance supporting consolidated view or filtered account
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

---

### Block 3: Dashboard Recent Entries Table Renderer (Replace lines 3204–3238)

```javascript
    // Render Dashboard Table (Recent 8 Entries) with Non-Duplicating Contra Representation
    function renderDashboardTable() {
      const tbody = document.getElementById('dashboard-txn-tbody');
      if (!tbody) return;

      const withBal = getTransactionsWithRunningBal(transactions, 'ALL');
      // Show newest first
      const recent = [...withBal].reverse().slice(0, 8);

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
      }).join('') || '<tr><td colspan="10" align="center" style="color: var(--win-text-muted); padding: 24px;">No vouchers recorded yet.</td></tr>';
    }
```

---

### Block 4: Full Day Book Filter & Table Renderer (Replace lines 3310–3356 in `applyFilters`)

```javascript
      // Calculate totals for summary banner
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

      const withBal = getTransactionsWithRunningBal(filtered, acc);
      const closing = withBal.length > 0 ? withBal[withBal.length - 1].runningBal : 0;

      document.getElementById('daybook-period-label').innerText = period === 'ALL' ? 'All Records' : period;
      document.getElementById('daybook-total-in').innerText = formatINR(totIn);
      document.getElementById('daybook-total-out').innerText = formatINR(totOut);
      document.getElementById('daybook-closing-bal').innerText = formatINR(closing);

      const tbody = document.getElementById('daybook-txn-tbody');
      if (!tbody) return;

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

---

### Block 5: Global Search Synchronized Renderer (Replace lines 4996–5022)

```javascript
      const withBal = getTransactionsWithRunningBal(filtered, 'ALL');
      const tbody = document.getElementById('dashboard-txn-tbody');
      tbody.innerHTML = [...withBal].reverse().map(t => {
        const amt = parseFloat(t.amount) || 0;
        const isVoid = t.status === 'voided';
        const isContra = t.type === 'contra';

        const accountCell = isContra ? `
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">${t.account}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">${t.toAccount || 'Destination'}</span>
          </div>
        ` : `<strong>${t.account}</strong>`;

        const partyCell = isContra ? `
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        ` : (t.party || '-');

        const typeBadge = isContra ? `
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        ` : `<span class="badge-type badge-${t.type}">${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>`;

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
            <td>${t.date} <small style="color:var(--win-text-muted);">${t.time || ''}</small></td>
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
      }).join('') || '<tr><td colspan="10" align="center" style="color: var(--win-text-muted); padding: 24px;">No matching entries found.</td></tr>';
```

---

## 6. Verification & Acceptance Checklist for Worker

1. **Numeric Typography**:
   - [ ] Inspect `.amount-in`, `.amount-out`, `.amount-balance`, and `.mono-amount` in DevTools: verify computed `font-family` contains `'JetBrains Mono', 'Consolas', monospace`.
   - [ ] Verify `font-variant-numeric: tabular-nums` is active on all amounts.
   - [ ] Verify digits 0–9 align in straight vertical lines without horizontal jumping.
2. **Dividers & Striping**:
   - [ ] Confirm table header and cell borders use 1px solid hairline (`#1E293B` in dark mode, `#E2E8F0` in light mode).
   - [ ] Confirm even rows (`tbody tr:nth-child(even)`) display subtle enterprise zebra background.
   - [ ] Confirm row hover displays subtle restrained tint.
3. **Contra Voucher Representation**:
   - [ ] Record a Contra voucher (e.g. Transfer ₹ 30,000 from SBI Current A/c to Cash in Hand).
   - [ ] On Dashboard and Day Book (All Accounts): Verify the Contra entry displays `[ ⇄ CONTRA ]` blue badge.
   - [ ] Verify the Account column displays `[SBI Current A/c] ➔ [Cash in Hand]`.
   - [ ] Verify the Party column displays `Internal Transfer`.
   - [ ] Verify the amount is NOT displayed twice in both columns. In Consolidated View, it spans the amount area cleanly as `[ ⇄ ₹ 30,000.00 Transfer ]`.
   - [ ] When filtering Day Book by `SBI Current A/c`, verify Outflow shows ₹ 30,000 and Inflow is `-`.
   - [ ] When filtering Day Book by `Cash in Hand`, verify Inflow shows ₹ 30,000 and Outflow is `-`.
   - [ ] Check Day Book header totals: verify Total Inflow and Total Outflow reflect external revenue and expense accurately.
