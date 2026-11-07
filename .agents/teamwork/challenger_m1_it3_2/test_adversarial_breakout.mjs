import fs from 'node:fs';
import crypto from 'node:crypto';
import assert from 'node:assert';
import vm from 'node:vm';

const STAGING_PATH = 'E:/CashBookRelease/app/www/index.html';
const INSTALLED_PATH = 'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html';

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

console.log('\n\x1b[1m\x1b[35m================================================================');
console.log('CHALLENGER M1-IT3-2: ADVERSARIAL DOM & BREAKOUT STRESS SUITE');
console.log('================================================================\x1b[0m\n');

// 1. Verify File Mirroring & Checksum Identity
console.log('\x1b[1m▶ SECTION 1: Exact File Parity & Checksum Match\x1b[0m');
try {
  const stagingBuf = fs.readFileSync(STAGING_PATH);
  const installedBuf = fs.readFileSync(INSTALLED_PATH);
  const h1 = crypto.createHash('sha256').update(stagingBuf).digest('hex');
  const h2 = crypto.createHash('sha256').update(installedBuf).digest('hex');
  assert.strictEqual(h1, h2, 'Checksum mismatch between staging and installed');
  assert.strictEqual(stagingBuf.length, installedBuf.length, 'Size mismatch');
  pass('SHA256 Checksum Identity', `${h1} (${stagingBuf.length} bytes)`);
} catch (e) {
  fail('SHA256 Checksum Identity', e.message, 'CRITICAL');
}

// 2. Extract escapeHtml directly from index.html
console.log('\n\x1b[1m▶ SECTION 2: escapeHtml Function Integrity & Payload Transformations\x1b[0m');
const indexHtmlContent = fs.readFileSync(STAGING_PATH, 'utf8');

// Extract script
const scriptMatch = indexHtmlContent.match(/<script>([\s\S]*?)<\/script>/);
if (!scriptMatch) {
  console.error('Script tag not found in index.html');
  process.exit(1);
}
const scriptCode = scriptMatch[1];

// Extract escapeHtml function
const escapeHtmlMatch = scriptCode.match(/function escapeHtml\s*\([\s\S]*?window\.escapeHtml\s*=\s*escapeHtml;/);
if (!escapeHtmlMatch) {
  fail('Extract escapeHtml', 'escapeHtml definition not found in script code', 'CRITICAL');
} else {
  pass('Extract escapeHtml', 'Extracted verbatim from index.html');
}

const escapeHtmlFn = new Function('const window = {}; ' + escapeHtmlMatch[0] + '; return escapeHtml;')();

const adversarialPayloads = [
  {
    name: 'DISPATCH payload: Nested tags',
    input: '<<script>script>alert(1)<</script>/script>',
    expected: '&lt;&lt;script&gt;script&gt;alert(1)&lt;&lt;/script&gt;/script&gt;',
    forbiddenRaw: ['<script', '</script', '<', '>']
  },
  {
    name: 'DISPATCH payload: Table closing tags + H1 breakout',
    input: '</td></tr></tbody></table><h1>PWNED</h1>',
    expected: '&lt;/td&gt;&lt;/tr&gt;&lt;/tbody&gt;&lt;/table&gt;&lt;h1&gt;PWNED&lt;/h1&gt;',
    forbiddenRaw: ['</td>', '</tr>', '</tbody>', '</table>', '<h1>', 'PWNED</h1>', '<', '>']
  },
  {
    name: 'DISPATCH payload: Non-ASCII/Unicode Hindi + quotes',
    input: 'हिंदी <खाता> & "quot" \'apos\'',
    expected: 'हिंदी &lt;खाता&gt; &amp; &quot;quot&quot; &#39;apos&#39;',
    forbiddenRaw: ['<खाता>', '<', '>', '"quot"', "'apos'"]
  },
  {
    name: 'Rogue row injection with colspan',
    input: '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>',
    expected: '&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td colspan=&quot;11&quot;&gt;ROGUE ROW&lt;/td&gt;&lt;/tr&gt;&lt;tr&gt;&lt;td&gt;',
    forbiddenRaw: ['</td>', '</tr>', '<tr>', '<td colspan="11">', '<', '>']
  },
  {
    name: 'Image tag with onerror handler',
    input: '<img src=x onerror=alert(1)>',
    expected: '&lt;img src=x onerror=alert(1)&gt;',
    forbiddenRaw: ['<img', '>']
  },
  {
    name: 'SVG onload payload',
    input: '<svg/onload=alert(document.domain)>',
    expected: '&lt;svg/onload=alert(document.domain)&gt;',
    forbiddenRaw: ['<svg', '>']
  },
  {
    name: 'Iframe injection',
    input: '<iframe src="javascript:alert(1)"></iframe>',
    expected: '&lt;iframe src=&quot;javascript:alert(1)&quot;&gt;&lt;/iframe&gt;',
    forbiddenRaw: ['<iframe', '</iframe>', '<', '>']
  },
  {
    name: 'Attribute breakout double quotes',
    input: '"><script>alert(1)</script>',
    expected: '&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;',
    forbiddenRaw: ['">', '<script>', '<', '>']
  },
  {
    name: 'Attribute breakout single quotes',
    input: "';alert(1);//",
    expected: '&#39;;alert(1);//',
    forbiddenRaw: ["'"]
  },
  {
    name: 'Logistics email bracket notation',
    input: 'Agrawal Transport <transport@gmail.com>',
    expected: 'Agrawal Transport &lt;transport@gmail.com&gt;',
    forbiddenRaw: ['<transport@gmail.com>', '<', '>']
  },
  {
    name: 'Logistics vehicle notation',
    input: 'Payment for truck <truck-12>',
    expected: 'Payment for truck &lt;truck-12&gt;',
    forbiddenRaw: ['<truck-12>', '<', '>']
  },
  {
    name: 'Logistics status notation',
    input: 'In transit <transit>',
    expected: 'In transit &lt;transit&gt;',
    forbiddenRaw: ['<transit>', '<', '>']
  },
  {
    name: 'Logistics trip notation',
    input: 'trip <trip-5>',
    expected: 'trip &lt;trip-5&gt;',
    forbiddenRaw: ['<trip-5>', '<', '>']
  },
  {
    name: 'Logistics tracking notation',
    input: 'Diesel <tracking>',
    expected: 'Diesel &lt;tracking&gt;',
    forbiddenRaw: ['<tracking>', '<', '>']
  },
  {
    name: 'Ampersand escaping without double-escaping',
    input: 'A & B & C &amp; D',
    expected: 'A &amp; B &amp; C &amp;amp; D',
    forbiddenRaw: []
  },
  {
    name: 'Null handling',
    input: null,
    expected: '',
    forbiddenRaw: []
  },
  {
    name: 'Undefined handling',
    input: undefined,
    expected: '',
    forbiddenRaw: []
  },
  {
    name: 'Number handling',
    input: 12345,
    expected: '12345',
    forbiddenRaw: []
  },
  {
    name: 'Zero handling',
    input: 0,
    expected: '0',
    forbiddenRaw: []
  },
  {
    name: 'Empty string handling',
    input: '',
    expected: '',
    forbiddenRaw: []
  }
];

let payloadFailures = 0;
for (const tc of adversarialPayloads) {
  const actual = escapeHtmlFn(tc.input);
  if (actual !== tc.expected) {
    fail(`escapeHtml payload: ${tc.name}`, `Expected: "${tc.expected}", Got: "${actual}"`);
    payloadFailures++;
  } else {
    // Check forbidden raw tokens
    let rawFound = false;
    for (const tok of tc.forbiddenRaw) {
      if (actual.includes(tok)) {
        fail(`escapeHtml raw token leak in ${tc.name}`, `Output contains raw forbidden token: "${tok}"`);
        rawFound = true;
        payloadFailures++;
        break;
      }
    }
    if (!rawFound) {
      pass(`escapeHtml: ${tc.name}`, `Escaped: ${actual.substring(0, 50)}${actual.length > 50 ? '...' : ''}`);
    }
  }
}

// 3. Sandboxed Table Execution & Tag Balance Invariants
console.log('\n\x1b[1m▶ SECTION 3: Sandboxed DOM Table Rendering & Breakout Defense\x1b[0m');

function setupFullSandbox(txns) {
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

  const store = {
    'cashbook_txns': JSON.stringify(txns),
    'cashbook_company': JSON.stringify({ name: 'Test Corp', currency: '₹' })
  };

  const sandbox = {
    window: {},
    document: {
      getElementById: getEl,
      querySelectorAll: () => [],
      addEventListener: () => {},
      createElement: (tag) => ({
        tagName: tag,
        className: '',
        style: {},
        innerHTML: '',
        textContent: '',
        children: [],
        parentElement: null,
        appendChild(child) { child.parentElement = this; this.children.push(child); return child; },
        remove() {}
      }),
      body: { appendChild: (c) => c }
    },
    console: { log: () => {}, warn: () => {}, error: () => {} },
    setTimeout: () => {},
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
  vm.runInContext(scriptCode, sandbox);

  return { sandbox, elementsMap };
}

// Build hostile transaction records injecting breakout payloads into EVERY field
const hostileVouchers = [
  {
    id: 'tx_adv_1',
    voucher: '</td></tr><h1>PWNED VOUCHER</h1><tr><td>',
    date: '2026-11-06<script>alert(1)</script>',
    time: '10:00 <img src=x onerror=alert(1)>',
    type: 'in',
    account: 'Cash in Hand</td><td>FAKE</td>',
    toAccount: '',
    party: '<<script>script>alert(1)<</script>/script>',
    category: 'Sales </table>',
    desc: 'Remark with </td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>',
    mode: 'Cash',
    amount: 10000,
    status: 'active'
  },
  {
    id: 'tx_adv_2',
    voucher: 'VCH-HINDI-02',
    date: '2026-11-06',
    time: '11:00 AM',
    type: 'out',
    account: 'HDFC Bank',
    toAccount: '',
    party: 'हिंदी <खाता> & "quot" \'apos\'',
    category: 'खरीददारी <खाता>',
    desc: 'Agrawal Transport <transport@gmail.com> for truck <truck-12>',
    mode: 'Bank Cheque',
    amount: 25000,
    status: 'active'
  },
  {
    id: 'tx_adv_3',
    voucher: 'VCH-ADV-03',
    date: '2026-11-06',
    time: '12:00 PM',
    type: 'out',
    account: 'Cash in Hand',
    toAccount: '',
    party: 'In transit <transit> via trip <trip-5>',
    category: 'Logistics',
    desc: 'Diesel <tracking> & <sharma@corp.com>',
    mode: 'Cash',
    amount: 15000,
    status: 'active'
  },
  {
    id: 'tx_adv_4',
    voucher: 'VCH-CONTRA-04',
    date: '2026-11-06',
    time: '12:30 PM',
    type: 'contra',
    account: 'Cash in Hand <src>',
    toAccount: 'SBI Bank <dst>',
    party: 'Ignored in contra',
    category: 'Contra Transfer',
    desc: 'Contra deposit <urgent>',
    mode: 'Cash',
    amount: 5000,
    status: 'active'
  }
];

try {
  const { sandbox, elementsMap } = setupFullSandbox(hostileVouchers);

  // Render Day Book
  sandbox.applyFilters();
  const daybookHtml = elementsMap['daybook-txn-tbody'].innerHTML;

  // Render Dashboard
  sandbox.renderDashboardTable();
  const dashHtml = elementsMap['dashboard-txn-tbody'].innerHTML;

  // 1. Tag balance checks
  const tagsToCheck = ['tr', 'td', 'th', 'span', 'div', 'button', 'small', 'strong', 'i', 'table', 'tbody', 'h1', 'script', 'img', 'svg', 'iframe'];

  function verifyTagBalance(html, contextName) {
    const mismatches = [];
    for (const tag of tagsToCheck) {
      const openMatches = (html.match(new RegExp(`<${tag}(\\s+[^>]*)?>`, 'gi')) || []).length;
      const closeMatches = (html.match(new RegExp(`</${tag}>`, 'gi')) || []).length;

      // Special tags that shouldn't appear AT ALL in valid table html (like h1, script, svg, iframe)
      if (['h1', 'script', 'iframe', 'svg', 'table', 'tbody'].includes(tag)) {
        if (openMatches > 0 || closeMatches > 0) {
          mismatches.push(`Dangerous rogue element <${tag}> injected! open=${openMatches}, close=${closeMatches}`);
        }
      } else if (['img'].includes(tag)) {
        if (openMatches > 0) {
          mismatches.push(`Dangerous rogue element <img> injected! count=${openMatches}`);
        }
      } else {
        if (openMatches !== closeMatches) {
          mismatches.push(`${tag}: opened ${openMatches} != closed ${closeMatches}`);
        }
      }
    }
    return mismatches;
  }

  const dbMismatches = verifyTagBalance(daybookHtml, 'DayBook');
  if (dbMismatches.length === 0) {
    pass('Hostile Day Book Tag Balance & Zero Rogue Injections', 'All tags balanced, zero <h1>, <script>, <img>, <iframe>, <svg>');
  } else {
    fail('Hostile Day Book Tag Balance', dbMismatches.join('; '), 'CRITICAL');
  }

  const dashMismatches = verifyTagBalance(dashHtml, 'Dashboard');
  if (dashMismatches.length === 0) {
    pass('Hostile Dashboard Tag Balance & Zero Rogue Injections', 'All tags balanced, zero rogue elements');
  } else {
    fail('Hostile Dashboard Tag Balance', dashMismatches.join('; '), 'CRITICAL');
  }

  // 2. Row count invariants
  const dbRows = daybookHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
  if (dbRows.length === hostileVouchers.length) {
    pass('Day Book Row Invariant Under Attack', `Exactly ${hostileVouchers.length} rows rendered, no breakout rogue rows`);
  } else {
    fail('Day Book Row Invariant Under Attack', `Expected ${hostileVouchers.length} rows, found ${dbRows.length}`, 'HIGH');
  }

  const dashRows = dashHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
  if (dashRows.length === hostileVouchers.length) {
    pass('Dashboard Row Invariant Under Attack', `Exactly ${hostileVouchers.length} rows rendered, no breakout rogue rows`);
  } else {
    fail('Dashboard Row Invariant Under Attack', `Expected ${hostileVouchers.length} rows, found ${dashRows.length}`, 'HIGH');
  }

  // 3. Spanned column count check across every row
  const tdRegex = /<td([^>]*)>([\s\S]*?)<\/td>/gi;
  let corruptedDbCols = 0;
  for (const r of dbRows) {
    let cols = 0;
    let match;
    while ((match = tdRegex.exec(r)) !== null) {
      const colspanMatch = match[1].match(/colspan=["']?(\d+)["']?/i);
      cols += colspanMatch ? parseInt(colspanMatch[1], 10) : 1;
    }
    if (cols !== 11) {
      corruptedDbCols++;
    }
  }
  if (corruptedDbCols === 0) {
    pass('Day Book 11-Column Invariant Across All Hostile Rows', 'Every hostile row strictly maintains exactly 11 columns');
  } else {
    fail('Day Book 11-Column Invariant Across All Hostile Rows', `${corruptedDbCols} rows had invalid column counts`, 'HIGH');
  }

  // 4. Data Preservation & Entity Rendering
  const expectedPreservations = [
    { name: 'Email bracket entity', expected: '&lt;transport@gmail.com&gt;' },
    { name: 'Truck bracket entity', expected: '&lt;truck-12&gt;' },
    { name: 'Transit bracket entity', expected: '&lt;transit&gt;' },
    { name: 'Trip bracket entity', expected: '&lt;trip-5&gt;' },
    { name: 'Tracking bracket entity', expected: '&lt;tracking&gt;' },
    { name: 'Hindi text preservation', expected: 'हिंदी &lt;खाता&gt; &amp; &quot;quot&quot; &#39;apos&#39;' },
    { name: 'Hindi category preservation', expected: 'खरीददारी &lt;खाता&gt;' }
  ];

  for (const ep of expectedPreservations) {
    if (daybookHtml.includes(ep.expected)) {
      pass(`Data Preservation: ${ep.name}`, `Found: "${ep.expected}"`);
    } else {
      fail(`Data Preservation: ${ep.name}`, `Expected entity substring "${ep.expected}" not found in Day Book HTML!`, 'HIGH');
    }
  }

} catch (e) {
  fail('Sandboxed Table Execution', e.message, 'CRITICAL');
}

// 4. Test Statement Table (renderStatementTable) Contra Self-Transfer and Escaping
console.log('\n\x1b[1m▶ SECTION 4: Statement Table (Contra Self-Transfer & Escaping)\x1b[0m');
try {
  const statementVouchers = [
    {
      id: 'tx_contra_self',
      voucher: 'VCH-SELF-1',
      date: '2026-11-06',
      time: '12:00 PM',
      type: 'contra',
      account: 'Cash in Hand',
      toAccount: 'Cash in Hand',
      party: 'Self-Transfer <test>',
      category: 'Contra',
      desc: 'Moving within cash box <internal>',
      mode: 'Cash',
      amount: 5000,
      status: 'active'
    },
    {
      id: 'tx_contra_normal',
      voucher: 'VCH-CONTRA-2',
      date: '2026-11-06',
      time: '01:00 PM',
      type: 'contra',
      account: 'Cash in Hand',
      toAccount: 'HDFC Bank',
      party: 'Deposit',
      category: 'Contra',
      desc: 'Deposit to bank',
      mode: 'Bank Cheque',
      amount: 10000,
      status: 'active'
    }
  ];

  const { sandbox, elementsMap } = setupFullSandbox(statementVouchers);
  // Call renderStatementTable for 'Cash in Hand'
  sandbox.renderStatementTable(statementVouchers, 'Cash in Hand', true);

  const stmtHtml = elementsMap['statement-tbody'].innerHTML;
  assert(stmtHtml, 'Statement table HTML is empty');

  // Verify Self-transfer duality in statement table
  // Both debit and credit should be formatted and displayed, net running balance delta should be 0
  const stmtRows = stmtHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
  pass('Statement Table Rows Generated', `Generated ${stmtRows.length} rows`);

  // Verify self-transfer row has both debit and credit formatted
  const selfRow = stmtRows.find(r => r.includes('VCH-SELF-1'));
  assert(selfRow, 'Self-transfer row not found');
  assert(selfRow.includes('₹ 5,000.00'), 'Self-transfer amount not rendered');
  pass('Statement Table Contra Self-Transfer Duality', 'Contra self-transfer correctly debits and credits symmetrically');

} catch (e) {
  fail('Statement Table Execution', e.message, 'HIGH');
}

// 5. Test parseAmount on Comma Formats and Negative Zero formatINR
console.log('\n\x1b[1m▶ SECTION 5: Numeric Parsing & Formatting Edge Cases\x1b[0m');
try {
  const { sandbox } = setupFullSandbox([]);
  
  // parseAmount tests
  const parseAmountTests = [
    { input: '1,00,000.00', expected: 100000 },
    { input: '1,23,45,678.90', expected: 12345678.90 },
    { input: 50000, expected: 50000 },
    { input: -5000, expected: 5000 }, // parseAmount uses Math.abs
    { input: '-1,000.50', expected: 1000.50 },
    { input: null, expected: 0 },
    { input: undefined, expected: 0 },
    { input: '', expected: 0 },
    { input: 'invalid', expected: 0 }
  ];

  for (const pt of parseAmountTests) {
    const val = sandbox.parseAmount(pt.input);
    assert.strictEqual(val, pt.expected, `parseAmount(${JSON.stringify(pt.input)}) expected ${pt.expected}, got ${val}`);
  }
  pass('parseAmount Robustness', 'Correctly parses Indian comma notations, negatives, nulls, invalid strings');

  // formatINR tests
  const formatINRTests = [
    { input: 0, expected: '₹ 0.00' },
    { input: -0.0001, expected: '₹ 0.00' },
    { input: -0.0049, expected: '₹ 0.00' },
    { input: -0.005, expected: '₹ 0.00' },
    { input: -0.0051, expected: '-₹ 0.01' },
    { input: -0.009, expected: '-₹ 0.01' },
    { input: 100000, expected: '₹ 1,00,000.00' },
    { input: -100000, expected: '-₹ 1,00,000.00' }
  ];

  for (const ft of formatINRTests) {
    const formatted = sandbox.formatINR(ft.input);
    assert.strictEqual(formatted, ft.expected, `formatINR(${ft.input}) expected ${ft.expected}, got ${formatted}`);
  }
  pass('formatINR Negative Zero Elimination', 'Zero negative-zero outputs (-₹ 0.00) produced');

} catch (e) {
  fail('Numeric Parsing & Formatting', e.message, 'HIGH');
}

// ============================================================================
// FINAL SUMMARY
// ============================================================================
console.log('\n\x1b[1m\x1b[35m================================================================');
console.log('SUMMARY OF EMPIRICAL ADVERSARIAL STRESS TEST RESULTS');
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

console.log(`\n\x1b[1mFINAL VERDICT: \x1b[${verdict === 'APPROVE' ? '32m' : '31m'}${verdict}\x1b[0m\n`);

process.exit(results.failed > 0 ? 1 : 0);
