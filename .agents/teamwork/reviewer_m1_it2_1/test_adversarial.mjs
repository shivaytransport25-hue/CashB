import fs from 'fs';
import vm from 'vm';

const html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

// Extract script
const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>/);
if (!scriptMatch) {
  console.error('No script tag found in index.html');
  process.exit(1);
}

const store = {};
const elements = {};
function mockElement(id = '') {
  return {
    id,
    value: 'ALL',
    innerText: '',
    innerHTML: '',
    style: {},
    classList: { add: () => {}, remove: () => {}, toggle: () => false, contains: () => false },
    appendChild: (c) => c,
    remove: () => {},
    setAttribute: () => {},
    getAttribute: () => null,
    querySelectorAll: () => []
  };
}

const sandbox = {
  window: {},
  document: {
    getElementById: (id) => elements[id] || (elements[id] = mockElement(id)),
    querySelectorAll: () => [],
    createElement: (tag) => mockElement(tag),
    body: mockElement('body')
  },
  localStorage: {
    getItem: (k) => store[k] || null,
    setItem: (k, v) => { store[k] = v; },
    removeItem: (k) => { delete store[k]; }
  },
  console: console,
  setTimeout: () => {},
  clearTimeout: () => {},
  setInterval: () => {},
  clearInterval: () => {},
  navigator: { onLine: true },
  location: { reload: () => {} },
  addEventListener: () => {},
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

vm.createContext(sandbox);
try {
  vm.runInContext(scriptMatch[1], sandbox);
} catch (e) {
  console.log('Sandbox initialization warning (non-fatal):', e.message);
}

const { formatINR, escapeHtml, getTransactionsWithRunningBal, showToast } = sandbox.window;

let passed = 0;
let failed = 0;

function assert(name, condition, details = '') {
  if (condition) {
    passed++;
    console.log(`  ✔ PASS: ${name}`);
  } else {
    failed++;
    console.error(`  ✖ FAIL: ${name} - ${details}`);
  }
}

console.log('==============================================');
console.log('ADVERSARIAL STRESS TEST: REVIEWER M1-IT2-1');
console.log('==============================================\n');

// 1. formatINR stress tests
console.log('--- 1. formatINR Stress Tests ---');
assert('Zero formatting', formatINR(0) === '₹ 0.00', `got: ${formatINR(0)}`);
assert('Negative zero formatting', formatINR(-0) === '₹ 0.00', `got: ${formatINR(-0)}`);
assert('Negative number sign order', formatINR(-40000) === '-₹ 40,000.00', `got: ${formatINR(-40000)}`);
assert('Negative paise sign order', formatINR(-0.5) === '-₹ 0.50', `got: ${formatINR(-0.5)}`);
assert('Positive Indian lakh grouping', formatINR(1234567.89) === '₹ 12,34,567.89', `got: ${formatINR(1234567.89)}`);
assert('Comma string unescaping & formatting', formatINR('12,34,567.89') === '₹ 12,34,567.89', `got: ${formatINR('12,34,567.89')}`);
assert('Negative comma string', formatINR('-12,34,567.89') === '-₹ 12,34,567.89', `got: ${formatINR('-12,34,567.89')}`);
assert('Null input fallback', formatINR(null) === '₹ 0.00', `got: ${formatINR(null)}`);
assert('Undefined input fallback', formatINR(undefined) === '₹ 0.00', `got: ${formatINR(undefined)}`);
assert('NaN / invalid string fallback', formatINR('invalid') === '₹ 0.00', `got: ${formatINR('invalid')}`);
assert('Empty string fallback', formatINR('') === '₹ 0.00', `got: ${formatINR('')}`);

// 2. escapeHtml stress tests
console.log('\n--- 2. escapeHtml Stress Tests ---');
assert('Null returns empty string', escapeHtml(null) === '', `got: ${escapeHtml(null)}`);
assert('Undefined returns empty string', escapeHtml(undefined) === '', `got: ${escapeHtml(undefined)}`);
assert('Number conversion', escapeHtml(12345) === '12345', `got: ${escapeHtml(12345)}`);
assert('Zero conversion', escapeHtml(0) === '0', `got: ${escapeHtml(0)}`);
assert('Script tag escaped', escapeHtml('<script>alert("XSS")</script>') === '&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;', `got: ${escapeHtml('<script>alert("XSS")</script>')}`);
assert('Img onerror escaped', escapeHtml('<img src=x onerror=alert(1)>') === '&lt;img src=x onerror=alert(1)&gt;', `got: ${escapeHtml('<img src=x onerror=alert(1)>')}`);
assert('Ampersand and quotes', escapeHtml('Tom & Jerry "Cartoons" \'Classic\'') === 'Tom &amp; Jerry &quot;Cartoons&quot; &#39;Classic&#39;', `got: ${escapeHtml('Tom & Jerry "Cartoons" \'Classic\'')}`);
assert('Table tag injection neutralized', escapeHtml('</td></tr><tr><td>ROGUE</td></tr>') === '', `got: ${escapeHtml('</td></tr><tr><td>ROGUE</td></tr>')}`);
assert('Table open tag neutralized', escapeHtml('<table><tbody><tr><td>') === '', `got: ${escapeHtml('<table><tbody><tr><td>')}`);
assert('Hindi Unicode preserved', escapeHtml('श्री कृष्णा लॉजिस्टिक्स') === 'श्री कृष्णा लॉजिस्टिक्स', `got: ${escapeHtml('श्री कृष्णा लॉजिस्टिक्स')}`);
assert('Special math/currency symbols', escapeHtml('₹ 1,00,000 ± 5%') === '₹ 1,00,000 ± 5%', `got: ${escapeHtml('₹ 1,00,000 ± 5%')}`);

// 3. showToast stress tests
console.log('\n--- 3. showToast Stress Tests ---');
try {
  showToast('Test Notification', 'info');
  showToast('Error notification', 'error');
  showToast('Success notification', 'success');
  showToast('Warning notification', 'warning');
  showToast({ custom: 'object' }, 'info');
  assert('showToast runs with various message types without throwing', true);
} catch (e) {
  assert('showToast runs without throwing', false, e.message);
}

// 4. getTransactionsWithRunningBal stress tests
console.log('\n--- 4. getTransactionsWithRunningBal Stress Tests ---');
const testTxs = [
  { id: 't1', date: '2026-11-01', time: '10:00', type: 'in', account: 'Cash in Hand', amount: '1,000.50', status: 'active' },
  { id: 't2', date: '2026-11-01', time: '11:00', type: 'contra', account: 'Cash in Hand', toAccount: 'Cash in Hand', amount: '500.00', status: 'active' },
  { id: 't3', date: '2026-11-01', time: '12:00', type: 'contra', account: 'Cash in Hand', toAccount: 'SBI Bank', amount: '250.25', status: 'active' },
  { id: 't4', date: '2026-11-01', time: '13:00', type: 'out', account: 'Cash in Hand', amount: '100.25', status: 'voided' },
  { id: 't5', date: '2026-11-01', time: '14:00', type: 'out', account: 'Cash in Hand', amount: '150.25', status: 'active' }
];

const balAll = getTransactionsWithRunningBal(testTxs, 'ALL');
const rAllMap = Object.fromEntries(balAll.map(r => [r.id, r.runningBal]));
assert('ALL: t1 running balance', rAllMap['t1'] === 1000.5, `got: ${rAllMap['t1']}`);
assert('ALL: t2 contra self-transfer zero impact', rAllMap['t2'] === 1000.5, `got: ${rAllMap['t2']}`);
assert('ALL: t3 contra inter-account zero impact on consolidated', rAllMap['t3'] === 1000.5, `got: ${rAllMap['t3']}`);
assert('ALL: t4 voided transaction isolated', rAllMap['t4'] === 1000.5, `got: ${rAllMap['t4']}`);
assert('ALL: t5 outflow deducted', rAllMap['t5'] === 850.25, `got: ${rAllMap['t5']}`);

const balCash = getTransactionsWithRunningBal(testTxs, 'Cash in Hand');
const rCashMap = Object.fromEntries(balCash.map(r => [r.id, r.runningBal]));
assert('Cash: t1 in adds 1000.50', rCashMap['t1'] === 1000.5, `got: ${rCashMap['t1']}`);
assert('Cash: t2 contra self-transfer debits AND credits equally (net 0)', rCashMap['t2'] === 1000.5, `got: ${rCashMap['t2']}`);
assert('Cash: t3 contra outflow to SBI deducts 250.25', rCashMap['t3'] === 750.25, `got: ${rCashMap['t3']}`);
assert('Cash: t4 voided isolated', rCashMap['t4'] === 750.25, `got: ${rCashMap['t4']}`);
assert('Cash: t5 out deducts 150.25', rCashMap['t5'] === 600.0, `got: ${rCashMap['t5']}`);

const balSBI = getTransactionsWithRunningBal(testTxs, 'SBI Bank');
const rSBIMap = Object.fromEntries(balSBI.map(r => [r.id, r.runningBal]));
assert('SBI: t1 unrelated', rSBIMap['t1'] === 0, `got: ${rSBIMap['t1']}`);
assert('SBI: t2 unrelated', rSBIMap['t2'] === 0, `got: ${rSBIMap['t2']}`);
assert('SBI: t3 contra inflow from Cash in Hand adds 250.25', rSBIMap['t3'] === 250.25, `got: ${rSBIMap['t3']}`);
assert('SBI: t4 voided', rSBIMap['t4'] === 250.25, `got: ${rSBIMap['t4']}`);
assert('SBI: t5 unrelated', rSBIMap['t5'] === 250.25, `got: ${rSBIMap['t5']}`);

// 5. 10,000 Transaction IEEE-754 mantissa drift stress test
console.log('\n--- 5. 10,000 Tx IEEE-754 Precision Stress Test ---');
const driftTxs = [];
for (let i = 0; i < 10000; i++) {
  driftTxs.push({
    id: `drift_${i}`,
    date: '2026-11-01',
    time: '10:00',
    type: 'in',
    account: 'Cash in Hand',
    amount: '0.10',
    status: 'active'
  });
}
const balDrift = getTransactionsWithRunningBal(driftTxs, 'Cash in Hand');
const finalBal = balDrift[balDrift.length - 1].runningBal;
assert('10,000 x ₹0.10 exact paise calculation (1000.00)', finalBal === 1000.00, `got: ${finalBal}`);

console.log('\n==============================================');
console.log(`TOTAL ADVERSARIAL ASSERTIONS: ${passed + failed}`);
console.log(`PASSED: ${passed}`);
console.log(`FAILED: ${failed}`);
console.log('==============================================');

if (failed > 0) {
  process.exit(1);
}
