import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert';

// Read index.html without modifying original file
let html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

// Patch 1: escapeHtml definition
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

html = html.replace('    function showToast(message, type = \'info\', durationMs = 4500) {', escapeHtmlDef);

// Patch 2: renderDashboardTable
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

html = html.replace(oldDashboard, newDashboard);

// Patch 3: applyFilters
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
            <td>${'${typeBadge}'}</td>
            <td>${'${accountCell}'}</td>
            <td>${'${partyCell}'}</td>
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
            <td>${'${typeBadge}'}</td>
            <td>${'${accountCell}'}</td>
            <td>${'${partyCell}'}</td>
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

html = html.replace(oldApplyFilters, newApplyFilters);

// Patch 4: handleGlobalSearch
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

html = html.replace(oldGlobalSearch, newGlobalSearch);

// Run Challenger 2 test harness logic with this modified `html`
const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>/);
const code = scriptMatch[1];

function createSandbox(initialTxns = [], initialCompany = null) {
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

  const elements = new Proxy(elementsMap, {
    get(target, prop) {
      if (typeof prop === 'string') return getEl(prop);
      return target[prop];
    }
  });

  const store = {};
  if (initialTxns && initialTxns.length > 0) store['cashbook_txns'] = JSON.stringify(initialTxns);
  if (initialCompany) store['cashbook_company'] = JSON.stringify(initialCompany);

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
            if (this.id === 'toast-container' || el.className.includes('toast')) toastsCreated.push(child);
            return child;
          },
          remove() {
            if (this.parentElement) {
              const idx = this.parentElement.children.indexOf(this);
              if (idx >= 0) this.parentElement.children.splice(idx, 1);
            }
          }
        };
        return el;
      },
      body: { appendChild(child) { return child; } }
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

  return { sandbox, elements, store, toastsCreated };
}

function analyzeTableRow(rowHtml) {
  const tdRegex = /<td([^>]*)>([\s\S]*?)<\/td>/gi;
  let match;
  let totalCols = 0;
  const cells = [];
  while ((match = tdRegex.exec(rowHtml)) !== null) {
    const attrs = match[1];
    const content = match[2];
    const colspanMatch = attrs.match(/colspan=["']?(\d+)["']?/i);
    const colspan = colspanMatch ? parseInt(colspanMatch[1], 10) : 1;
    totalCols += colspan;
    cells.push({ attrs, content, colspan });
  }
  return { totalCols, cells };
}

function checkTagBalance(htmlString) {
  const tagsToCheck = ['tr', 'td', 'th', 'span', 'div', 'button', 'small', 'strong', 'i'];
  const mismatches = [];
  for (const tag of tagsToCheck) {
    const openRegex = new RegExp(`<${tag}(\\s+[^>]*)?>`, 'gi');
    const closeRegex = new RegExp(`</${tag}>`, 'gi');
    const openCount = (htmlString.match(openRegex) || []).length;
    const closeCount = (htmlString.match(closeRegex) || []).length;
    if (openCount !== closeCount) mismatches.push(`${tag}: opened ${openCount} != closed ${closeCount}`);
  }
  return { balanced: mismatches.length === 0, mismatches };
}

console.log('--- RUNNING FULL CHALLENGER SUITE ON PATCHED CODE ---');

// Test 2: Tag Balance
const { sandbox, elements } = createSandbox();
sandbox.applyFilters();
const daybookHtml = elements['daybook-txn-tbody'].innerHTML;
assert(checkTagBalance(daybookHtml).balanced, 'Daybook tag balance');

sandbox.renderDashboardTable();
const dashHtml = elements['dashboard-txn-tbody'].innerHTML;
assert(checkTagBalance(dashHtml).balanced, 'Dashboard tag balance');
console.log('✔ Test Suite 2: Tag balance checks PASSED');

// Test 4.1: escapeHtml defined?
assert.strictEqual(typeof sandbox.escapeHtml, 'function', 'escapeHtml is function');
console.log('✔ Test 4.1: escapeHtml defined PASSED');

// Test 4.2: showToast
sandbox.showToast('Test Toast');
console.log('✔ Test 4.2: showToast PASSED');

// Test 4.3: Attack vectors
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

const attackEnv = createSandbox(attackVouchers);
attackEnv.sandbox.applyFilters();
const attackDbHtml = attackEnv.elements['daybook-txn-tbody'].innerHTML;

assert(!attackDbHtml.includes('<script>alert("XSS Party")</script>'), 'Check 1: Script tag escaped');
console.log('✔ Check 1: Script tag escaped PASSED');

assert(!attackDbHtml.includes('<img src=x onerror='), 'Check 2: Img tag escaped');
console.log('✔ Check 2: Img tag escaped PASSED');

// Check 3: Table breakout
// An unescaped breakout creates an isolated row without VCH-BREAKOUT containing <td colspan="11">ROGUE ROW</td>
const rows = attackDbHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
let rogueRowInjected = false;
for (const r of rows) {
  if (r.includes('<td colspan="11">ROGUE ROW') || (!r.includes('VCH-BREAKOUT') && r.includes('ROGUE ROW'))) {
    rogueRowInjected = true;
  }
}
assert(!rogueRowInjected, 'Check 3: No rogue row injected from breakout');
assert(attackDbHtml.includes('&lt;td colspan=&quot;11&quot;&gt;ROGUE ROW&lt;/td&gt;'), 'Check 3: Breakout tags entity-encoded safely');
console.log('✔ Check 3: Breakout neutralized and tags safely entity-encoded PASSED');

assert(!attackDbHtml.includes('<sharma@corp.com>'), 'Check 4: Email angle brackets not raw');
assert(attackDbHtml.includes('&lt;sharma@corp.com&gt;'), 'Check 4: Email converted to HTML entities');
console.log('✔ Check 4: Email angle brackets entity-encoded PASSED');

assert(!attackDbHtml.includes('<urgent>'), 'Check 5: Remark not raw');
assert(attackDbHtml.includes('&lt;urgent&gt;'), 'Check 5: Remark converted to HTML entities');
console.log('✔ Check 5: Remark angle brackets entity-encoded PASSED');

assert(attackDbHtml.includes('श्री कृष्णा लॉजिस्टिक्स'), 'Check 6: Hindi preserved');
console.log('✔ Check 6: Unicode Hindi preserved PASSED');

console.log('\n=======================================================');
console.log('ALL CHALLENGER M1-2 EMPIRICAL TESTS PASS 100%!');
console.log('=======================================================');
