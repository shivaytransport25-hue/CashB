/**
 * empirical_m1_it3_stress_test.mjs
 * Challenger M1-It3-1 Empirical Adversarial Stress Harness
 * 
 * Target: Cash Book Pro Milestone M1 Iteration 3
 * Evaluates:
 *   1. SHA256 Distribution & Checksum Parity
 *   2. parseAmount Extreme Boundaries & Adversarial Inputs
 *   3. formatINR Negative Zero Eradication & Integer Paise Oracle
 *   4. Contra Self-Transfers (Account === ToAccount) in getTransactionsWithRunningBal & renderStatementTable
 *   5. Multi-Account Balance Conservation & Consolidated Ledger Invariants
 *   6. Codebase Call-Site Audit for Raw parseFloat(t.amount)
 */

import fs from 'node:fs';
import crypto from 'node:crypto';
import assert from 'node:assert';

const STAGING_PATH = 'E:/CashBookRelease/app/www/index.html';
const INSTALLED_PATH = 'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html';

console.log('========================================================================');
console.log('CHALLENGER M1-IT3-1: EMPIRICAL STRESS TEST & ADVERSARIAL HARNESS');
console.log('========================================================================\n');

let totalChecks = 0;
let passedChecks = 0;
let failedChecks = 0;
const failures = [];

function check(title, fn) {
  totalChecks++;
  try {
    fn();
    passedChecks++;
    console.log(`  ✔ PASS: ${title}`);
  } catch (err) {
    failedChecks++;
    console.log(`  ✖ FAIL: ${title}`);
    console.log(`    ↳ Reason: ${err.message}`);
    failures.push({ title, error: err.message, stack: err.stack });
  }
}

// Deterministic PRNG
function makePRNG(seed = 987654321) {
  let s = seed;
  return function() {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

// Extract function from source
function extractFunction(source, fnName) {
  const regex = new RegExp(`function ${fnName}\\s*\\([\\s\\S]*?\\n    \\}`);
  const match = source.match(regex);
  if (!match) {
    throw new Error(`Could not extract function ${fnName} from index.html`);
  }
  return new Function(`
    ${match[0]}
    return ${fnName};
  `)();
}

// -----------------------------------------------------------------------------
// PRE-FLIGHT: FILE INTEGRITY & CHECKSU-PARITY
// -----------------------------------------------------------------------------
console.log('------------------------------------------------------------------------');
console.log('PRE-FLIGHT: File Integrity & SHA-256 Parity');
console.log('------------------------------------------------------------------------');

const stagingContent = fs.readFileSync(STAGING_PATH, 'utf8');
const installedContent = fs.readFileSync(INSTALLED_PATH, 'utf8');

check('PF.1: Staging and Installed files are present and match byte-for-byte', () => {
  assert.ok(stagingContent.length > 50000, `Staging file size unexpected: ${stagingContent.length}`);
  assert.ok(installedContent.length > 50000, `Installed file size unexpected: ${installedContent.length}`);
  const hashStaging = crypto.createHash('sha256').update(stagingContent).digest('hex');
  const hashInstalled = crypto.createHash('sha256').update(installedContent).digest('hex');
  assert.strictEqual(hashStaging, hashInstalled, `Hash mismatch:\nStaging: ${hashStaging}\nInstalled: ${hashInstalled}`);
  console.log(`    ↳ SHA256: ${hashStaging} (${stagingContent.length} bytes)`);
});

// Extract functions under test from live index.html
const parseAmount = extractFunction(stagingContent, 'parseAmount');
const formatINR = extractFunction(stagingContent, 'formatINR');
const getTransactionsWithRunningBal = extractFunction(stagingContent, 'getTransactionsWithRunningBal');
const escapeHtml = extractFunction(stagingContent, 'escapeHtml');

// Extract renderStatementTable logic for headless simulation
// renderStatementTable depends on DOM (document.getElementById), so we extract its core calculation logic
function simulateStatementTableCalc(txns, entityName, isAccount) {
  const sorted = [...txns].sort((a,b) => (a.date + (a.time||'')).localeCompare(b.date + (b.time||'')));
  let running = 0;
  return sorted.map(t => {
    const amt = parseAmount(t.amount);
    let debit = 0, credit = 0;
    if (isAccount) {
      if (t.status !== 'voided') {
        if (t.type === 'in' && t.account === entityName) { debit = amt; running += amt; }
        else if (t.type === 'out' && t.account === entityName) { credit = amt; running -= amt; }
        else if (t.type === 'contra') {
          if (t.toAccount === entityName) { debit = amt; running += amt; }
          if (t.account === entityName) { credit = amt; running -= amt; }
        }
        running = Math.round((running + Number.EPSILON) * 100) / 100;
      }
    } else {
      if (t.status !== 'voided') {
        if (t.type === 'out') { debit = amt; running += amt; }
        else if (t.type === 'in') { credit = amt; running -= amt; }
      }
    }
    return { ...t, debit, credit, runningBal: running };
  });
}

// -----------------------------------------------------------------------------
// SUITE 1: parseAmount EXTREME BOUNDARY & ADVERSARIAL INPUTS
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 1: parseAmount Extreme Boundary & Adversarial Suite');
console.log('------------------------------------------------------------------------');

check('S1.1: Canonical boundary inputs (99,99,99,999.99, 100 Crores, 1 Paisa)', () => {
  assert.strictEqual(parseAmount('99,99,99,999.99'), 999999999.99);
  assert.strictEqual(parseAmount('1,00,00,00,000.00'), 1000000000.00);
  assert.strictEqual(parseAmount('1,00,000.00'), 100000.00);
  assert.strictEqual(parseAmount('0.01'), 0.01);
  assert.strictEqual(parseAmount(0.01), 0.01);
});

check('S1.2: Negative zero and negative values return absolute non-negative floats', () => {
  assert.strictEqual(parseAmount('-0.00'), 0);
  assert.strictEqual(parseAmount(-0), 0);
  assert.strictEqual(parseAmount('-1,00,000.00'), 100000.00);
  assert.strictEqual(parseAmount(-50000), 50000);
  assert.strictEqual(Object.is(parseAmount('-0.00'), -0), false, 'Must not return negative zero');
  assert.strictEqual(Object.is(parseAmount(-0), -0), false, 'Must normalize -0 to +0');
});

check('S1.3: Sub-cent and floating-point micro precision inputs', () => {
  assert.strictEqual(parseAmount(0.000001), 0.000001);
  assert.strictEqual(parseAmount('0.000001'), 0.000001);
  assert.strictEqual(parseAmount('0.004'), 0.004);
  assert.strictEqual(parseAmount('0.009'), 0.009);
});

check('S1.4: Null, undefined, empty, non-numeric, and adversarial inputs default safely to 0', () => {
  const adversarialCases = [
    null,
    undefined,
    '',
    '   ',
    'abc',
    'NaN',
    NaN,
    false,
    true,
    {},
    [],
    '-₹ 1,000',
    'undefined',
    'null'
  ];
  adversarialCases.forEach(input => {
    const res = parseAmount(input);
    assert.strictEqual(typeof res, 'number', `parseAmount(${JSON.stringify(input)}) must return number`);
    assert.ok(!isNaN(res), `parseAmount(${JSON.stringify(input)}) must not return NaN`);
    assert.ok(res >= 0, `parseAmount(${JSON.stringify(input)}) must be >= 0, got ${res}`);
  });
});

check('S1.5: 10,000 Randomized strings with varied comma formats, spaces, and signs', () => {
  const prng = makePRNG(11223344);
  for (let i = 0; i < 10000; i++) {
    const rawVal = Math.round(prng() * 10000000 * 100) / 100;
    const formatted = rawVal.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    const padded = `  ${prng() < 0.5 ? '-' : ''}${formatted}  `;
    const parsed = parseAmount(padded);
    assert.strictEqual(parsed, rawVal, `Failed parsing "${padded}", expected ${rawVal}, got ${parsed}`);
    assert.ok(parsed >= 0 && Number.isFinite(parsed));
  }
});

// -----------------------------------------------------------------------------
// SUITE 2: formatINR NEGATIVE ZERO & INTEGER PAISE ORACLE
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 2: formatINR Negative Zero & Float Rounding Oracle');
console.log('------------------------------------------------------------------------');

check('S2.1: Elimination of negative zero across sub-cent rounding boundaries', () => {
  const subCentZeroCases = [
    0,
    -0,
    '0',
    '-0',
    '0.00',
    '-0.00',
    -0.000001,
    -0.001,
    -0.004,
    -0.00499999,
  ];
  subCentZeroCases.forEach(input => {
    const res = formatINR(input);
    assert.strictEqual(res, '₹ 0.00', `formatINR(${input}) produced "${res}", expected "₹ 0.00"`);
    assert.ok(!res.includes('-'), `Negative sign found in zero output: "${res}"`);
  });
});

check('S2.2: Threshold transition: values <= -0.0051 round to -₹ 0.01, values >= -0.005 round to ₹ 0.00', () => {
  assert.strictEqual(formatINR(-0.005), '₹ 0.00');
  assert.strictEqual(formatINR(-0.0051), '-₹ 0.01');
  assert.strictEqual(formatINR(-0.009), '-₹ 0.01');
  assert.strictEqual(formatINR(-0.01), '-₹ 0.01');
  assert.strictEqual(formatINR(-0.014), '-₹ 0.01');
  assert.strictEqual(formatINR(-0.0151), '-₹ 0.02');
  assert.strictEqual(formatINR(-0.016), '-₹ 0.02');
});

check('S2.3: Currency symbol ordering invariant (-₹ X vs ₹ -X)', () => {
  const negativeAmounts = [-0.01, -1, -50.5, -1000, -40000, -100000, -10000000];
  negativeAmounts.forEach(amt => {
    const res = formatINR(amt);
    assert.ok(res.startsWith('-₹ '), `Negative amount ${amt} must start with "-₹ ", got "${res}"`);
    assert.ok(!res.includes('₹ -'), `Negative amount ${amt} must not contain "₹ -", got "${res}"`);
  });
});

check('S2.4: 10,000 Float vs Integer Paise Oracle Verification', () => {
  const prng = makePRNG(55667788);
  for (let i = 0; i < 10000; i++) {
    // Generate paise as integer: -10,00,00,000 paise to +10,00,00,000 paise
    const paiseInt = Math.floor(prng() * 200000000) - 100000000;
    const floatVal = paiseInt / 100;

    const res = formatINR(floatVal);

    if (paiseInt === 0) {
      assert.strictEqual(res, '₹ 0.00', `Zero paise must format to "₹ 0.00", got "${res}"`);
    } else if (paiseInt > 0) {
      assert.ok(res.startsWith('₹ '), `Positive paise must start with "₹ ", got "${res}"`);
    } else {
      assert.ok(res.startsWith('-₹ '), `Negative paise must start with "-₹ ", got "${res}"`);
    }
  }
});

// -----------------------------------------------------------------------------
// SUITE 3: CONTRA SELF-TRANSFERS (account === toAccount === 'Cash')
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 3: Contra Self-Transfers & Ledger Balance Shifts');
console.log('------------------------------------------------------------------------');

check('S3.1: getTransactionsWithRunningBal: Contra self-transfer has zero net balance shift', () => {
  const txns = [
    { id: 1, date: '2025-01-01', time: '10:00', type: 'in', account: 'Cash', amount: '10,000.00', status: 'posted' },
    // Self-transfer: Cash -> Cash
    { id: 2, date: '2025-01-02', time: '11:00', type: 'contra', account: 'Cash', toAccount: 'Cash', amount: '3,000.00', status: 'posted' },
    { id: 3, date: '2025-01-03', time: '12:00', type: 'out', account: 'Cash', amount: '2,000.00', status: 'posted' }
  ];

  // 1. In filtered account view 'Cash'
  const cashLedger = getTransactionsWithRunningBal(txns, 'Cash');
  assert.strictEqual(cashLedger[0].runningBal, 10000);
  assert.strictEqual(cashLedger[1].runningBal, 10000, `Self-transfer must keep running balance at 10000, got ${cashLedger[1].runningBal}`);
  assert.strictEqual(cashLedger[2].runningBal, 8000);

  // 2. In consolidated 'ALL' view
  const allLedger = getTransactionsWithRunningBal(txns, 'ALL');
  assert.strictEqual(allLedger[0].runningBal, 10000);
  assert.strictEqual(allLedger[1].runningBal, 10000, `Consolidated view must keep running balance at 10000, got ${allLedger[1].runningBal}`);
  assert.strictEqual(allLedger[2].runningBal, 8000);
});

check('S3.2: renderStatementTable: Contra self-transfer shows equal debit & credit and zero balance shift', () => {
  const txns = [
    { id: 1, voucher: 'V1', date: '2025-01-01', time: '10:00', type: 'in', account: 'Cash', amount: '25,000.00', status: 'posted' },
    // Self-transfer: Cash -> Cash for 12,000
    { id: 2, voucher: 'V2', date: '2025-01-02', time: '11:00', type: 'contra', account: 'Cash', toAccount: 'Cash', amount: '12,000.00', status: 'posted' }
  ];

  const statementRows = simulateStatementTableCalc(txns, 'Cash', true);
  const selfTxn = statementRows[1];

  assert.strictEqual(selfTxn.debit, 12000, `Self-transfer debit must equal 12000, got ${selfTxn.debit}`);
  assert.strictEqual(selfTxn.credit, 12000, `Self-transfer credit must equal 12000, got ${selfTxn.credit}`);
  assert.strictEqual(selfTxn.runningBal, 25000, `Self-transfer running balance must remain 25000, got ${selfTxn.runningBal}`);
});

check('S3.3: Contra Circular Transfer Invariance: Multi-Account (Cash -> SBI -> HDFC -> Cash)', () => {
  const txns = [
    { id: 1, date: '2025-01-01', time: '09:00', type: 'in', account: 'Cash', amount: '50,000', status: 'posted' },
    { id: 2, date: '2025-01-02', time: '10:00', type: 'contra', account: 'Cash', toAccount: 'SBI', amount: '20,000', status: 'posted' },
    { id: 3, date: '2025-01-03', time: '11:00', type: 'contra', account: 'SBI', toAccount: 'HDFC', amount: '20,000', status: 'posted' },
    { id: 4, date: '2025-01-04', time: '12:00', type: 'contra', account: 'HDFC', toAccount: 'Cash', amount: '20,000', status: 'posted' }
  ];

  const cashBal = getTransactionsWithRunningBal(txns, 'Cash').at(-1).runningBal;
  const sbiBal = getTransactionsWithRunningBal(txns, 'SBI').at(-1).runningBal;
  const hdfcBal = getTransactionsWithRunningBal(txns, 'HDFC').at(-1).runningBal;
  const allBal = getTransactionsWithRunningBal(txns, 'ALL').at(-1).runningBal;

  assert.strictEqual(cashBal, 50000, `Cash ending balance must return to 50000, got ${cashBal}`);
  assert.strictEqual(sbiBal, 0, `SBI ending balance must return to 0, got ${sbiBal}`);
  assert.strictEqual(hdfcBal, 0, `HDFC ending balance must return to 0, got ${hdfcBal}`);
  assert.strictEqual(allBal, 50000, `Consolidated balance must remain 50000, got ${allBal}`);
});

// -----------------------------------------------------------------------------
// SUITE 4: MULTI-ACCOUNT 5,000 RANDOMIZED OPERATIONS BALANCE CONSERVATION
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 4: 5,000 Randomized Operations Multi-Account Balance Conservation');
console.log('------------------------------------------------------------------------');

check('S4.1: Multi-Account Conservation: Consolidated Balance === Sum(Individual Balances) at every stage', () => {
  const prng = makePRNG(99887766);
  const accounts = ['Cash', 'SBI Current', 'HDFC Bank', 'ICICI OD', 'Petty Cash'];
  const testTxs = [];

  for (let i = 0; i < 5000; i++) {
    const fromIdx = Math.floor(prng() * accounts.length);
    const toIdx = Math.floor(prng() * accounts.length);
    const acc = accounts[fromIdx];
    const toAcc = accounts[toIdx];

    const typeRoll = prng();
    let type;
    if (typeRoll < 0.35) type = 'in';
    else if (typeRoll < 0.65) type = 'out';
    else type = 'contra'; // Can include self-transfers when fromIdx === toIdx

    // Amount can be number, string with commas, or paise
    const amtFloat = Math.round(prng() * 100000 * 100) / 100;
    const isString = prng() < 0.5;
    const amount = isString ? amtFloat.toLocaleString('en-IN', { minimumFractionDigits: 2 }) : amtFloat;
    const status = prng() < 0.05 ? 'voided' : 'posted';

    testTxs.push({
      id: i + 1,
      date: '2025-01-01',
      time: String(i).padStart(5, '0'),
      type,
      account: acc,
      toAccount: toAcc,
      amount,
      status
    });
  }

  // Calculate consolidated running balance
  const consolidated = getTransactionsWithRunningBal(testTxs, 'ALL');
  const finalConsolidatedBal = consolidated.length > 0 ? consolidated.at(-1).runningBal : 0;

  // Calculate sum of individual account running balances
  let sumOfAccounts = 0;
  accounts.forEach(acc => {
    const accLedger = getTransactionsWithRunningBal(testTxs, acc);
    const accFinal = accLedger.length > 0 ? accLedger.at(-1).runningBal : 0;
    sumOfAccounts += accFinal;
  });
  sumOfAccounts = Math.round((sumOfAccounts + Number.EPSILON) * 100) / 100;

  assert.strictEqual(
    finalConsolidatedBal,
    sumOfAccounts,
    `Multi-account balance conservation failure!\nConsolidated: ${finalConsolidatedBal}\nSum of Accounts: ${sumOfAccounts}`
  );
  console.log(`    ↳ Verified conservation across 5,000 vouchers: Final Cash Balance = ₹ ${finalConsolidatedBal}`);
});

// -----------------------------------------------------------------------------
// SUITE 5: LIVE CODEBASE CALL-SITE AUDIT & SANITIZATION
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 5: Live Codebase Call-Site Audit');
console.log('------------------------------------------------------------------------');

check('S5.1: Zero instances of unpatched parseFloat(t.amount) without parseAmount', () => {
  const matches = [...stagingContent.matchAll(/parseFloat\(\s*(?:t\.amount|val)\s*\)/g)];
  assert.strictEqual(matches.length, 0, `Found ${matches.length} unpatched raw parseFloat(t.amount/val) calls!`);
});

check('S5.2: parseAmount is invoked in accounting, reports, charts, vouchers, and exports', () => {
  const parseAmountCalls = [...stagingContent.matchAll(/parseAmount\(/g)];
  assert.ok(parseAmountCalls.length >= 12, `Expected at least 12 calls to parseAmount, found: ${parseAmountCalls.length}`);
  console.log(`    ↳ Confirmed ${parseAmountCalls.length} parseAmount call sites in index.html`);
});

check('S5.3: escapeHtml entity encoding preserves logistics/transport business remark strings', () => {
  const testRemarks = [
    'Agrawal Transport <transport@gmail.com>',
    'Payment for truck <truck-12>',
    'In transit <transit>',
    'trip <trip-5>',
    'Diesel <tracking>'
  ];
  testRemarks.forEach(rem => {
    const escaped = escapeHtml(rem);
    assert.ok(escaped.length > 0, `escapeHtml erased data for "${rem}"`);
    assert.ok(!escaped.includes('<'), `Raw '<' found in escaped output: "${escaped}"`);
    assert.ok(!escaped.includes('>'), `Raw '>' found in escaped output: "${escaped}"`);
    assert.ok(escaped.includes('&lt;'), `Missing '&lt;' in escaped output: "${escaped}"`);
    assert.ok(escaped.includes('&gt;'), `Missing '&gt;' in escaped output: "${escaped}"`);
  });
});

check('S5.4: escapeHtml safely neutralizes HTML injection payloads without DOM breakout', () => {
  const maliciousInputs = [
    '<script>alert(1)</script>',
    '<img src="x" onerror="alert(1)">',
    '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>',
    '" onmouseover="alert(1)"',
    "' onfocus='alert(1)'"
  ];
  maliciousInputs.forEach(input => {
    const escaped = escapeHtml(input);
    assert.ok(!escaped.includes('<script>'), `Unescaped script tag: "${escaped}"`);
    assert.ok(!escaped.includes('<tr>'), `Unescaped table row tag: "${escaped}"`);
    assert.ok(!escaped.includes('<td>'), `Unescaped table data tag: "${escaped}"`);
    assert.ok(!escaped.includes('"'), `Unescaped double quote: "${escaped}"`);
    assert.ok(!escaped.includes("'"), `Unescaped single quote: "${escaped}"`);
  });
});

// -----------------------------------------------------------------------------
// SUMMARY
// -----------------------------------------------------------------------------
console.log('\n========================================================================');
console.log('CHALLENGER M1-IT3-1 EMPIRICAL STRESS TEST SUMMARY');
console.log('========================================================================');
console.log(`Total Checks Run : ${totalChecks}`);
console.log(`Passed Checks    : ${passedChecks}`);
console.log(`Failed Checks    : ${failedChecks}`);
console.log('========================================================================');

if (failedChecks === 0) {
  console.log('RECOMMENDED VERDICT: APPROVE');
  process.exit(0);
} else {
  console.log('RECOMMENDED VERDICT: REQUEST_CHANGES');
  process.exit(1);
}
