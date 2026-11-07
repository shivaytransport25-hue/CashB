/**
 * empirical_stress_test_math.mjs
 * Challenger M1-It2-1 Specialized Empirical Stress Harness
 * 
 * Target: Cash Book Pro Milestone M1 Iteration 2 (Desktop Accounting Math & Formatting)
 * Files Under Test:
 *  - E:\CashBookRelease\app\www\index.html (Staging)
 *  - C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html (Installed)
 */

import fs from 'node:fs';
import crypto from 'node:crypto';
import assert from 'node:assert';

const STAGING_PATH = 'E:/CashBookRelease/app/www/index.html';
const INSTALLED_PATH = 'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html';

console.log('========================================================================');
console.log('CHALLENGER M1-IT2-1: EMPIRICAL ACCOUNTING MATH & FORMATTING STRESS HARNESS');
console.log('========================================================================\n');

let totalAssertions = 0;
let passedAssertions = 0;
let failedAssertions = 0;
const findings = [];

function check(title, fn) {
  totalAssertions++;
  try {
    fn();
    passedAssertions++;
    console.log(`  ✔ PASS: ${title}`);
  } catch (err) {
    failedAssertions++;
    console.log(`  ✖ FAIL: ${title}`);
    console.log(`    ↳ Details: ${err.message}`);
    findings.push({ title, error: err.message, stack: err.stack });
  }
}

// -----------------------------------------------------------------------------
// HELPER: Deterministic Pseudo-Random Number Generator (PRNG) for reproducibility
// -----------------------------------------------------------------------------
function makePRNG(seed = 123456789) {
  let s = seed;
  return function() {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

// -----------------------------------------------------------------------------
// PRE-FLIGHT: FILE & PARITY CHECKS
// -----------------------------------------------------------------------------
console.log('------------------------------------------------------------------------');
console.log('PRE-FLIGHT: Target Accessibility & SHA-256 Parity');
console.log('------------------------------------------------------------------------');

const stagingContent = fs.readFileSync(STAGING_PATH, 'utf8');
const installedContent = fs.readFileSync(INSTALLED_PATH, 'utf8');

check('P1: Staging and Installed distribution files exist and are non-empty', () => {
  assert.ok(stagingContent.length > 50000, `Staging file too small: ${stagingContent.length} bytes`);
  assert.ok(installedContent.length > 50000, `Installed file too small: ${installedContent.length} bytes`);
});

check('P2: Staging and Installed index.html have 100% SHA-256 identity parity', () => {
  const hashStaging = crypto.createHash('sha256').update(stagingContent).digest('hex');
  const hashInstalled = crypto.createHash('sha256').update(installedContent).digest('hex');
  assert.strictEqual(hashStaging, hashInstalled, `Hash mismatch:\nStaging:   ${hashStaging}\nInstalled: ${hashInstalled}`);
  console.log(`    ↳ SHA-256: ${hashStaging}`);
});

// Function extractors
function extractFunction(source, fnName) {
  const regex = new RegExp(`function ${fnName}\\([\\s\\S]*?\\n    \\}`);
  const match = source.match(regex);
  if (!match) {
    throw new Error(`Could not extract function ${fnName} from source`);
  }
  return new Function(`
    ${match[0]}
    return ${fnName};
  `)();
}

const formatINR = extractFunction(stagingContent, 'formatINR');
const getTransactionsWithRunningBal = extractFunction(stagingContent, 'getTransactionsWithRunningBal');
const formatINR_installed = extractFunction(installedContent, 'formatINR');
const getTransactionsWithRunningBal_installed = extractFunction(installedContent, 'getTransactionsWithRunningBal');

// -----------------------------------------------------------------------------
// SUITE 1: formatINR DEEP ADVERSARIAL STRESS TESTING
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 1: formatINR Deep Adversarial Stress Testing');
console.log('------------------------------------------------------------------------');

check('S1.1: Zero and negative zero formatting invariants', () => {
  const zeroValues = [0, -0, '0', '-0', '0.00', '-0.00', 0.0000];
  zeroValues.forEach(zv => {
    const res = formatINR(zv);
    assert.strictEqual(res, '₹ 0.00', `formatINR(${JSON.stringify(zv)}) expected "₹ 0.00", got "${res}"`);
    const resInst = formatINR_installed(zv);
    assert.strictEqual(resInst, '₹ 0.00', `Installed formatINR(${JSON.stringify(zv)}) expected "₹ 0.00", got "${resInst}"`);
  });
});

check('S1.2: Standard positive integers and 2-decimal paise formatting', () => {
  const cases = [
    [1, '₹ 1.00'],
    [10, '₹ 10.00'],
    [100, '₹ 100.00'],
    [1000, '₹ 1,000.00'],
    [10000, '₹ 10,000.00'],
    [100000, '₹ 1,00,000.00'], // 1 Lakh
    [1000000, '₹ 10,00,000.00'], // 10 Lakhs
    [10000000, '₹ 1,00,00,000.00'], // 1 Crore
    [100000000, '₹ 10,00,00,000.00'], // 10 Crores
    [1234567.89, '₹ 12,34,567.89'],
    [0.05, '₹ 0.05'],
    [0.99, '₹ 0.99']
  ];
  cases.forEach(([val, expected]) => {
    const res = formatINR(val);
    assert.strictEqual(res, expected, `formatINR(${val}) expected "${expected}", got "${res}"`);
  });
});

check('S1.3: Negative currency formatting: Minus sign strictly precedes Rupee symbol (-₹ X vs ₹ -X)', () => {
  const negCases = [
    [-0.01, '-₹ 0.01'],
    [-1, '-₹ 1.00'],
    [-50, '-₹ 50.00'],
    [-1234.56, '-₹ 1,234.56'],
    [-40000, '-₹ 40,000.00'],
    [-100000, '-₹ 1,00,000.00'],
    [-10000000, '-₹ 1,00,00,000.00'],
  ];
  negCases.forEach(([val, expected]) => {
    const res = formatINR(val);
    assert.strictEqual(res.startsWith('-₹ '), true, `Negative amount ${val} must start with "-₹ ", got "${res}"`);
    assert.strictEqual(res.includes('₹ -'), false, `Negative amount ${val} must NOT contain "₹ -", got "${res}"`);
    assert.strictEqual(res, expected, `formatINR(${val}) expected "${expected}", got "${res}"`);
  });
});

check('S1.4: Comma-containing and string formatted inputs without truncation', () => {
  const strCases = [
    ['1,000.00', '₹ 1,000.00'],
    ['1,50,000.00', '₹ 1,50,000.00'],
    ['10,00,000.50', '₹ 10,00,000.50'],
    ['-40,000.00', '-₹ 40,000.00'],
    ['-1,25,000.75', '-₹ 1,25,000.75'],
    ['25000', '₹ 25,000.00'],
    [' 5000.50 ', '₹ 5,000.50'],
  ];
  strCases.forEach(([str, expected]) => {
    const res = formatINR(str);
    assert.strictEqual(res, expected, `formatINR("${str}") expected "${expected}", got "${res}"`);
  });
});

check('S1.5: Adversarial, null, undefined, empty, and invalid inputs resilience', () => {
  const invalidCases = [null, undefined, '', '   ', 'invalid', 'abc', NaN];
  invalidCases.forEach(inv => {
    const res = formatINR(inv);
    assert.strictEqual(res, '₹ 0.00', `formatINR(${inv}) expected "₹ 0.00", got "${res}"`);
  });
});

check('S1.6: 10,000 Randomized formatINR calls (Regex, Sign, and Fraction Invariants)', () => {
  const prng = makePRNG(987654321);
  const regexINR = /^(-?₹\s)(0|[1-9]\d{0,1}(,\d{2})*|\d+)(,\d{3})?(\.\d{2})$/;
  const generalRegex = /^-?₹\s[\d,]+\.\d{2}$/;

  for (let i = 0; i < 10000; i++) {
    // Generate numbers from -10 Crores to +10 Crores
    const sign = prng() < 0.5 ? -1 : 1;
    const magnitude = prng() * 100000000;
    const rounded = Math.round(magnitude * 100) / 100;
    const val = sign * rounded;

    const formatted = formatINR(val);
    assert.ok(generalRegex.test(formatted), `formatINR(${val}) output "${formatted}" failed general regex`);

    if (val < 0) {
      assert.ok(formatted.startsWith('-₹ '), `Negative ${val} must start with "-₹ ", got: "${formatted}"`);
    } else {
      assert.ok(formatted.startsWith('₹ '), `Positive ${val} must start with "₹ ", got: "${formatted}"`);
    }
  }
});

// -----------------------------------------------------------------------------
// SUITE 2: 10,000 RANDOMIZED TRANSACTIONS STRESS TEST (getTransactionsWithRunningBal)
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 2: 10,000 Randomized Operations Stress Test');
console.log('------------------------------------------------------------------------');

check('S2.1: 10,000 Randomized Operations — 0 Paise IEEE-754 Drift Oracle Test', () => {
  const prng = makePRNG(42424242);
  const accounts = ['Cash In Hand', 'HDFC Current', 'SBI Overdraft', 'ICICI Bank', 'Petty Cash'];
  const testTxs = [];

  const startDate = new Date('2025-01-01T00:00:00Z').getTime();

  for (let i = 0; i < 10000; i++) {
    const accIdx = Math.floor(prng() * accounts.length);
    let toAccIdx = Math.floor(prng() * accounts.length);
    const acc = accounts[accIdx];
    const toAcc = accounts[toAccIdx];

    const typeRoll = prng();
    let type;
    if (typeRoll < 0.40) type = 'in';
    else if (typeRoll < 0.70) type = 'out';
    else type = 'contra';

    // Variety of amounts: small paise, medium, large crores, comma-strings
    const amtRoll = prng();
    let amt;
    if (amtRoll < 0.20) {
      // 0.10, 0.20, 0.30, 0.01 mantissa drift triggers
      const commonDrifts = [0.01, 0.10, 0.20, 0.30, 0.07, 0.14, 0.70];
      amt = commonDrifts[Math.floor(prng() * commonDrifts.length)];
    } else if (amtRoll < 0.50) {
      // General 2-decimal floats
      amt = Math.round(prng() * 100000 * 100) / 100;
    } else if (amtRoll < 0.80) {
      // Formatted strings with commas
      const raw = Math.round(prng() * 500000 * 100) / 100;
      amt = raw.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    } else {
      // Large amounts (Crores)
      amt = Math.round((1000000 + prng() * 50000000) * 100) / 100;
    }

    // Voided distribution (~5%)
    const isVoided = prng() < 0.05;

    // Random date timestamp
    const dateOffset = Math.floor(prng() * 365 * 2) * 86400000;
    const d = new Date(startDate + dateOffset);
    const dateStr = d.toISOString().split('T')[0];
    const hour = String(Math.floor(prng() * 24)).padStart(2, '0');
    const min = String(Math.floor(prng() * 60)).padStart(2, '0');

    testTxs.push({
      id: `tx_${i}`,
      date: dateStr,
      time: `${hour}:${min}`,
      amount: amt,
      type: type,
      account: acc,
      toAccount: type === 'contra' ? toAcc : undefined,
      status: isVoided ? 'voided' : 'active'
    });
  }

  // Exact BigInt Paise Oracle Simulation for consolidated 'ALL' view
  // First, sort as getTransactionsWithRunningBal does:
  const sortedTxs = [...testTxs].sort((a, b) => (a.date + (a.time || '')).localeCompare(b.date + (b.time || '')));

  let oraclePaiseAll = 0n;
  const oracleRunningBalancesAll = [];

  for (const t of sortedTxs) {
    if (t.status !== 'voided') {
      const cleanAmt = typeof t.amount === 'string' ? t.amount.replace(/,/g, '') : t.amount;
      const parsed = Math.abs(parseFloat(cleanAmt) || 0);
      const amtPaise = BigInt(Math.round(parsed * 100));

      if (t.type === 'in') {
        oraclePaiseAll += amtPaise;
      } else if (t.type === 'out') {
        oraclePaiseAll -= amtPaise;
      }
      // contra has net 0 impact on consolidated view
    }
    oracleRunningBalancesAll.push(Number(oraclePaiseAll) / 100);
  }

  // Execute getTransactionsWithRunningBal on staging
  const resultAll = getTransactionsWithRunningBal(testTxs, 'ALL');
  assert.strictEqual(resultAll.length, 10000, 'Consolidated result must contain exactly 10,000 transactions');

  // Verify every single step against the oracle
  let maxDrift = 0;
  for (let i = 0; i < resultAll.length; i++) {
    const act = resultAll[i].runningBal;
    const exp = oracleRunningBalancesAll[i];
    assert.strictEqual(typeof act, 'number', `runningBal at #${i} must be number`);
    assert.strictEqual(Number.isNaN(act), false, `runningBal at #${i} is NaN`);
    assert.strictEqual(Number.isFinite(act), true, `runningBal at #${i} is not finite`);

    const drift = Math.abs(act - exp);
    if (drift > maxDrift) maxDrift = drift;
    assert.strictEqual(
      act,
      exp,
      `Drift detected at txn #${i}: act=${act}, exp=${exp}, diff=${drift}`
    );
  }
  console.log(`    ↳ Verified 10,000 transactions against BigInt Oracle. Max drift: ${maxDrift}`);
});

check('S2.2: Multi-Account Balance Conservation across 10,000 operations', () => {
  // Generate 10,000 transactions and verify:
  // Consolidated balance == Sum of individual account balances
  const prng = makePRNG(11223344);
  const accounts = ['Cash In Hand', 'HDFC Current', 'SBI Overdraft', 'ICICI Bank', 'Petty Cash'];
  const testTxs = [];

  for (let i = 0; i < 10000; i++) {
    const acc = accounts[Math.floor(prng() * accounts.length)];
    const toAcc = accounts[Math.floor(prng() * accounts.length)];
    const typeRoll = prng();
    const type = typeRoll < 0.35 ? 'in' : (typeRoll < 0.65 ? 'out' : 'contra');
    const amt = Math.round((10 + prng() * 50000) * 100) / 100;
    const isVoid = prng() < 0.08;

    testTxs.push({
      id: `tx_${i}`,
      date: `2026-11-${String((i % 28) + 1).padStart(2, '0')}`,
      time: `${String(i % 24).padStart(2, '0')}:${String(i % 60).padStart(2, '0')}`,
      amount: amt,
      type: type,
      account: acc,
      toAccount: type === 'contra' ? toAcc : undefined,
      status: isVoid ? 'voided' : 'active'
    });
  }

  const consolidatedRes = getTransactionsWithRunningBal(testTxs, 'ALL');
  const consolidatedClosing = consolidatedRes[consolidatedRes.length - 1].runningBal;

  let sumOfAccountBalances = 0;
  for (const acc of accounts) {
    const accRes = getTransactionsWithRunningBal(testTxs, acc);
    const accClosing = accRes.length > 0 ? accRes[accRes.length - 1].runningBal : 0;
    sumOfAccountBalances += accClosing;
  }
  sumOfAccountBalances = Math.round(sumOfAccountBalances * 100) / 100;

  const diffPaise = Math.abs(Math.round(consolidatedClosing * 100) - Math.round(sumOfAccountBalances * 100));
  assert.strictEqual(
    diffPaise,
    0,
    `Conservation violated! Consolidated: ${consolidatedClosing}, Sum of accounts: ${sumOfAccountBalances}`
  );
  console.log(`    ↳ Consolidated Closing (${consolidatedClosing}) === Sum of Accounts (${sumOfAccountBalances}). Conservation verified.`);
});

check('S2.3: 1,000 Consecutive Contra Self-Transfers (Cash -> Cash) net zero impact', () => {
  const selfTxs = [];
  for (let i = 0; i < 1000; i++) {
    selfTxs.push({
      date: '2026-11-01',
      time: `10:${String(i % 60).padStart(2, '0')}`,
      amount: 5000 + i,
      type: 'contra',
      account: 'Cash In Hand',
      toAccount: 'Cash In Hand',
      status: 'active'
    });
  }
  const res = getTransactionsWithRunningBal(selfTxs, 'Cash In Hand');
  for (let i = 0; i < res.length; i++) {
    assert.strictEqual(res[i].runningBal, 0, `Self-transfer at #${i} must have runningBal 0, got ${res[i].runningBal}`);
  }
});

check('S2.4: Out-of-order input timestamp shuffling invariant', () => {
  const prng = makePRNG(998877);
  const baseTxs = [];
  for (let i = 0; i < 500; i++) {
    baseTxs.push({
      id: `tx_${i}`,
      date: `2026-11-${String((i % 25) + 1).padStart(2, '0')}`,
      time: `${String(i % 24).padStart(2, '0')}:${String((i * 7) % 60).padStart(2, '0')}`,
      amount: 100,
      type: 'in',
      account: 'Cash'
    });
  }

  // Pre-sorted calculation
  const normalRes = getTransactionsWithRunningBal(baseTxs, 'Cash');

  // Fisher-Yates shuffle
  const shuffled = [...baseTxs];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(prng() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }

  const shuffledRes = getTransactionsWithRunningBal(shuffled, 'Cash');
  assert.strictEqual(shuffledRes.length, normalRes.length);

  // Assert identical order and running balances
  for (let i = 0; i < normalRes.length; i++) {
    assert.strictEqual(shuffledRes[i].date, normalRes[i].date);
    assert.strictEqual(shuffledRes[i].time, normalRes[i].time);
    assert.strictEqual(shuffledRes[i].runningBal, normalRes[i].runningBal);
  }
});

check('S2.5: Massive Voided Transaction Isolation (₹1,000 Crores)', () => {
  const txs = [
    { date: '2026-11-01', amount: 5000, type: 'in', account: 'Cash' },
    { date: '2026-11-02', amount: 10000000000, type: 'in', account: 'Cash', status: 'voided' },
    { date: '2026-11-03', amount: 10000000000, type: 'out', account: 'Cash', status: 'voided' },
    { date: '2026-11-04', amount: 10000000000, type: 'contra', account: 'Cash', toAccount: 'SBI', status: 'voided' },
    { date: '2026-11-05', amount: 2000, type: 'out', account: 'Cash' },
  ];
  const res = getTransactionsWithRunningBal(txs, 'Cash');
  assert.strictEqual(res[0].runningBal, 5000);
  assert.strictEqual(res[1].runningBal, 5000, 'Voided ₹1000 Cr in must not alter balance');
  assert.strictEqual(res[2].runningBal, 5000, 'Voided ₹1000 Cr out must not alter balance');
  assert.strictEqual(res[3].runningBal, 5000, 'Voided ₹1000 Cr contra must not alter balance');
  assert.strictEqual(res[4].runningBal, 3000, 'Final running balance after ₹2000 outflow must be 3000');
});

check('S2.6: Negative amount input sanitization in getTransactionsWithRunningBal', () => {
  // If amount is passed as -500 (e.g. from buggy user input or external import), Math.abs should sanitize
  const negAmtTxs = [
    { date: '2026-11-01', amount: -500, type: 'in', account: 'Cash' },
    { date: '2026-11-02', amount: '-200.50', type: 'out', account: 'Cash' },
  ];
  const res = getTransactionsWithRunningBal(negAmtTxs, 'Cash');
  assert.strictEqual(res[0].runningBal, 500, 'Negative inflow must be treated as +500');
  assert.strictEqual(res[1].runningBal, 299.50, 'Negative outflow must be treated as -200.50');
});

// -----------------------------------------------------------------------------
// SUITE 3: INSTALLED TARGET EQUIVALENCE & CROSS-TARGET FIDELITY
// -----------------------------------------------------------------------------
console.log('\n------------------------------------------------------------------------');
console.log('SUITE 3: Installed Target Equivalence & Cross-Target Fidelity');
console.log('------------------------------------------------------------------------');

check('S3.1: Staging vs Installed execution parity across 1,000 transactions', () => {
  const prng = makePRNG(556677);
  const sampleTxs = [];
  for (let i = 0; i < 1000; i++) {
    sampleTxs.push({
      date: `2026-11-${String((i % 25) + 1).padStart(2, '0')}`,
      time: `${String(i % 24).padStart(2, '0')}:${String(i % 60).padStart(2, '0')}`,
      amount: Math.round(prng() * 100000 * 100) / 100,
      type: i % 3 === 0 ? 'in' : (i % 3 === 1 ? 'out' : 'contra'),
      account: 'Cash',
      toAccount: 'Bank'
    });
  }

  const resStaging = getTransactionsWithRunningBal(sampleTxs, 'Cash');
  const resInstalled = getTransactionsWithRunningBal_installed(sampleTxs, 'Cash');

  assert.strictEqual(resStaging.length, resInstalled.length);
  for (let i = 0; i < resStaging.length; i++) {
    assert.strictEqual(
      resStaging[i].runningBal,
      resInstalled[i].runningBal,
      `Mismatch at #${i}: Staging=${resStaging[i].runningBal}, Installed=${resInstalled[i].runningBal}`
    );
  }
});

// -----------------------------------------------------------------------------
// SUMMARY & VERDICT
// -----------------------------------------------------------------------------
console.log('\n========================================================================');
console.log('CHALLENGER M1-IT2-1 EMPIRICAL STRESS TEST EXECUTION SUMMARY');
console.log('========================================================================');
console.log(`Total Assertions Run: ${totalAssertions}`);
console.log(`Passed: ${passedAssertions}`);
console.log(`Failed: ${failedAssertions}`);

if (failedAssertions > 0) {
  console.log('\nCONFIRMED DEFECTS / REGRESSIONS:');
  findings.forEach((f, idx) => {
    console.log(`\n[DEFECT #${idx + 1}] ${f.title}`);
    console.log(`Error: ${f.error}`);
  });
  console.log('\nRECOMMENDED VERDICT: REQUEST_CHANGES');
} else {
  console.log('\nALL STRESS TESTS PASSED WITH ZERO DRIFT AND ZERO REGRESSIONS.');
  console.log('RECOMMENDED VERDICT: APPROVE');
}

console.log('========================================================================\n');
process.exit(failedAssertions > 0 ? 1 : 0);
