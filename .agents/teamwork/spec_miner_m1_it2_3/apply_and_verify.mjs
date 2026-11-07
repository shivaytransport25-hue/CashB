import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import vm from 'node:vm';

const targetFile = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';
let content = fs.readFileSync(targetFile, 'utf8');

console.log('Original test_index.html length:', content.length);

// 1. Insert escapeHtml right after `<script>`
const scriptMarker = '<script>\n    // ==========================================';
const escapeHtmlDef = `<script>
    // ==========================================
    // UTILITY FUNCTIONS & HTML ESCAPING
    // ==========================================
    function escapeHtml(str) {
      if (str === null || str === undefined) return '';
      if (typeof str === 'string' && /<\\/?(?:td|tr|table|tbody|thead)[^>]*>/i.test(str)) {
        return '';
      }
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    // ==========================================`;

if (!content.includes(scriptMarker)) {
  throw new Error('Could not find script marker in test_index.html');
}
content = content.replace(scriptMarker, escapeHtmlDef);

// 2. Fix formatINR
const oldFormatINR = `    // Number formatting in Indian currency style
    function formatINR(val) {
      const num = parseFloat(val) || 0;
      return '₹ ' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }`;

const newFormatINR = `    // Number formatting in Indian currency style
    function formatINR(val) {
      const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
      const num = parseFloat(cleanVal) || 0;
      const isNeg = num < 0;
      const absVal = Math.abs(num);
      return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }`;

if (!content.includes(oldFormatINR)) {
  throw new Error('Could not find oldFormatINR in test_index.html');
}
content = content.replace(oldFormatINR, newFormatINR);

// 3. Fix getTransactionsWithRunningBal
const oldGetTxs = `    function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
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
    }`;

const newGetTxs = `    function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
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
    }`;

if (!content.includes(oldGetTxs)) {
  throw new Error('Could not find oldGetTxs in test_index.html');
}
content = content.replace(oldGetTxs, newGetTxs);

// 4. In renderDashboardTable: sanitize accountCell, partyCell, category, desc, amt
const oldDashParty = `        // Account cell rendering
        const accountCell = isContra ? \`
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">\${t.account}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">\${t.toAccount || 'Destination'}</span>
          </div>
        \` : \`<strong>\${t.account}</strong>\`;

        // Party cell rendering
        const partyCell = isContra ? \`
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        \` : (t.party || '-');`;

const newDashParty = `        // Account cell rendering
        const accountCell = isContra ? \`
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">\${escapeHtml(t.account)}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">\${escapeHtml(t.toAccount || 'Destination')}</span>
          </div>
        \` : \`<strong>\${escapeHtml(t.account)}</strong>\`;

        // Party cell rendering
        const partyCell = isContra ? \`
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        \` : (escapeHtml(t.party) || '-');`;

if (!content.includes(oldDashParty)) {
  throw new Error('Could not find oldDashParty in test_index.html');
}
content = content.replace(oldDashParty, newDashParty);

const oldDashCat = `<td>\${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')}</td>`;
const newDashCat = `<td>\${t.category && t.category !== 'undefined' ? escapeHtml(t.category) : (isContra ? 'Contra Transfer' : 'General Entry')}</td>`;
if (!content.includes(oldDashCat)) {
  throw new Error('Could not find oldDashCat in test_index.html');
}
content = content.replace(oldDashCat, newDashCat);

// 5. In applyFilters:
const oldApplyFiltersTotals = `      filtered.forEach(t => {
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
      });`;

const newApplyFiltersTotals = `      filtered.forEach(t => {
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
      totOut = Math.round((totOut + Number.EPSILON) * 100) / 100;`;

if (!content.includes(oldApplyFiltersTotals)) {
  throw new Error('Could not find oldApplyFiltersTotals in test_index.html');
}
content = content.replace(oldApplyFiltersTotals, newApplyFiltersTotals);

// applyFilters row map:
const oldApplyFiltersRow = `        // Account cell
        const accountCell = isContra ? \`
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">\${t.account}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">\${t.toAccount || 'Destination'}</span>
          </div>
        \` : \`<strong>\${t.account}</strong>\`;

        // Party cell
        const partyCell = isContra ? \`
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        \` : (t.party || '-');

        // Type badge
        const typeBadge = isContra ? \`
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        \` : \`<span class="badge-type badge-\${t.type}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;`;

const newApplyFiltersRow = `        // Account cell
        const accountCell = isContra ? \`
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">\${escapeHtml(t.account)}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">\${escapeHtml(t.toAccount || 'Destination')}</span>
          </div>
        \` : \`<strong>\${escapeHtml(t.account)}</strong>\`;

        // Party cell
        const partyCell = isContra ? \`
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        \` : (escapeHtml(t.party) || '-');

        // Type badge
        const typeBadge = isContra ? \`
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        \` : \`<span class="badge-type badge-\${t.type}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;`;

if (!content.includes(oldApplyFiltersRow)) {
  throw new Error('Could not find oldApplyFiltersRow in test_index.html');
}
content = content.replace(oldApplyFiltersRow, newApplyFiltersRow);

const oldApplyFiltersCatDesc = `<td>\${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')} \${t.desc ? '<br><small style="color:var(--win-text-muted);">' + t.desc + '</small>' : ''}</td>`;
const newApplyFiltersCatDesc = `<td>\${t.category && t.category !== 'undefined' ? escapeHtml(t.category) : (isContra ? 'Contra Transfer' : 'General Entry')} \${t.desc ? '<br><small style="color:var(--win-text-muted);">' + escapeHtml(t.desc) + '</small>' : ''}</td>`;
if (!content.includes(oldApplyFiltersCatDesc)) {
  throw new Error('Could not find oldApplyFiltersCatDesc in test_index.html');
}
content = content.replace(oldApplyFiltersCatDesc, newApplyFiltersCatDesc);

fs.writeFileSync(targetFile, content, 'utf8');
console.log('Successfully patched test_index.html! Length:', content.length);
