# Handoff Report: Desktop Ledger Table Presentation & Contra Voucher Strategy (Task M1-2)

- **Agent**: Explorer M1-2 (Desktop Ledger & Contra Layout Explorer)
- **Role**: Read-only investigation and fix specification
- **Target File**: `E:\CashBookRelease\app\www\index.html` (and `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`)
- **Recipient**: Parent Agent / Task Worker M1-2
- **Date**: 2026-11-06
- **Handoff Type**: Hard (Task Complete)

---

## 1. Observation

Direct investigation of `E:\CashBookRelease\app\www\index.html` and visual master reference `media_1793958737050_b0f8f00b.jpg` established the following facts:

1. **Monospace Font & Numeral Alignment**:
   - `index.html:95`:
     ```css
     --win-mono: 'Consolas', 'Courier New', monospace;
     ```
   - `index.html:888-890`:
     ```css
     .amount-in { color: var(--win-green); font-family: var(--win-mono); font-weight: 700; text-align: right; }
     .amount-out { color: var(--win-red); font-family: var(--win-mono); font-weight: 700; text-align: right; }
     .amount-balance { color: #f1f5f9; font-family: var(--win-mono); font-weight: 700; text-align: right; }
     ```
   - Neither `.data-table`, `.amount-in`, `.amount-out`, `.amount-balance`, nor `.mono-amount` specifies `font-variant-numeric: tabular-nums` or `font-feature-settings: "tnum" 1, "zero" 1`.

2. **Divider Borders & Row Striping**:
   - `index.html:847, 857`:
     ```css
     .data-table th { ... border-bottom: 1px solid var(--win-border); ... }
     .data-table td { ... border-bottom: 1px solid rgba(38, 53, 79, 0.6); ... }
     ```
   - Light mode overrides (`index.html:126-128`):
     ```css
     body[data-theme="light"] .data-table th { background: #e2e8f0; color: #334155; border-bottom-color: #cbd5e1; }
     body[data-theme="light"] .data-table td { color: #0f172a; border-bottom-color: #e2e8f0; }
     ```
   - Standard hairline tokens (`--win-border-hairline` / `--color-border-hairline: #E2E8F0` / `#1E293B`) are completely missing.
   - Alternating row striping (`nth-child(even)`) is absent.

3. **Contra Voucher Ambiguity & Duplication**:
   - `index.html:3224-3225` (`renderDashboardTable`):
     ```javascript
     <td class="amount-in">${t.type === 'in' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
     <td class="amount-out">${t.type === 'out' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
     ```
   - `index.html:3342-3343` (`applyFilters` Day Book Table):
     ```javascript
     <td class="amount-in">${t.type === 'in' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
     <td class="amount-out">${t.type === 'out' ? formatINR(amt) : (t.type === 'contra' ? '<span style="color:#60a5fa;">' + formatINR(amt) + '</span>' : '-')}</td>
     ```
   - `index.html:5009-5010` (`handleGlobalSearch`):
     ```javascript
     <td class="amount-in">${t.type === 'in' ? formatINR(amt) : '-'}</td>
     <td class="amount-out">${t.type === 'out' ? formatINR(amt) : '-'}</td>
     ```
   - `index.html:4723-4724` (`printDayBook`):
     ```javascript
     <td align="right">${t.type === 'in' ? formatINR(amt) : '-'}</td>
     <td align="right">${t.type === 'out' ? formatINR(amt) : '-'}</td>
     ```
   - `computeBalances()` (`index.html:3046-3050`):
     ```javascript
     } else if (t.type === 'contra') {
       accountBalances[t.account] = (accountBalances[t.account] || 0) - amt;
       accountBalances[t.toAccount] = (accountBalances[t.toAccount] || 0) + amt;
     }
     ```
     `totalIn` and `totalOut` do NOT include Contra transfers, so column totals conflict with visible amounts.

4. **Account & Party Rendering**:
   - `index.html:3221, 3338`:
     ```javascript
     <td><strong>${t.account}</strong> ${t.toAccount ? '➔ ' + t.toAccount : ''}</td>
     ```
   - Party column renders raw `"Contra Transfer"`.

---

## 2. Logic Chain

1. **From Observation 1 to Font & Numeral Alignment**:
   Without `font-variant-numeric: tabular-nums` and `font-feature-settings: "tnum" 1, "zero" 1`, monospace and proportional fonts display digits with varying character widths and normal zeroes. When scrolling ledger rows with amounts like `₹ 150.00` and `₹ 84,000.00`, decimal points jump horizontally, degrading professional readability. Upgrading `--win-mono` to `'JetBrains Mono', 'Consolas', monospace` with OpenType `tnum` and `zero` fixes numeric alignment.

2. **From Observation 2 to Table Layout & Row Striping**:
   The current borders use `rgba(38, 53, 79, 0.6)` on dark theme and `#e2e8f0` on light theme without standard tokenization. Introducing `--win-border-hairline: #1e293b;` (Dark) and `#e2e8f0;` (Light) brings compliance with `spec_requirements.md`. Adding `nth-child(even)` row striping (`rgba(255, 255, 255, 0.018)` / `#f8fafc`) allows accountants to effortlessly track long rows horizontally across 10+ columns without eye fatigue.

3. **From Observation 3 to Contra Resolution**:
   In `renderDashboardTable` and `applyFilters`, printing `formatINR(amt)` in both `amount-in` and `amount-out` cells creates accounting ambiguity:
   - It appears as if the transaction is simultaneously credit and debit on the same line.
   - It implies external cash flow when it is purely an internal transfer.
   - Summing the visible figures in either column does not match the header totals (`daybook-total-in` and `daybook-total-out`), violating financial audit expectations.
   - Merging the two amount columns using `<td colspan="2" class="amount-contra">` with a single centered transfer pill `<span class="badge-contra-amount"><i class="fa-solid fa-arrow-right-arrow-left"></i> ${formatINR(amt)} Transfer</span>` in Consolidated View eliminates amount duplication and misrepresentation while preserving the visibility of the transaction amount.
   - In Single-Account Filter View, displaying the amount under `OUTFLOW (-)` for the source account and under `INFLOW (+)` for the destination account cleanly represents account-level fund flow.

4. **From Observation 4 to Visual Routing**:
   Replacing the raw `➔` string with a structured `.contra-route-pill` (`[Source Account] ➔ [Destination Account]`) and changing Party Name to `<span class="contra-internal-tag">Internal Transfer</span>` provides unambiguous ledger context.

---

## 3. Caveats

1. **WebView2 Font Availability**: `JetBrains Mono` will fall back to `Consolas` and generic `monospace` if the local Windows machine does not have `JetBrains Mono` installed as a system font or web font. The CSS font stack explicitly includes `'Consolas', monospace` to ensure 100% deterministic fallback.
2. **Account Balance Recalculation Scope**: `getTransactionsWithRunningBal()` modifications specifically enhance running balance calculations when filtering by a single account. If `filter-account` is `'ALL'`, it maintains the existing consolidated cash balance logic.
3. **No Direct Code Modifications Performed**: Per explorer role constraints, all code changes are documented in `analysis.md` and this handoff report. Direct edits to `E:\CashBookRelease\app\www\index.html` must be executed by the Worker.

---

## 4. Conclusion

The strategy to overhaul the desktop ledger table and resolve Contra voucher ambiguity is fully formulated and ready for execution.

**Actionable Implementation Instructions for Worker**:
1. **CSS Replacement**: Replace `:root` and `body[data-theme="light"]` border/font tokens, and replace `.data-table` definitions (lines 832–891) with Block 1 from `analysis.md`.
2. **Running Balance Engine**: Replace `getTransactionsWithRunningBal` (lines 3190–3202) with Block 2 from `analysis.md`.
3. **Dashboard Table Renderer**: Replace `renderDashboardTable` (lines 3205–3238) with Block 3 from `analysis.md`.
4. **Day Book Table Renderer**: Replace totals and row mapping in `applyFilters` (lines 3310–3356) with Block 4 from `analysis.md`.
5. **Search Synchronization**: Replace row mapping in `handleGlobalSearch` (lines 4996–5022) with Block 5 from `analysis.md`.

---

## 5. Verification Method

To verify the implementation once applied by the Worker:

1. **Automated Search & Lint Checks**:
   ```powershell
   # Confirm hairline tokens are defined
   Select-String -Path 'E:\CashBookRelease\app\www\index.html' -Pattern 'win-border-hairline'

   # Confirm tabular numbers are configured
   Select-String -Path 'E:\CashBookRelease\app\www\index.html' -Pattern 'font-variant-numeric: tabular-nums'

   # Confirm contra amount duplication in both columns is eliminated
   Select-String -Path 'E:\CashBookRelease\app\www\index.html' -Pattern 'amount-contra'
   ```

2. **Visual & Behavioral Verification in Desktop App**:
   - Launch `E:\CashBookRelease\app\CashBookPro.exe` or open `E:\CashBookRelease\app\www\index.html` in Microsoft Edge / WebView2.
   - Inspect Day Book (Consolidated): Verify Contra vouchers render `[ ⇄ CONTRA ]` blue badge, `[Source] ➔ [Destination]` pill, and single centered transfer amount spanning Inflow/Outflow.
   - Inspect Day Book (Filtered by Source Account): Verify Contra amount displays under `OUTFLOW (-)`.
   - Inspect Day Book (Filtered by Destination Account): Verify Contra amount displays under `INFLOW (+)`.
   - Inspect font rendering: Verify numbers, decimals, and commas align in straight vertical columns.
   - Toggle Dark/Light mode: Verify 1px hairline dividers and zebra striping are visible and high-contrast in both themes.
