import fs from 'node:fs';

console.log('======================================================');
console.log('  EXPLORER M1-IT3-2 VERIFICATION & SIMULATION TEST');
console.log('======================================================');

// 1. CANONICAL parseAmount DEFINITION
function parseAmount(val) {
  if (val === null || val === undefined) return 0;
  const clean = typeof val === 'string' ? val.replace(/,/g, '') : val;
  return Math.abs(parseFloat(clean) || 0);
}

// TEST 1: parseAmount EDGE CASES
console.log('\n--- Test 1: parseAmount Edge Cases ---');
const parseCases = [
  { in: null, exp: 0 },
  { in: undefined, exp: 0 },
  { in: '', exp: 0 },
  { in: '   ', exp: 0 },
  { in: 0, exp: 0 },
  { in: -0, exp: 0 },
  { in: 100, exp: 100 },
  { in: -100, exp: 100 },
  { in: '100', exp: 100 },
  { in: '-100', exp: 100 },
  { in: '1,00,000.00', exp: 100000 },
  { in: '1,00,000.50', exp: 100000.5 },
  { in: '50,000', exp: 50000 },
  { in: '1,23,45,678.90', exp: 12345678.9 },
  { in: '-50,000.25', exp: 50000.25 },
  { in: NaN, exp: 0 },
  { in: 'invalid', exp: 0 }
];

let parseFailures = 0;
parseCases.forEach((tc, idx) => {
  const actual = parseAmount(tc.in);
  const pass = actual === tc.exp;
  if (!pass) {
    console.error(`  FAIL parseAmount[${idx}]: in=${tc.in}, expected=${tc.exp}, actual=${actual}`);
    parseFailures++;
  } else {
    console.log(`  ✔ PASS parseAmount: ${String(tc.in).padEnd(16)} => ${actual}`);
  }
});

// 2. formatINR PRE-ROUNDING & NEGATIVE ZERO ELIMINATION
console.log('\n--- Test 2: formatINR Negative Zero Elimination ---');
function formatINR(val) {
  const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
  const num = parseFloat(cleanVal) || 0;
  const rounded = Math.round((num + Number.EPSILON) * 100) / 100;
  const isNeg = rounded < 0;
  const absVal = Math.abs(rounded);
  return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const formatCases = [
  { in: 0, notExp: '-₹ 0.00', exp: '₹ 0.00' },
  { in: -0, notExp: '-₹ 0.00', exp: '₹ 0.00' },
  { in: -0.00001, notExp: '-₹ 0.00', exp: '₹ 0.00' },
  { in: -0.004, notExp: '-₹ 0.00', exp: '₹ 0.00' },
  { in: -0.0049, notExp: '-₹ 0.00', exp: '₹ 0.00' },
  { in: -0.0051, notExp: '-₹ 0.00', exp: '-₹ 0.01' },
  { in: -0.01, notExp: '-₹ 0.00', exp: '-₹ 0.01' },
  { in: -40000, notExp: '-₹ 0.00', exp: '-₹ 40,000.00' },
  { in: '1,00,000.00', notExp: '-₹ 0.00', exp: '₹ 1,00,000.00' },
  { in: '-1,00,000.00', notExp: '-₹ 0.00', exp: '-₹ 1,00,000.00' }
];

let formatFailures = 0;
formatCases.forEach((tc, idx) => {
  const actual = formatINR(tc.in);
  const pass = actual === tc.exp && !actual.includes('-₹ 0.00');
  if (!pass) {
    console.error(`  FAIL formatINR[${idx}]: in=${tc.in}, expected=${tc.exp}, actual=${actual}`);
    formatFailures++;
  } else {
    console.log(`  ✔ PASS formatINR: ${String(tc.in).padEnd(16)} => ${actual}`);
  }
});

// 3. VOUCHER PRINTING WITH COMMA STRING
console.log('\n--- Test 3: Voucher Printing (Dead-End Remediation) ---');
// Read numberToWordsINR from index.html
const htmlContent = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');
const numWordsMatch = htmlContent.match(/function numberToWordsINR[\s\S]*?return result \+ ' Only';\s*\}/);
if (!numWordsMatch) {
  console.error('numberToWordsINR not found in index.html');
  process.exit(1);
}
const numberToWordsINR = new Function(numWordsMatch[0] + '; return numberToWordsINR;')();

const commaVoucher = {
  id: 'tx-test-1',
  voucher: 'VCH-2026-TEST',
  amount: '1,00,000.00',
  type: 'in'
};

// Raw parseFloat behavior (DEAD END)
const deadEndAmt = parseFloat(commaVoucher.amount) || 0;
const deadEndWords = numberToWordsINR(deadEndAmt);
console.log(`  Unpatched parseFloat("${commaVoucher.amount}") => ${deadEndAmt} => "${deadEndWords}" (TRUNCATED DEFECT!)`);

// Patched parseAmount behavior
const patchedAmt = parseAmount(commaVoucher.amount);
const patchedWords = numberToWordsINR(patchedAmt);
console.log(`  Patched parseAmount("${commaVoucher.amount}") => ${patchedAmt} => "${patchedWords}"`);
if (patchedWords !== 'Rupees One Lakh Only') {
  console.error(`  FAIL: Expected "Rupees One Lakh Only", got "${patchedWords}"`);
  process.exit(1);
} else {
  console.log('  ✔ PASS: Voucher printing correctly translates comma-formatted ₹1,00,000.00 to words');
}

// 4. CONTRA SELF-TRANSFER IN renderStatementTable
console.log('\n--- Test 4: Contra Self-Transfer in renderStatementTable ---');
function simulateStatementRow(t, entityName, isAccount, decouple) {
  const amt = parseAmount(t.amount);
  let debit = 0, credit = 0, running = 25000;

  if (isAccount) {
    if (t.status !== 'voided') {
      if (t.type === 'in' && t.account === entityName) { debit = amt; running += amt; }
      else if (t.type === 'out' && t.account === entityName) { credit = amt; running -= amt; }
      else if (t.type === 'contra') {
        if (decouple) {
          if (t.toAccount === entityName) { debit = amt; running += amt; }
          if (t.account === entityName) { credit = amt; running -= amt; }
        } else {
          if (t.toAccount === entityName) { debit = amt; running += amt; }
          else if (t.account === entityName) { credit = amt; running -= amt; }
        }
      }
      running = Math.round((running + Number.EPSILON) * 100) / 100;
    }
  }

  return { debit, credit, running };
}

const selfContra = {
  type: 'contra',
  account: 'Cash in Hand',
  toAccount: 'Cash in Hand',
  amount: '10,000.00',
  status: 'posted'
};

const beforeRes = simulateStatementRow(selfContra, 'Cash in Hand', true, false);
console.log('  Before fix (coupled else if):', beforeRes, '(DEFECT: running balance inflated by ₹10,000)');

const afterRes = simulateStatementRow(selfContra, 'Cash in Hand', true, true);
console.log('  After fix (decoupled if):', afterRes);

if (afterRes.running !== 25000 || afterRes.debit !== 10000 || afterRes.credit !== 10000) {
  console.error('  FAIL: After fix running balance should remain unchanged (25,000)');
  process.exit(1);
} else {
  console.log('  ✔ PASS: Contra self-transfer nets out to zero running change (25,000 -> 25,000)');
}

// Inter-account transfer check
const interContra = {
  type: 'contra',
  account: 'Cash in Hand',
  toAccount: 'SBI Current A/c',
  amount: '5,000.00',
  status: 'posted'
};
const cashStatement = simulateStatementRow(interContra, 'Cash in Hand', true, true);
const sbiStatement = simulateStatementRow(interContra, 'SBI Current A/c', true, true);
console.log('  Inter-account Cash in Hand view:', cashStatement);
console.log('  Inter-account SBI Current view:', sbiStatement);
if (cashStatement.credit !== 5000 || cashStatement.running !== 20000) {
  console.error('  FAIL: Cash in Hand outflow should be 5000 credit, running 20000');
  process.exit(1);
}
if (sbiStatement.debit !== 5000 || sbiStatement.running !== 30000) {
  console.error('  FAIL: SBI Current inflow should be 5000 debit, running 30000');
  process.exit(1);
}
console.log('  ✔ PASS: Inter-account contra transfer preserves debit/credit duality');

// 5. VERIFY ALL 12 TARGET SITES IN index.html
console.log('\n--- Test 5: Verify All 12 Call Sites in index.html ---');
const rawHtml = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');
const lines = rawHtml.split('\n');

const targetSites = [
  { line: 3957, pattern: 'const amt = parseFloat(t.amount) || 0;', scope: 'renderAccounts' },
  { line: 4001, pattern: 'const amt = parseFloat(t.amount) || 0;', scope: 'renderReports (Categories)' },
  { line: 4031, pattern: 'const amt = parseFloat(t.amount) || 0;', scope: 'renderReports (Monthly)' },
  { line: 4104, pattern: "if (t.type === 'in') dayIn += parseFloat(t.amount) || 0;", scope: 'renderCharts (7d In)' },
  { line: 4105, pattern: "if (t.type === 'out') dayOut += parseFloat(t.amount) || 0;", scope: 'renderCharts (7d Out)' },
  { line: 4119, pattern: 'mIn += parseFloat(t.amount) || 0;', scope: 'renderCharts (FY In)' },
  { line: 4129, pattern: 'mOut += parseFloat(t.amount) || 0;', scope: 'renderCharts (FY Out)' },
  { line: 4193, pattern: 'catMap[t.category] = (catMap[t.category] || 0) + (parseFloat(t.amount) || 0);', scope: 'renderCharts (Donut)' },
  { line: 4878, pattern: 'const amt = parseFloat(t.amount) || 0;', scope: 'renderStatementTable' },
  { line: 5153, pattern: 'const amt = parseFloat(t.amount) || 0;', scope: 'printSingleVoucher' },
  { line: 5243, pattern: 'const amt = parseFloat(t.amount) || 0;', scope: 'printDayBook' },
  { line: 5352, pattern: 'catMap[t.category] = (catMap[t.category] || 0) + (parseFloat(t.amount) || 0);', scope: 'exportMultiSheetExcel' }
];

let siteErrors = 0;
targetSites.forEach(site => {
  const lineIdx = site.line - 1;
  const actualLine = lines[lineIdx]?.trim();
  if (actualLine === site.pattern) {
    console.log(`  ✔ PASS Line ${site.line} (${site.scope}): Exact match found`);
  } else {
    console.error(`  FAIL Line ${site.line} (${site.scope}):\n    Expected: ${site.pattern}\n    Found:    ${actualLine}`);
    siteErrors++;
  }
});

// Check contra self-transfer lines
const contraLine = lines[4886]?.trim();
console.log(`\n  Checking contra else if at line 4887: "${lines[4886]?.trim()}"`);
if (contraLine === 'else if (t.account === entityName) { credit = amt; running -= amt; }') {
  console.log('  ✔ PASS: Found target line 4887 for contra decoupling');
} else {
  console.error(`  FAIL: Target line 4887 does not match expected, found: ${contraLine}`);
  siteErrors++;
}

// Check formatINR lines
console.log(`\n  Checking formatINR at lines 3396-3402:`);
const formatINRBlock = lines.slice(3395, 3403).join('\n');
console.log(formatINRBlock);

console.log('\n======================================================');
console.log(`ALL CHECKS COMPLETE: ${parseFailures + formatFailures + siteErrors === 0 ? 'ALL PASSED (100%)' : 'FAILURES DETECTED'}`);
console.log('======================================================');
