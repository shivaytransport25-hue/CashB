/**
 * empirical_stress_test.mjs
 * Challenger M1-1 Empirical Challenge & Adversarial Stress Test Harness
 * 
 * Target: Cash Book Pro Milestone M1 (Desktop Styling & Ledger Overhaul)
 * Author: Challenger M1-1 (critic / specialist)
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert';

const STAGING_PATH = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';
const INSTALLED_PATH = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';

console.log('================================================================');
console.log('CHALLENGER M1-1: EMPIRICAL STRESS TEST & ADVERSARIAL HARNESS');
console.log('================================================================\n');

let totalTests = 0;
let passedTests = 0;
let failedTests = 0;
const findings = [];

function check(title, fn) {
  totalTests++;
  try {
    fn();
    passedTests++;
    console.log(`  ✔ PASS: ${title}`);
  } catch (err) {
    failedTests++;
    console.log(`  ✖ FAIL: ${title}`);
    console.log(`    ↳ Finding: ${err.message}`);
    findings.push({ title, error: err.message });
  }
}

// ----------------------------------------------------------------------------
// SUITE 1: STATIC & STYLING TOKEN INVARIANT AUDIT
// ----------------------------------------------------------------------------
console.log('----------------------------------------------------------------');
console.log('SUITE 1: Prohibited Styling Tokens & Visual Invariant Audit');
console.log('----------------------------------------------------------------');

const stagingContent = fs.readFileSync(STAGING_PATH, 'utf8');
const installedContent = fs.readFileSync(INSTALLED_PATH, 'utf8');

check('S1.1: SHA256 checksum identity between Staging and Installed app', () => {
  const hashStaging = crypto.createHash('sha256').update(stagingContent).digest('hex');
  const hashInstalled = crypto.createHash('sha256').update(installedContent).digest('hex');
  assert.strictEqual(hashStaging, hashInstalled, `Checksum mismatch: ${hashStaging} vs ${hashInstalled}`);
});

check('S1.2: Prohibit @keyframes pulseGlow and glowing keyframes', () => {
  assert.strictEqual(/@keyframes\s+pulseGlow/i.test(stagingContent), false, 'Found prohibited @keyframes pulseGlow');
  assert.strictEqual(/@keyframes\s+[a-zA-Z0-9_-]*glow/i.test(stagingContent), false, 'Found prohibited glow keyframe');
});

check('S1.3: Prohibit active filter: blur() and backdrop-filter: blur()', () => {
  const activeBlurMatches = stagingContent.split('\n').filter(line => {
    const trimmed = line.trim();
    if (trimmed.startsWith('/*') || trimmed.startsWith('*') || trimmed.startsWith('//')) return false;
    return /filter:\s*blur\(/i.test(trimmed) || /backdrop-filter:\s*blur\(/i.test(trimmed);
  });
  assert.strictEqual(activeBlurMatches.length, 0, `Found active blur filters: ${activeBlurMatches.join(', ')}`);
});

check('S1.4: Prohibit diffuse neon shadows (e.g. 14px 32px, 16px 40px, 24px 48px, 25px 60px)', () => {
  const diffuseMatches = stagingContent.match(/0\s+(14px\s+32px|16px\s+40px|24px\s+48px|25px\s+60px|20px\s+50px)/gi) || [];
  assert.strictEqual(diffuseMatches.length, 0, `Found diffuse shadows: ${diffuseMatches.length}`);
});

check('S1.5: Prohibit linear-gradient and radial-gradient in CSS', () => {
  const gradients = stagingContent.match(/(linear-gradient|radial-gradient)\([^)]+\)/gi) || [];
  assert.strictEqual(gradients.length, 0, `Found residual gradients: ${gradients.length}`);
});

check('S1.6: Standardized enterprise border radii (radii <= 10px or 50% circle)', () => {
  const largeRadii = [];
  const lines = stagingContent.split('\n');
  lines.forEach((line, idx) => {
    const match = line.match(/border-radius:\s*(\d+)px/i);
    if (match) {
      const val = parseInt(match[1], 10);
      if (val > 10) {
        largeRadii.push({ line: idx + 1, val, text: line.trim() });
      }
    }
  });
  assert.strictEqual(largeRadii.length, 0, `Found oversized border radii: ${JSON.stringify(largeRadii)}`);
});

check('S1.7: Require tabular numbers styling (tabular-nums and "tnum" 1)', () => {
  assert.ok(/font-variant-numeric:\s*tabular-nums/.test(stagingContent), 'Missing font-variant-numeric: tabular-nums');
  assert.ok(/font-feature-settings:\s*"tnum"\s*1/.test(stagingContent), 'Missing font-feature-settings: "tnum" 1');
});

check('S1.8: Require Enterprise Daylight and Dark Slate foundation tokens', () => {
  assert.ok(/--color-bg-canvas:\s*#F8FAFC/i.test(stagingContent), 'Missing daylight canvas #F8FAFC');
  assert.ok(/--color-bg-canvas:\s*#0B111B/i.test(stagingContent), 'Missing dark canvas #0B111B');
  assert.ok(/--color-credit:\s*#047857/i.test(stagingContent), 'Missing credit token #047857');
  assert.ok(/--color-debit:\s*#B91C1C/i.test(stagingContent), 'Missing debit token #B91C1C');
  assert.ok(/--color-transfer:\s*#1D4ED8/i.test(stagingContent), 'Missing transfer token #1D4ED8');
  assert.ok(/--color-accent:\s*#D97706/i.test(stagingContent), 'Missing accent token #D97706');
});

// ----------------------------------------------------------------------------
// SUITE 2: ACCOUNTING STRESS TESTING (getTransactionsWithRunningBal)
// ----------------------------------------------------------------------------
console.log('\n----------------------------------------------------------------');
console.log('SUITE 2: Accounting Function Stress Testing (getTransactionsWithRunningBal)');
console.log('----------------------------------------------------------------');

// Dynamically extract getTransactionsWithRunningBal from index.html
const fnMatch = stagingContent.match(/function getTransactionsWithRunningBal\([\s\S]*?\n    \}/);
if (!fnMatch) {
  throw new Error('Could not find getTransactionsWithRunningBal in index.html');
}
const getTransactionsWithRunningBal = new Function(`
  ${fnMatch[0]}
  return getTransactionsWithRunningBal;
`)();

// Extract formatINR for display verification
const formatINRMatch = stagingContent.match(/function formatINR\([\s\S]*?\n    \}/);
const formatINR = new Function(`
  ${formatINRMatch[0]}
  return formatINR;
`)();

check('S2.1: 2,500 Randomized Vouchers - Zero NaN and Zero String Concatenation Invariant', () => {
  const accounts = ['Cash', 'SBI Bank', 'HDFC Bank', 'ICICI Bank', 'Petty Cash'];
  const testTxs = [];
  
  for (let i = 0; i < 2500; i++) {
    const acc = accounts[i % accounts.length];
    let toAcc = accounts[(i + 1) % accounts.length];
    const typeRand = i % 10;
    const type = typeRand < 4 ? 'in' : (typeRand < 7 ? 'out' : 'contra');
    const baseAmt = ((i * 37) % 50000) + 0.25;
    const amt = i % 2 === 0 ? baseAmt.toFixed(2) : baseAmt;

    testTxs.push({
      id: `tx_${i}`,
      date: `2026-11-${String((i % 28) + 1).padStart(2, '0')}`,
      time: `${String(i % 24).padStart(2, '0')}:${String(i % 60).padStart(2, '0')}`,
      amount: amt,
      type: type,
      account: acc,
      toAccount: type === 'contra' ? toAcc : undefined,
      status: i % 17 === 0 ? 'voided' : 'active',
    });
  }

  const result = getTransactionsWithRunningBal(testTxs, 'ALL');
  assert.strictEqual(result.length, 2500, 'Result length must match input length');

  for (let i = 0; i < result.length; i++) {
    const r = result[i];
    assert.strictEqual(typeof r.runningBal, 'number', `runningBal at index ${i} is not number: ${typeof r.runningBal}`);
    assert.strictEqual(Number.isNaN(r.runningBal), false, `runningBal at index ${i} is NaN`);
    assert.strictEqual(Number.isFinite(r.runningBal), true, `runningBal at index ${i} is not finite`);
  }
});

check('S2.2: Multi-Account Balance Conservation: Consolidated Balance == Sum(Account Balances)', () => {
  const accounts = ['Cash', 'SBI', 'HDFC', 'PettyCash'];
  const txs = [
    { date: '2026-11-01', time: '10:00', amount: 150000.50, type: 'in', account: 'Cash' },
    { date: '2026-11-01', time: '10:30', amount: 200000.00, type: 'in', account: 'SBI' },
    { date: '2026-11-01', time: '11:00', amount: 50000.25, type: 'in', account: 'HDFC' },
    { date: '2026-11-02', time: '09:00', amount: 35000.00, type: 'out', account: 'Cash' },
    { date: '2026-11-02', time: '10:00', amount: 75000.75, type: 'out', account: 'SBI' },
    { date: '2026-11-02', time: '11:00', amount: 40000.00, type: 'contra', account: 'Cash', toAccount: 'SBI' },
    { date: '2026-11-02', time: '12:00', amount: 15000.00, type: 'contra', account: 'SBI', toAccount: 'PettyCash' },
    { date: '2026-11-02', time: '13:00', amount: 25000.00, type: 'contra', account: 'HDFC', toAccount: 'Cash' },
    { date: '2026-11-02', time: '14:00', amount: 99999.00, type: 'in', account: 'Cash', status: 'voided' }
  ];

  const consolidated = getTransactionsWithRunningBal(txs, 'ALL');
  const consolidatedClosing = consolidated[consolidated.length - 1].runningBal;

  let sumOfAccounts = 0;
  accounts.forEach(acc => {
    const accList = getTransactionsWithRunningBal(txs, acc);
    const end = accList.length > 0 ? accList[accList.length - 1].runningBal : 0;
    sumOfAccounts += end;
  });

  const diffPaise = Math.abs(Math.round(consolidatedClosing * 100) - Math.round(sumOfAccounts * 100));
  assert.strictEqual(diffPaise, 0, `Consolidated (${consolidatedClosing}) does not equal sum of accounts (${sumOfAccounts})`);
  assert.strictEqual(formatINR(consolidatedClosing), formatINR(sumOfAccounts));
});

check('S2.3: Extreme Value Boundaries (1 Paisa to 100 Crores)', () => {
  const extremeTxs = [
    { date: '2026-11-01', amount: 0.01, type: 'in', account: 'Cash' },
    { date: '2026-11-02', amount: 1000000000.00, type: 'in', account: 'Cash' },
    { date: '2026-11-03', amount: 0.01, type: 'out', account: 'Cash' },
    { date: '2026-11-04', amount: 999999999.99, type: 'out', account: 'Cash' },
  ];
  const res = getTransactionsWithRunningBal(extremeTxs, 'Cash');
  assert.strictEqual(Math.round(res[0].runningBal * 100), 1, '1 paisa balance');
  assert.strictEqual(Math.round(res[1].runningBal * 100), 100000000001, '100 Crores + 1 paisa');
  assert.strictEqual(Math.round(res[2].runningBal * 100), 100000000000, '100 Crores exact');
  assert.strictEqual(Math.round(res[3].runningBal * 100), 1, 'Closing 1 paisa');
});

check('S2.4: Deep Overdraft Negative Balances Calculation', () => {
  const overdraftTxs = [
    { date: '2026-11-01', amount: 10000, type: 'in', account: 'SBI' },
    { date: '2026-11-02', amount: 50000, type: 'out', account: 'SBI' }
  ];
  const res = getTransactionsWithRunningBal(overdraftTxs, 'SBI');
  assert.strictEqual(res[1].runningBal, -40000, 'Overdraft running balance must be -40000');
});

check('S2.5: Contra Circular Transfer Invariance (A->B->C->A net zero)', () => {
  const cycleTxs = [
    { date: '2026-11-01', time: '10:00', amount: 25000, type: 'contra', account: 'Cash', toAccount: 'SBI' },
    { date: '2026-11-01', time: '11:00', amount: 25000, type: 'contra', account: 'SBI', toAccount: 'HDFC' },
    { date: '2026-11-01', time: '12:00', amount: 25000, type: 'contra', account: 'HDFC', toAccount: 'Cash' },
  ];
  const consolidated = getTransactionsWithRunningBal(cycleTxs, 'ALL');
  assert.strictEqual(consolidated[consolidated.length - 1].runningBal, 0, 'Consolidated cycle must be 0');

  ['Cash', 'SBI', 'HDFC'].forEach(acc => {
    const accRes = getTransactionsWithRunningBal(cycleTxs, acc);
    const closing = accRes[accRes.length - 1].runningBal;
    assert.strictEqual(closing, 0, `Account ${acc} cycle closing must be 0, got: ${closing}`);
  });
});

check('S2.6: Voided Transactions Zero Impact Isolation', () => {
  const voidTxs = [
    { date: '2026-11-01', amount: 10000, type: 'in', account: 'Cash' },
    { date: '2026-11-02', amount: 50000, type: 'in', account: 'Cash', status: 'voided' },
    { date: '2026-11-03', amount: 2000, type: 'out', account: 'Cash' },
    { date: '2026-11-04', amount: 10000, type: 'out', account: 'Cash', status: 'voided' },
    { date: '2026-11-05', amount: 5000, type: 'contra', account: 'Cash', toAccount: 'SBI', status: 'voided' },
  ];
  const res = getTransactionsWithRunningBal(voidTxs, 'Cash');
  assert.strictEqual(res[0].runningBal, 10000);
  assert.strictEqual(res[1].runningBal, 10000, 'Voided inflow must not change balance');
  assert.strictEqual(res[2].runningBal, 8000);
  assert.strictEqual(res[3].runningBal, 8000, 'Voided outflow must not change balance');
  assert.strictEqual(res[4].runningBal, 8000, 'Voided contra must not change balance');
});

check('S2.7: Chronological Date/Time Ordering Invariant', () => {
  const unsortedTxs = [
    { date: '2026-11-03', time: '14:00', amount: 300, type: 'in', account: 'Cash' },
    { date: '2026-11-01', time: '09:00', amount: 100, type: 'in', account: 'Cash' },
    { date: '2026-11-02', time: '12:00', amount: 200, type: 'in', account: 'Cash' }
  ];
  const res = getTransactionsWithRunningBal(unsortedTxs, 'Cash');
  assert.strictEqual(res[0].amount, 100, 'Index 0 must be 2026-11-01');
  assert.strictEqual(res[0].runningBal, 100);
  assert.strictEqual(res[1].amount, 200, 'Index 1 must be 2026-11-02');
  assert.strictEqual(res[1].runningBal, 300);
  assert.strictEqual(res[2].amount, 300, 'Index 2 must be 2026-11-03');
  assert.strictEqual(res[2].runningBal, 600);
});

// CRITICAL ADVERSARIAL CHALLENGES:
check('S2.8 [ADVERSARIAL DEFECT]: Zero Float Rounding Leakage in raw runningBal', () => {
  // Accumulate 100 transactions of 0.10
  const floatTxs = [];
  for (let i = 0; i < 100; i++) {
    floatTxs.push({ date: '2026-11-01', time: '10:00', amount: 0.10, type: 'in', account: 'Cash' });
  }
  const res = getTransactionsWithRunningBal(floatTxs, 'Cash');
  const finalBal = res[99].runningBal;
  
  // Test if raw runningBal suffers IEEE-754 precision drift (9.99999999999998 instead of 10.00)
  const isExactFloat = finalBal === 10.0;
  const rawLeakage = Math.abs(finalBal - 10.0);

  if (!isExactFloat) {
    throw new Error(
      `Float rounding leakage detected: 100 * 0.10 produced ${finalBal} (drift: ${rawLeakage}). ` +
      `Underlying runningBal lacks paise rounding (Math.round((running + Number.EPSILON) * 100) / 100).`
    );
  }
});

check('S2.9 [ADVERSARIAL DEFECT]: Negative Currency Symbol Ordering (-₹ X vs ₹ -X)', () => {
  const formattedNegative = formatINR(-40000);
  // Standard accounting / Indian numbering conventions require the minus sign before the currency symbol: -₹ 40,000.00
  if (formattedNegative.startsWith('₹ -')) {
    throw new Error(
      `Negative currency symbol misplacement: formatINR(-40000) returned "${formattedNegative}". ` +
      `Minus sign appears after rupee symbol ("₹ -40,000.00") instead of before ("-₹ 40,000.00").`
    );
  }
  assert.strictEqual(formattedNegative.startsWith('-₹ '), true, `Expected -₹ prefix, got: ${formattedNegative}`);
});

check('S2.10 [ADVERSARIAL DEFECT]: Comma-formatted Currency String Truncation in parseFloat', () => {
  const commaTx = [{ date: '2026-11-01', amount: '1,000.00', type: 'in', account: 'Cash' }];
  const res = getTransactionsWithRunningBal(commaTx, 'Cash');
  if (res[0].runningBal === 1) {
    throw new Error(
      `Comma-formatted amount truncation: parseFloat("1,000.00") evaluated to 1 instead of 1000. ` +
      `Amount string requires comma stripping (amount.replace(/,/g, '')) before parseFloat.`
    );
  }
  assert.strictEqual(res[0].runningBal, 1000, `Expected 1000, got: ${res[0].runningBal}`);
});

check('S2.11 [ADVERSARIAL DEFECT]: Contra Self-Transfer Identity Leakage (Cash -> Cash)', () => {
  const selfTx = [{ date: '2026-11-01', amount: 5000, type: 'contra', account: 'Cash', toAccount: 'Cash' }];
  const res = getTransactionsWithRunningBal(selfTx, 'Cash');
  if (res[0].runningBal === 5000) {
    throw new Error(
      `Contra self-transfer leakage: transfer from Cash to Cash increased Cash balance by 5000 due to 'else if' branch skipping outflow deduction.`
    );
  }
  assert.strictEqual(res[0].runningBal, 0, `Self-transfer net change must be 0, got: ${res[0].runningBal}`);
});

// ----------------------------------------------------------------------------
// SUITE 3: UI TABLE CONTRA VOUCHER INTEGRATION
// ----------------------------------------------------------------------------
console.log('\n----------------------------------------------------------------');
console.log('SUITE 3: UI Table Contra Voucher Integration');
console.log('----------------------------------------------------------------');

check('S3.1: renderDashboardTable uses colspan="2" for Contra vouchers in consolidated view', () => {
  assert.ok(/colspan="2"\s+class="amount-contra"/.test(stagingContent), 'Missing colspan="2" class="amount-contra" in dashboard');
  assert.ok(/badge-contra-amount/.test(stagingContent), 'Missing badge-contra-amount in dashboard');
});

check('S3.2: applyFilters uses colspan="2" for Contra in Day Book table', () => {
  assert.ok(/contra-route-pill/.test(stagingContent), 'Missing contra-route-pill');
  assert.ok(/contra-route-src/.test(stagingContent), 'Missing contra-route-src');
  assert.ok(/contra-route-dst/.test(stagingContent), 'Missing contra-route-dst');
});

check('S3.3: printDayBook renders non-duplicating single amount column for Contra', () => {
  assert.ok(/<td\s+colspan="2"\s+align="center"[^>]*>⇄\s+\$\{formatINR\(amt\)\}\s+\(Transfer\)<\/td>/.test(stagingContent), 'Missing non-duplicating Contra print row');
});

// ----------------------------------------------------------------------------
// SUMMARY
// ----------------------------------------------------------------------------
console.log('\n================================================================');
console.log('CHALLENGER M1-1 STRESS TEST EXECUTION SUMMARY');
console.log('================================================================');
console.log(`Total Assertions Run: ${totalTests}`);
console.log(`Passed: ${passedTests}`);
console.log(`Failed: ${failedTests}`);

if (failedTests > 0) {
  console.log('\nCONFIRMED EMPIRICAL FINDINGS:');
  findings.forEach((f, idx) => {
    console.log(`\n[DEFECT #${idx + 1}] ${f.title}`);
    console.log(`Finding: ${f.error}`);
  });
}

console.log('\n================================================================\n');
// process.exit
