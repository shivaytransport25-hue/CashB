import fs from 'fs';
import vm from 'vm';

const html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

// Extract script content
const scriptMatch = html.match(/<script>([\s\S]*?)<\/script>\s*<\/body>/i);
if (!scriptMatch) {
  throw new Error('Could not extract main script tag');
}
const scriptCode = scriptMatch[1];

// Create simulated environment
const sandbox = {
  window: {
    addEventListener: () => {},
    removeEventListener: () => {}
  },
  addEventListener: () => {},
  removeEventListener: () => {},
  document: {
    getElementById: (id) => null,
    querySelectorAll: (sel) => [],
    addEventListener: () => {},
    removeEventListener: () => {},
    createElement: (tag) => ({
      className: '',
      id: '',
      innerHTML: '',
      appendChild: () => {},
      classList: { add: () => {}, remove: () => {}, toggle: () => false }
    }),
    body: {
      appendChild: () => {}
    }
  },
  navigator: { userAgent: 'Auditor-Node' },
  localStorage: {
    getItem: () => null,
    setItem: () => {}
  },
  console: console,
  setTimeout: setTimeout,
  clearTimeout: clearTimeout,
  setInterval: () => {},
  clearInterval: () => {},
  Math: Math,
  Date: Date,
  parseFloat: parseFloat,
  String: String,
  Number: Number,
  JSON: JSON
};
sandbox.window = sandbox;

vm.createContext(sandbox);

// Execute up to accounting functions
vm.runInContext(scriptCode, sandbox);

console.log('=== ADVERSARIAL EDGE CASE VALIDATION ===');

// 1. escapeHtml tests
const escapeHtml = sandbox.window.escapeHtml || sandbox.escapeHtml;
console.log('1. escapeHtml exists:', typeof escapeHtml === 'function');

const escapeCases = [
  { in: null, expected: '' },
  { in: undefined, expected: '' },
  { in: 'Normal Text', expected: 'Normal Text' },
  { in: '<script>alert(1)</script>', expected: '&lt;script&gt;alert(1)&lt;/script&gt;' },
  { in: '<img src=x onerror=alert(1)>', expected: '&lt;img src=x onerror=alert(1)&gt;' },
  { in: '</td></tr><tr><td>rogue</td></tr>', expected: '' },
  { in: '<b>Safe bold</b>', expected: '&lt;b&gt;Safe bold&lt;/b&gt;' },
  { in: 'A & B "C" \'D\'', expected: 'A &amp; B &quot;C&quot; &#39;D&#39;' }
];

let escapePass = true;
for (const tc of escapeCases) {
  const actual = escapeHtml(tc.in);
  const ok = actual === tc.expected;
  if (!ok) {
    console.error(`FAIL: escapeHtml(${JSON.stringify(tc.in)}) => ${JSON.stringify(actual)}, expected ${JSON.stringify(tc.expected)}`);
    escapePass = false;
  }
}
console.log('   escapeHtml all edge cases pass:', escapePass);

// 2. formatINR tests
const formatINR = sandbox.formatINR;
console.log('2. formatINR exists:', typeof formatINR === 'function');

const inrCases = [
  { in: 0, expected: '₹ 0.00' },
  { in: 1000, expected: '₹ 1,000.00' },
  { in: -40000, expected: '-₹ 40,000.00' },
  { in: '1,000.00', expected: '₹ 1,000.00' },
  { in: '-1,25,000.50', expected: '-₹ 1,25,000.50' },
  { in: 0.1, expected: '₹ 0.10' }
];

let inrPass = true;
for (const tc of inrCases) {
  const actual = formatINR(tc.in);
  const ok = actual === tc.expected;
  if (!ok) {
    console.error(`FAIL: formatINR(${tc.in}) => "${actual}", expected "${tc.expected}"`);
    inrPass = false;
  }
}
console.log('   formatINR all edge cases pass:', inrPass);

// 3. getTransactionsWithRunningBal tests
const getTransactionsWithRunningBal = sandbox.getTransactionsWithRunningBal;
console.log('3. getTransactionsWithRunningBal exists:', typeof getTransactionsWithRunningBal === 'function');

// 3a. Float rounding leakage check
const floatTx = Array.from({ length: 100 }, (_, i) => ({
  id: `tx_${i}`,
  date: '2026-11-06',
  time: String(i).padStart(4, '0'),
  type: 'in',
  amount: 0.10,
  account: 'Cash',
  status: 'active'
}));

const resFloat = getTransactionsWithRunningBal(floatTx, 'ALL');
const lastBal = resFloat[resFloat.length - 1].runningBal;
console.log('   Float test (100 * 0.10): final balance =', lastBal, '(exact match 10:', lastBal === 10, ')');

// 3b. Contra self-transfer check
const contraSelfTx = [
  { id: 'tx_init', date: '2026-11-06', time: '10:00', type: 'in', amount: 10000, account: 'Cash', status: 'active' },
  { id: 'tx_self', date: '2026-11-06', time: '10:05', type: 'contra', amount: 5000, account: 'Cash', toAccount: 'Cash', status: 'active' }
];

const resContraCash = getTransactionsWithRunningBal(contraSelfTx, 'Cash');
console.log('   Contra self-transfer in Cash ledger:');
console.log('     Step 0 (inflow 10000): bal =', resContraCash[0].runningBal);
console.log('     Step 1 (self-transfer 5000): bal =', resContraCash[1].runningBal, '(expected 10000:', resContraCash[1].runningBal === 10000, ')');

// 3c. Comma formatted strings in transactions
const commaTx = [
  { id: 'tx_1', date: '2026-11-06', time: '10:00', type: 'in', amount: '1,000.00', account: 'Cash', status: 'active' },
  { id: 'tx_2', date: '2026-11-06', time: '10:01', type: 'in', amount: '25,000.50', account: 'Cash', status: 'active' }
];

const resComma = getTransactionsWithRunningBal(commaTx, 'ALL');
console.log('   Comma string amounts:');
console.log('     Step 1 (1,000.00): bal =', resComma[0].runningBal, '(expected 1000:', resComma[0].runningBal === 1000, ')');
console.log('     Step 2 (25,000.50): bal =', resComma[1].runningBal, '(expected 26000.5:', resComma[1].runningBal === 26000.5, ')');

console.log('=== ALL EMPIRICAL ADVERSARIAL VALIDATIONS COMPLETED ===');
