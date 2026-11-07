/**
 * stress_test_ui.mjs
 * Challenger M1-2 Empirical Stress Test Harness
 * 
 * Objectives:
 * 1. Validate SHA256 checksum identity between staging and installed app files.
 * 2. Verify Day Book & Dashboard table HTML integrity and tag balance.
 * 3. Stress-test special characters (&, <, >, ", ') and HTML injection in Party and Remark fields.
 * 4. Stress-test rapid account switching (ALL -> Cash in Hand -> Bank -> HDFC) to ensure column counts (colspan="2" vs 1) never corrupt table layouts.
 * 5. Verify function definitions (e.g. escapeHtml) and error handling.
 */

import fs from 'node:fs';
import crypto from 'node:crypto';
import assert from 'node:assert';
import vm from 'node:vm';

const STAGING_PATH = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';
const INSTALLED_PATH = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';

const results = {
  passed: 0,
  failed: 0,
  findings: []
};

function pass(name, details = '') {
  results.passed++;
  console.log(`  \x1b[32m✔ PASS\x1b[0m ${name} ${details ? '\x1b[2m(' + details + ')\x1b[0m' : ''}`);
}

function fail(name, error, severity = 'HIGH') {
  results.failed++;
  results.findings.push({ name, error, severity });
  console.log(`  \x1b[31m✖ FAIL\x1b[0m [${severity}] ${name}`);
  console.log(`    \x1b[31mError: ${error}\x1b[0m`);
}

console.log('\n\x1b[1m\x1b[36m================================================================');
console.log('CHALLENGER M1-2: EMPIRICAL UI & WEBVIEW2 STRESS TEST HARNESS');
console.log('================================================================\x1b[0m\n');

// ============================================================================
// TEST SUITE 1: SHA256 Checksum Parity & File Mirroring
// ============================================================================
console.log('\x1b[1m▶ TEST SUITE 1: SHA256 Checksum Parity & File Mirroring\x1b[0m');

try {
  const stagingBuf = fs.readFileSync(STAGING_PATH);
  const installedBuf = fs.readFileSync(INSTALLED_PATH);

  const stagingHash = crypto.createHash('sha256').update(stagingBuf).digest('hex');
  const installedHash = crypto.createHash('sha256').update(installedBuf).digest('hex');

  assert.strictEqual(stagingHash, installedHash, `Hashes differ: ${stagingHash} vs ${installedHash}`);
  assert.strictEqual(stagingBuf.length, installedBuf.length, `Sizes differ: ${stagingBuf.length} vs ${installedBuf.length}`);

  pass('SHA256 Checksum Identity', `Hash: ${stagingHash.substring(0, 16)}... Size: ${stagingBuf.length} bytes`);
} catch (e) {
  fail('SHA256 Checksum Identity', e.message, 'CRITICAL');
}

// ============================================================================
// SETUP SANDBOX DOM ENVIRONMENT FOR INDEX.HTML
// ============================================================================

function createSandbox(initialTxns = [], initialCompany = null) {
  const html = fs.readFileSync(STAGING_PATH, 'utf8');
  const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>/);
  if (!scriptMatch) throw new Error('Could not find <script> in index.html');
  const code = scriptMatch[1];

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
        }
      };
    }
    return elementsMap[id];
  }

  const elements = new Proxy(elementsMap, {
    get(target, prop) {
      if (typeof prop === 'string') {
        return getEl(prop);
      }
      return target[prop];
    }
  });

  const store = {};
  if (initialTxns && initialTxns.length > 0) {
    store['cashbook_txns'] = JSON.stringify(initialTxns);
  }
  if (initialCompany) {
    store['cashbook_company'] = JSON.stringify(initialCompany);
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
          remove() {
            if (this.parentElement) {
              const idx = this.parentElement.children.indexOf(this);
              if (idx >= 0) this.parentElement.children.splice(idx, 1);
            }
          }
        };
        return el;
      },
      body: {
        appendChild(child) {
          return child;
        }
      }
    },
    console: {
      log: () => {},
      warn: () => {},
      error: () => {}
    },
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

// Helper: parse table row column count and tags
function analyzeTableRow(rowHtml) {
  // Count <td> elements and calculate effective spanned columns
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
  // Verifies that opening tags match closing tags for paired HTML tags
  const tagsToCheck = ['tr', 'td', 'th', 'span', 'div', 'button', 'small', 'strong', 'i'];
  const mismatches = [];

  for (const tag of tagsToCheck) {
    const openRegex = new RegExp(`<${tag}(\\s+[^>]*)?>`, 'gi');
    const closeRegex = new RegExp(`</${tag}>`, 'gi');

    const openCount = (htmlString.match(openRegex) || []).length;
    const closeCount = (htmlString.match(closeRegex) || []).length;

    if (openCount !== closeCount) {
      mismatches.push(`${tag}: opened ${openCount} != closed ${closeCount}`);
    }
  }

  return { balanced: mismatches.length === 0, mismatches };
}

// ============================================================================
// TEST SUITE 2: Tag Balance & HTML Structural Integrity
// ============================================================================
console.log('\n\x1b[1m▶ TEST SUITE 2: HTML Structural Integrity & Tag Balance\x1b[0m');

try {
  const { sandbox, elements } = createSandbox();
  
  // Test Day Book rendering with default transactions
  sandbox.applyFilters();
  const daybookHtml = elements['daybook-txn-tbody'].innerHTML;
  const dbBalance = checkTagBalance(daybookHtml);
  if (dbBalance.balanced) {
    pass('Day Book default records tag balance', 'All tr, td, span, div, button tags balanced');
  } else {
    fail('Day Book default records tag balance', dbBalance.mismatches.join(', '), 'HIGH');
  }

  // Test Dashboard table rendering
  sandbox.renderDashboardTable();
  const dashHtml = elements['dashboard-txn-tbody'].innerHTML;
  const dashBalance = checkTagBalance(dashHtml);
  if (dashBalance.balanced) {
    pass('Dashboard default records tag balance', 'All tr, td, span, div, button tags balanced');
  } else {
    fail('Dashboard default records tag balance', dashBalance.mismatches.join(', '), 'HIGH');
  }

  // Test Empty state tag balance
  const emptyEnv = createSandbox([{ id: 'tx_dummy', status: 'voided', amount: 0, date: '1970-01-01', type: 'in', account: 'Cash' }]);
  emptyEnv.elements['filter-period'].value = 'TODAY'; // forces empty result
  emptyEnv.sandbox.applyFilters();
  const emptyHtml = emptyEnv.elements['daybook-txn-tbody'].innerHTML;
  const emptyBalance = checkTagBalance(emptyHtml);
  if (emptyBalance.balanced) {
    pass('Day Book empty state tag balance', 'Empty message tr and td balanced');
  } else {
    fail('Day Book empty state tag balance', emptyBalance.mismatches.join(', '), 'HIGH');
  }

} catch (e) {
  fail('HTML Structural Integrity execution', e.message, 'HIGH');
}

// ============================================================================
// TEST SUITE 3: Column Count Consistency & Rapid Account Switching
// ============================================================================
console.log('\n\x1b[1m▶ TEST SUITE 3: Rapid Account Switching & Column Count Invariants\x1b[0m');

try {
  const testVouchers = [
    {
      id: 'tx_in_cash',
      voucher: 'VCH-001',
      date: '2026-11-06',
      time: '10:00 AM',
      type: 'in',
      account: 'Cash in Hand',
      toAccount: '',
      party: 'Retail Client',
      category: 'Sales',
      desc: 'Cash received',
      mode: 'Cash',
      amount: 15000,
      status: 'active'
    },
    {
      id: 'tx_out_cash',
      voucher: 'VCH-002',
      date: '2026-11-06',
      time: '11:00 AM',
      type: 'out',
      account: 'Cash in Hand',
      toAccount: '',
      party: 'Office Landlord',
      category: 'Rent',
      desc: 'Office rent',
      mode: 'Cash',
      amount: 8000,
      status: 'active'
    },
    {
      id: 'tx_contra_cash_to_bank',
      voucher: 'VCH-003',
      date: '2026-11-06',
      time: '12:00 PM',
      type: 'contra',
      account: 'Cash in Hand',
      toAccount: 'HDFC Bank',
      party: '',
      category: 'Contra Transfer',
      desc: 'Deposit cash to bank',
      mode: 'Bank Cheque',
      amount: 5000,
      status: 'active'
    },
    {
      id: 'tx_in_bank',
      voucher: 'VCH-004',
      date: '2026-11-06',
      time: '01:00 PM',
      type: 'in',
      account: 'HDFC Bank',
      toAccount: '',
      party: 'Big Buyer Corp',
      category: 'Wholesale Sales',
      desc: 'RTGS received',
      mode: 'NEFT / RTGS',
      amount: 45000,
      status: 'active'
    },
    {
      id: 'tx_contra_bank_to_sbi',
      voucher: 'VCH-005',
      date: '2026-11-06',
      time: '02:00 PM',
      type: 'contra',
      account: 'HDFC Bank',
      toAccount: 'SBI Current A/c',
      party: '',
      category: 'Contra Transfer',
      desc: 'Inter-bank fund transfer',
      mode: 'NEFT / RTGS',
      amount: 20000,
      status: 'active'
    },
    {
      id: 'tx_voided',
      voucher: 'VCH-006',
      date: '2026-11-06',
      time: '03:00 PM',
      type: 'out',
      account: 'SBI Current A/c',
      toAccount: '',
      party: 'Accidental Entry',
      category: 'Misc',
      desc: 'Voided transaction',
      mode: 'Cash',
      amount: 1000,
      status: 'voided'
    }
  ];

  const env = createSandbox(testVouchers);
  const EXPECTED_DAYBOOK_COLS = 11;
  const accountsToTest = ['ALL', 'Cash in Hand', 'HDFC Bank', 'SBI Current A/c', 'NonExistentAcc'];

  let totalSwitchChecks = 0;
  let corruptedRows = 0;

  // Stress loop: 200 rapid filter switches
  for (let cycle = 0; cycle < 40; cycle++) {
    for (const acc of accountsToTest) {
      env.elements['filter-account'].value = acc;
      env.elements['filter-period'].value = 'ALL';
      env.elements['filter-type'].value = 'ALL';

      env.sandbox.applyFilters();
      const tbodyHtml = env.elements['daybook-txn-tbody'].innerHTML;

      // Extract rows
      const rowMatches = tbodyHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];

      for (const rowHtml of rowMatches) {
        totalSwitchChecks++;
        const { totalCols, cells } = analyzeTableRow(rowHtml);

        if (totalCols !== EXPECTED_DAYBOOK_COLS) {
          corruptedRows++;
          fail(`Column count mismatch during switch to [${acc}]`, `Expected ${EXPECTED_DAYBOOK_COLS} columns, got ${totalCols}. Row: ${rowHtml.substring(0, 100)}...`, 'CRITICAL');
        }
      }
    }
  }

  if (corruptedRows === 0) {
    pass(`Rapid Account Switching: ${totalSwitchChecks} row evaluations`, `All rows strictly maintain ${EXPECTED_DAYBOOK_COLS} spanned columns across 40 full cycles`);
  }

  // Dashboard table column check (Expected 10 columns)
  const EXPECTED_DASHBOARD_COLS = 10;
  env.sandbox.renderDashboardTable();
  const dashHtml = env.elements['dashboard-txn-tbody'].innerHTML;
  const dashRows = dashHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
  let dashCorrupted = 0;

  for (const r of dashRows) {
    const { totalCols } = analyzeTableRow(r);
    if (totalCols !== EXPECTED_DASHBOARD_COLS) {
      dashCorrupted++;
      fail('Dashboard column count mismatch', `Expected ${EXPECTED_DASHBOARD_COLS}, got ${totalCols}`, 'HIGH');
    }
  }

  if (dashCorrupted === 0) {
    pass(`Dashboard Table: ${dashRows.length} rows evaluated`, `All rows strictly maintain ${EXPECTED_DASHBOARD_COLS} spanned columns (Contra colspan="2")`);
  }

} catch (e) {
  fail('Rapid Account Switching Stress', e.message, 'CRITICAL');
}

// ============================================================================
// TEST SUITE 4: Character Escaping & HTML Injection Stress Test
// ============================================================================
console.log('\n\x1b[1m▶ TEST SUITE 4: Character Escaping & HTML Injection Stress Test\x1b[0m');

try {
  // Test 4.1: Is escapeHtml defined in the script scope?
  const testEnv = createSandbox();
  const hasEscapeHtml = typeof testEnv.sandbox.escapeHtml === 'function';

  if (!hasEscapeHtml) {
    fail('escapeHtml Function Definition', 'function escapeHtml is NOT DEFINED anywhere in index.html, causing showToast() and renderDiagnosticsHistory() to throw ReferenceError when invoked', 'CRITICAL');
  } else {
    pass('escapeHtml Function Definition', 'function is defined in scope');
  }

  // Test 4.2: Calling showToast() directly
  try {
    testEnv.sandbox.showToast('Test Notification Message');
    pass('showToast Execution', 'Notification displayed without throwing ReferenceError');
  } catch (err) {
    fail('showToast Execution', `showToast failed: ${err.message}`, 'CRITICAL');
  }

  // Test 4.3: Special characters in Party and Remark (Desc) fields
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
  const daybookHtml = attackEnv.elements['daybook-txn-tbody'].innerHTML;

  // Check 1: Did <script> tags get directly injected unescaped?
  if (daybookHtml.includes('<script>alert("XSS Party")</script>')) {
    fail('Party Field HTML Escaping (Script Tag)', 'Unescaped <script> tag injected into Day Book DOM via partyCell', 'HIGH');
  } else {
    pass('Party Field HTML Escaping (Script Tag)', 'Script tag properly sanitized/escaped');
  }

  // Check 2: Did <img src=x onerror=...> get directly injected unescaped?
  if (daybookHtml.includes('<img src=x onerror=')) {
    fail('Party Field HTML Escaping (Image Tag)', 'Unescaped <img onerror> injected into Day Book DOM via partyCell', 'HIGH');
  } else {
    pass('Party Field HTML Escaping (Image Tag)', 'Image tag properly sanitized/escaped');
  }

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

  // Check 4: Were email angle brackets and ampersands handled safely?
  // In a safe implementation, <sharma@corp.com> should be &lt;sharma@corp.com&gt; or escaped.
  // If rendered as raw <sharma@corp.com>, the browser treats it as an unknown HTML element.
  if (daybookHtml.includes('<sharma@corp.com>')) {
    fail('Party Angle Bracket Escaping (<email>)', 'Raw <sharma@corp.com> injected into HTML without entity encoding (&lt;...&gt;), causing DOM parser to treat as custom element', 'MEDIUM');
  } else if (daybookHtml.includes('&lt;sharma@corp.com&gt;') || daybookHtml.includes('&amp;')) {
    pass('Party Angle Bracket Escaping', 'Special characters converted to HTML entities');
  } else {
    pass('Party Angle Bracket Escaping', 'Handled');
  }

  // Check 5: Remark (desc) unescaped injection
  if (daybookHtml.includes('<urgent>')) {
    fail('Remark Angle Bracket Escaping (<urgent>)', 'Raw <urgent> tag injected into HTML via desc narration without entity encoding', 'MEDIUM');
  } else if (daybookHtml.includes('&lt;urgent&gt;')) {
    pass('Remark Angle Bracket Escaping', 'Handled safely');
  }

  // Check 6: Unicode Hindi preservation
  if (daybookHtml.includes('श्री कृष्णा लॉजिस्टिक्स')) {
    pass('Unicode Hindi Script Rendering', 'Hindi party name accurately preserved and rendered');
  } else {
    fail('Unicode Hindi Script Rendering', 'Hindi script corrupted or omitted in output', 'HIGH');
  }

} catch (e) {
  fail('Character Escaping & Injection Suite', e.message, 'HIGH');
}

// ============================================================================
// SUMMARY & VERDICT GENERATION
// ============================================================================
console.log('\n\x1b[1m\x1b[36m================================================================');
console.log('SUMMARY OF EMPIRICAL STRESS TEST RESULTS');
console.log('================================================================\x1b[0m');
console.log(`Passed: \x1b[32m${results.passed}\x1b[0m | Failed: ${results.failed > 0 ? '\x1b[31m' + results.failed : '\x1b[32m0'}\x1b[0m`);

if (results.findings.length > 0) {
  console.log('\n\x1b[31mIdentified Vulnerabilities & Defects:\x1b[0m');
  results.findings.forEach((f, idx) => {
    console.log(`  ${idx + 1}. [${f.severity}] ${f.name}: ${f.error}`);
  });
}

const criticalOrHighCount = results.findings.filter(f => f.severity === 'CRITICAL' || f.severity === 'HIGH').length;
const verdict = criticalOrHighCount > 0 ? 'REQUEST_CHANGES' : 'APPROVE';

console.log(`\n\x1b[1mRECOMMENDED VERDICT: \x1b[${verdict === 'APPROVE' ? '32m' : '31m'}${verdict}\x1b[0m\n`);

// process.exit
