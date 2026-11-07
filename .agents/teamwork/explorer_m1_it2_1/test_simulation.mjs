import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert';

// Read index.html without modifying it
let content = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

// 1. Apply escapeHtml function definition right before showToast
const escapeHtmlDef = `    // Global HTML Escape Utility to Prevent XSS and DOM Injection
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

    function showToast(message, type = 'info', durationMs = 4500) {`;

content = content.replace('    function showToast(message, type = \'info\', durationMs = 4500) {', escapeHtmlDef);

// 2. Patch renderDashboardTable
const oldDashboard = `        // Account cell rendering
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
        \` : (t.party || '-');

        // Type badge rendering
        const typeBadge = isContra ? \`
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        \` : \`<span class="badge-type badge-\${t.type}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;

        // Amount cells: Single merged cell for Contra across Inflow & Outflow; distinct columns for In/Out
        const amountCells = isContra ? \`
          <td colspan="2" class="amount-contra">
            <span class="badge-contra-amount">
              <i class="fa-solid fa-arrow-right-arrow-left"></i> \${formatINR(amt)}
              <span class="contra-amount-note">Transfer</span>
            </span>
          </td>
        \` : \`
          <td class="amount-in">\${t.type === 'in' ? formatINR(amt) : '-'}</td>
          <td class="amount-out">\${t.type === 'out' ? formatINR(amt) : '-'}</td>
        \`;

        return \`
          <tr class="\${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">\${t.voucher}</span></td>
            <td>\${t.date} \${t.time ? '<small style="color:var(--win-text-muted);">' + t.time + '</small>' : ''}</td>
            <td>\${typeBadge}</td>
            <td>\${accountCell}</td>
            <td>\${partyCell}</td>
            <td>\${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')}</td>
            \${amountCells}
            <td class="amount-balance">\${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('\${t.id}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('\${t.id}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('\${t.id}')"><i class="fa-solid fa-trash-can"></i></button>
              \${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : \`
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('\${t.id}')"><i class="fa-solid fa-ban"></i></button>
              \`}
            </td>
          </tr>
        \`;`;

const newDashboard = `        // Account cell rendering
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
        \` : (escapeHtml(t.party) || '-');

        // Type badge rendering
        const typeBadge = isContra ? \`
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        \` : \`<span class="badge-type badge-\${escapeHtml(t.type)}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;

        // Amount cells: Single merged cell for Contra across Inflow & Outflow; distinct columns for In/Out
        const amountCells = isContra ? \`
          <td colspan="2" class="amount-contra">
            <span class="badge-contra-amount">
              <i class="fa-solid fa-arrow-right-arrow-left"></i> \${formatINR(amt)}
              <span class="contra-amount-note">Transfer</span>
            </span>
          </td>
        \` : \`
          <td class="amount-in">\${t.type === 'in' ? formatINR(amt) : '-'}</td>
          <td class="amount-out">\${t.type === 'out' ? formatINR(amt) : '-'}</td>
        \`;

        const catLabel = t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry');

        return \`
          <tr class="\${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">\${escapeHtml(t.voucher)}</span></td>
            <td>\${escapeHtml(t.date)} \${t.time ? '<small style="color:var(--win-text-muted);">' + escapeHtml(t.time) + '</small>' : ''}</td>
            <td>\${typeBadge}</td>
            <td>\${accountCell}</td>
            <td>\${partyCell}</td>
            <td>\${escapeHtml(catLabel)}\${t.desc ? '<br><small style="color:var(--win-text-muted);">' + escapeHtml(t.desc) + '</small>' : ''}</td>
            \${amountCells}
            <td class="amount-balance">\${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('\${escapeHtml(t.id)}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-trash-can"></i></button>
              \${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : \`
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-ban"></i></button>
              \`}
            </td>
          </tr>
        \`;`;

assert(content.includes(oldDashboard), 'oldDashboard must be in content');
content = content.replace(oldDashboard, newDashboard);

// 3. Patch applyFilters
const oldApplyFilters = `        // Account cell
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
        \` : \`<span class="badge-type badge-\${t.type}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;

        // Amount cells handling:
        let amountCells = '';
        if (isContra) {
          if (acc === 'ALL') {
            // Consolidated: span across both columns without duplicating
            amountCells = \`
              <td colspan="2" class="amount-contra">
                <span class="badge-contra-amount">
                  <i class="fa-solid fa-arrow-right-arrow-left"></i> \${formatINR(amt)}
                  <span class="contra-amount-note">Transfer</span>
                </span>
              </td>
            \`;
          } else if (t.toAccount === acc) {
            // Filtered account received the transfer (Inflow)
            amountCells = \`
              <td class="amount-in"><span style="color:var(--win-blue);">\${formatINR(amt)}</span></td>
              <td class="amount-out">-</td>
            \`;
          } else {
            // Filtered account paid the transfer (Outflow)
            amountCells = \`
              <td class="amount-in">-</td>
              <td class="amount-out"><span style="color:var(--win-blue);">\${formatINR(amt)}</span></td>
            \`;
          }
        } else {
          amountCells = \`
            <td class="amount-in">\${t.type === 'in' ? formatINR(amt) : '-'}</td>
            <td class="amount-out">\${t.type === 'out' ? formatINR(amt) : '-'}</td>
          \`;
        }

        return \`
          <tr class="\${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">\${t.voucher}</span></td>
            <td>\${t.date} <small style="color:var(--win-text-muted);">\${t.time || ''}</small></td>
            <td>\${typeBadge}</td>
            <td>\${accountCell}</td>
            <td>\${partyCell}</td>
            <td>\${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')} \${t.desc ? '<br><small style="color:var(--win-text-muted);">' + t.desc + '</small>' : ''}</td>
            <td>\${t.mode || 'Cash'}</td>
            \${amountCells}
            <td class="amount-balance">\${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('\${t.id}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('\${t.id}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('\${t.id}')"><i class="fa-solid fa-trash-can"></i></button>
              \${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : \`
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('\${t.id}')"><i class="fa-solid fa-ban"></i></button>
              \`}
            </td>
          </tr>
        \`;`;

const newApplyFilters = `        // Account cell
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
        \` : \`<span class="badge-type badge-\${escapeHtml(t.type)}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;

        // Amount cells handling:
        let amountCells = '';
        if (isContra) {
          if (acc === 'ALL') {
            // Consolidated: span across both columns without duplicating
            amountCells = \`
              <td colspan="2" class="amount-contra">
                <span class="badge-contra-amount">
                  <i class="fa-solid fa-arrow-right-arrow-left"></i> \${formatINR(amt)}
                  <span class="contra-amount-note">Transfer</span>
                </span>
              </td>
            \`;
          } else if (t.toAccount === acc) {
            // Filtered account received the transfer (Inflow)
            amountCells = \`
              <td class="amount-in"><span style="color:var(--win-blue);">\${formatINR(amt)}</span></td>
              <td class="amount-out">-</td>
            \`;
          } else {
            // Filtered account paid the transfer (Outflow)
            amountCells = \`
              <td class="amount-in">-</td>
              <td class="amount-out"><span style="color:var(--win-blue);">\${formatINR(amt)}</span></td>
            \`;
          }
        } else {
          amountCells = \`
            <td class="amount-in">\${t.type === 'in' ? formatINR(amt) : '-'}</td>
            <td class="amount-out">\${t.type === 'out' ? formatINR(amt) : '-'}</td>
          \`;
        }

        const catLabel = t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry');

        return \`
          <tr class="\${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">\${escapeHtml(t.voucher)}</span></td>
            <td>\${escapeHtml(t.date)} <small style="color:var(--win-text-muted);">\${escapeHtml(t.time || '')}</small></td>
            <td>\${typeBadge}</td>
            <td>\${accountCell}</td>
            <td>\${partyCell}</td>
            <td>\${escapeHtml(catLabel)} \${t.desc ? '<br><small style="color:var(--win-text-muted);">' + escapeHtml(t.desc) + '</small>' : ''}</td>
            <td>\${escapeHtml(t.mode || 'Cash')}</td>
            \${amountCells}
            <td class="amount-balance">\${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('\${escapeHtml(t.id)}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-trash-can"></i></button>
              \${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : \`
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-ban"></i></button>
              \`}
            </td>
          </tr>
        \`;`;

assert(content.includes(oldApplyFilters), 'oldApplyFilters must be in content');
content = content.replace(oldApplyFilters, newApplyFilters);

// 4. Patch handleGlobalSearch
const oldGlobalSearch = `        const accountCell = isContra ? \`
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">\${t.account}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">\${t.toAccount || 'Destination'}</span>
          </div>
        \` : \`<strong>\${t.account}</strong>\`;

        const partyCell = isContra ? \`
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        \` : (t.party || '-');

        const typeBadge = isContra ? \`
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        \` : \`<span class="badge-type badge-\${t.type}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;

        const amountCells = isContra ? \`
          <td colspan="2" class="amount-contra">
            <span class="badge-contra-amount">
              <i class="fa-solid fa-arrow-right-arrow-left"></i> \${formatINR(amt)}
              <span class="contra-amount-note">Transfer</span>
            </span>
          </td>
        \` : \`
          <td class="amount-in">\${t.type === 'in' ? formatINR(amt) : '-'}</td>
          <td class="amount-out">\${t.type === 'out' ? formatINR(amt) : '-'}</td>
        \`;

        return \`
          <tr class="\${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">\${t.voucher}</span></td>
            <td>\${t.date} <small style="color:var(--win-text-muted);">\${t.time || ''}</small></td>
            <td>\${typeBadge}</td>
            <td>\${accountCell}</td>
            <td>\${partyCell}</td>
            <td>\${t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry')}</td>
            \${amountCells}
            <td class="amount-balance">\${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('\${t.id}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('\${t.id}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('\${t.id}')"><i class="fa-solid fa-trash-can"></i></button>
              \${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : \`
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('\${t.id}')"><i class="fa-solid fa-ban"></i></button>
              \`}
            </td>
          </tr>
        \`;`;

const newGlobalSearch = `        const accountCell = isContra ? \`
          <div class="contra-route-pill" title="Transfer Route">
            <span class="contra-route-src">\${escapeHtml(t.account)}</span>
            <span class="contra-route-arrow"><i class="fa-solid fa-arrow-right-long"></i></span>
            <span class="contra-route-dst">\${escapeHtml(t.toAccount || 'Destination')}</span>
          </div>
        \` : \`<strong>\${escapeHtml(t.account)}</strong>\`;

        const partyCell = isContra ? \`
          <span class="contra-internal-tag"><i class="fa-solid fa-building-columns"></i> Internal Transfer</span>
        \` : (escapeHtml(t.party) || '-');

        const typeBadge = isContra ? \`
          <span class="badge-type badge-contra"><i class="fa-solid fa-arrow-right-arrow-left"></i> CONTRA</span>
        \` : \`<span class="badge-type badge-\${escapeHtml(t.type)}">\${t.type === 'in' ? 'CASH IN' : 'CASH OUT'}</span>\`;

        const amountCells = isContra ? \`
          <td colspan="2" class="amount-contra">
            <span class="badge-contra-amount">
              <i class="fa-solid fa-arrow-right-arrow-left"></i> \${formatINR(amt)}
              <span class="contra-amount-note">Transfer</span>
            </span>
          </td>
        \` : \`
          <td class="amount-in">\${t.type === 'in' ? formatINR(amt) : '-'}</td>
          <td class="amount-out">\${t.type === 'out' ? formatINR(amt) : '-'}</td>
        \`;

        const catLabel = t.category && t.category !== 'undefined' ? t.category : (isContra ? 'Contra Transfer' : 'General Entry');

        return \`
          <tr class="\${isVoid ? 'row-voided' : ''}">
            <td><span class="badge-voucher">\${escapeHtml(t.voucher)}</span></td>
            <td>\${escapeHtml(t.date)} <small style="color:var(--win-text-muted);">\${escapeHtml(t.time || '')}</small></td>
            <td>\${typeBadge}</td>
            <td>\${accountCell}</td>
            <td>\${partyCell}</td>
            <td>\${escapeHtml(catLabel)}\${t.desc ? '<br><small style="color:var(--win-text-muted);">' + escapeHtml(t.desc) + '</small>' : ''}</td>
            \${amountCells}
            <td class="amount-balance">\${formatINR(t.runningBal)}</td>
            <td align="center" style="white-space: nowrap;">
              <button class="row-btn btn-edit" title="Edit Voucher (Entry)" onclick="editTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-pen-to-square"></i></button>
              <button class="row-btn" title="Print Single Voucher" onclick="printSingleVoucher('\${escapeHtml(t.id)}')"><i class="fa-solid fa-print"></i></button>
              <button class="row-btn btn-del" title="Delete Voucher Permanently" onclick="deleteTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-trash-can"></i></button>
              \${isVoid ? '<span style="color:var(--win-red); font-size:10px; font-weight:700; margin-left:3px;">VOIDED</span>' : \`
                <button class="row-btn btn-void" title="Void Transaction" onclick="voidTransaction('\${escapeHtml(t.id)}')"><i class="fa-solid fa-ban"></i></button>
              \`}
            </td>
          </tr>
        \`;`;

assert(content.includes(oldGlobalSearch), 'oldGlobalSearch must be in content');
content = content.replace(oldGlobalSearch, newGlobalSearch);

console.log('Successfully simulated patches on index.html in memory!');

// Now run the Challenger 2 stress test logic on this in-memory content!
const scriptMatch = content.match(/<script>([\s\S]*?)<\/script>/);
const code = scriptMatch[1];

function createMockEnv(initialTxns = []) {
  const elementsMap = {};
  function getEl(id) {
    if (!elementsMap[id]) {
      elementsMap[id] = {
        id,
        value: 'ALL',
        innerText: '',
        textContent: '',
        innerHTML: '',
        style: {},
        classList: {
          _classes: new Set(),
          add(c) { this._classes.add(c); },
          remove(c) { this._classes.delete(c); },
          toggle(c, force) {
            if (force !== undefined) {
              if (force) this._classes.add(c); else this._classes.delete(c);
              return force;
            }
            if (this._classes.has(c)) { this._classes.delete(c); return false; }
            this._classes.add(c); return true;
          },
          contains(c) { return this._classes.has(c); }
        },
        appendChild(child) {
          if (!this.children) this.children = [];
          this.children.push(child);
          return child;
        }
      };
    }
    return elementsMap[id];
  }

  const store = {};
  if (initialTxns && initialTxns.length > 0) {
    store['cashbook_txns'] = JSON.stringify(initialTxns);
  }

  const toastsCreated = [];

  const sandbox = {
    window: {},
    document: {
      getElementById: getEl,
      querySelectorAll: () => [],
      addEventListener: () => {},
      createElement: (tag) => {
        const el = {
          tagName: tag,
          className: '',
          style: {},
          innerHTML: '',
          textContent: '',
          children: [],
          parentElement: null,
          appendChild(child) {
            child.parentElement = this;
            this.children.push(child);
            if (this.id === 'toast-container' || el.className.includes('toast')) {
              toastsCreated.push(child);
            }
            return child;
          },
          remove() {}
        };
        return el;
      },
      body: {
        appendChild(child) {
          return child;
        }
      }
    },
    console: { log: () => {}, warn: () => {}, error: () => {} },
    setTimeout: (fn) => {},
    clearTimeout: () => {},
    setInterval: () => {},
    clearInterval: () => {},
    Date: Date,
    Math: Math,
    parseFloat: parseFloat,
    parseInt: parseInt,
    String: String,
    Array: Array,
    Object: Object,
    RegExp: RegExp,
    JSON: JSON,
    localStorage: {
      getItem: (k) => store[k] || null,
      setItem: (k, v) => { store[k] = v; },
      removeItem: (k) => { delete store[k]; }
    },
    navigator: { onLine: true },
    location: { reload: () => {} },
    Chart: class {},
    QRCode: class {},
    XLSX: {},
    firebase: {
      initializeApp: () => ({
        auth: () => ({ onAuthStateChanged: () => {} }),
        firestore: () => ({ collection: () => ({}) })
      })
    }
  };

  sandbox.window = sandbox;
  sandbox.addEventListener = () => {};

  vm.createContext(sandbox);
  vm.runInContext(code, sandbox);

  return { sandbox, elements: elementsMap, toasts: toastsCreated };
}

// 1. Test escapeHtml existence
const env = createMockEnv();
console.log('1. typeof sandbox.escapeHtml:', typeof env.sandbox.escapeHtml);
assert.strictEqual(typeof env.sandbox.escapeHtml, 'function');

// 2. Test showToast
env.sandbox.showToast('Test Toast Notification');
console.log('2. showToast executed successfully! Toast count:', env.toasts.length);

// 3. Test attack vectors
const attackVouchers = [
  {
    id: 'tx_xss_script',
    voucher: 'VCH-XSS-01',
    date: '2026-11-06',
    time: '12:00 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: '<script>alert("XSS Party")</script>',
    category: 'Sales',
    desc: '<script>alert("XSS Remark")</script>',
    mode: 'Cash',
    amount: 1000,
    status: 'active'
  },
  {
    id: 'tx_html_img',
    voucher: 'VCH-XSS-02',
    date: '2026-11-06',
    time: '12:05 PM',
    type: 'out',
    account: 'Cash in Hand',
    party: '<img src=x onerror=alert("pwn")>',
    category: 'Office',
    desc: '<b onmouseover=alert("hover")>Bold Narration</b>',
    mode: 'Cash',
    amount: 2000,
    status: 'active'
  },
  {
    id: 'tx_table_breakout',
    voucher: 'VCH-BREAKOUT',
    date: '2026-11-06',
    time: '12:10 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>',
    category: 'Testing',
    desc: '</td><td>Injected Col</td>',
    mode: 'Cash',
    amount: 3000,
    status: 'active'
  },
  {
    id: 'tx_amp_quotes',
    voucher: 'VCH-CHARS',
    date: '2026-11-06',
    time: '12:15 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: 'Sharma & Sons <sharma@corp.com> "VIP" \'Client\'',
    category: 'Sales',
    desc: 'Payment for 100 & 200 units <urgent> & "confidential"',
    mode: 'Cash',
    amount: 4000,
    status: 'active'
  },
  {
    id: 'tx_hindi_unicode',
    voucher: 'VCH-HINDI',
    date: '2026-11-06',
    time: '12:20 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: 'श्री कृष्णा लॉजिस्टिक्स (ट्रांसपोर्ट & वेयरहाउस)',
    category: 'भाड़ा',
    desc: 'भाड़ा भुगतान अग्रिम',
    mode: 'Cash',
    amount: 50000,
    status: 'active'
  }
];

const attackEnv = createMockEnv(attackVouchers);
attackEnv.sandbox.applyFilters();
const daybookHtml = attackEnv.elements['daybook-txn-tbody'].innerHTML;

assert(!daybookHtml.includes('<script>alert("XSS Party")</script>'), 'No raw script in party');
assert(!daybookHtml.includes('<img src=x onerror='), 'No raw img in party');
assert(!daybookHtml.includes('ROGUE ROW</td></tr>'), 'No breakout table row');
assert(!daybookHtml.includes('<sharma@corp.com>'), 'No raw email');
assert(daybookHtml.includes('&lt;sharma@corp.com&gt;'), 'Email is properly entity encoded');
assert(!daybookHtml.includes('<urgent>'), 'No raw <urgent>');
assert(daybookHtml.includes('&lt;urgent&gt;'), '<urgent> is properly entity encoded');
assert(daybookHtml.includes('श्री कृष्णा लॉजिस्टिक्स'), 'Hindi preserved');

console.log('3. All attack vector assertions passed on Day Book!');

// Test Dashboard table
attackEnv.sandbox.renderDashboardTable();
const dashHtml = attackEnv.elements['dashboard-txn-tbody'].innerHTML;
assert(!dashHtml.includes('<script>alert("XSS Party")</script>'), 'Dashboard: No raw script in party');
assert(!dashHtml.includes('<img src=x onerror='), 'Dashboard: No raw img in party');
assert(!dashHtml.includes('ROGUE ROW</td></tr>'), 'Dashboard: No breakout table row');
assert(!dashHtml.includes('<sharma@corp.com>'), 'Dashboard: No raw email');
assert(dashHtml.includes('&lt;sharma@corp.com&gt;'), 'Dashboard: Email is properly entity encoded');
assert(dashHtml.includes('श्री कृष्णा लॉजिस्टिक्स'), 'Dashboard: Hindi preserved');

console.log('4. All attack vector assertions passed on Dashboard!');

// Test Global Search
attackEnv.sandbox.handleGlobalSearch('Sharma');
const searchHtml = attackEnv.elements['dashboard-txn-tbody'].innerHTML;
assert(!searchHtml.includes('<sharma@corp.com>'), 'Search: No raw email');
assert(searchHtml.includes('&lt;sharma@corp.com&gt;'), 'Search: Email entity encoded');

console.log('5. All attack vector assertions passed on Global Search!');
console.log('SUCCESS: All in-memory simulation tests passed completely!');
