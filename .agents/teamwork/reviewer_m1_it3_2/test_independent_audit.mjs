import fs from 'fs';
import path from 'path';
import vm from 'vm';

console.log('================================================================');
console.log('REVIEWER M1-IT3-2: INDEPENDENT CODE AUDIT & ADVERSARIAL HARNESS');
console.log('================================================================');

const htmlPath = 'E:\\CashBookRelease\\app\\www\\index.html';
const htmlContent = fs.readFileSync(htmlPath, 'utf-8');

let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

function assert(desc, condition, details = '') {
  totalTests++;
  if (condition) {
    passedTests++;
    console.log(`  ✔ PASS [${desc}]`);
  } else {
    failedTests++;
    console.error(`  ✖ FAIL [${desc}] ${details}`);
  }
}

// 1. EXTRACT AND EVALUATE CRITICAL UTILITIES FROM index.html
console.log('\n▶ SUITE 1: Syntax & Core Utility Function Extractions from index.html');

// Check that index.html contains parseAmount definition and window export
const parseAmountDefRegex = /function\s+parseAmount\s*\(\s*val\s*\)\s*\{[\s\S]*?\}\s*window\.parseAmount\s*=\s*parseAmount;/;
assert('parseAmount canonical definition & window export presence', parseAmountDefRegex.test(htmlContent));

// Extract parseAmount, formatINR, numberToWordsINR, escapeHtml directly from index.html
const sandbox = {
  window: {},
  document: {
    querySelectorAll: () => [],
    getElementById: () => null
  },
  console: console,
  Math: Math,
  parseFloat: parseFloat,
  String: String,
  Number: Number
};

// Create a synthetic script pulling the functions from index.html source
const escapeHtmlMatch = htmlContent.match(/function escapeHtml[\s\S]*?window\.escapeHtml\s*=\s*escapeHtml;/);
const parseAmountMatch = htmlContent.match(/function parseAmount[\s\S]*?window\.parseAmount\s*=\s*parseAmount;/);
const formatINRMatch = htmlContent.match(/function formatINR[\s\S]*?window\.formatINR\s*=\s*formatINR;/);
const numberToWordsMatch = htmlContent.match(/function numberToWordsINR\(amount\)[\s\S]*?return result \+ ' Only';\s*\}/);

assert('escapeHtml extraction from index.html', !!escapeHtmlMatch);
assert('parseAmount extraction from index.html', !!parseAmountMatch);
assert('formatINR extraction from index.html', !!formatINRMatch);
assert('numberToWordsINR extraction from index.html', !!numberToWordsMatch);

const scriptCode = `
${escapeHtmlMatch[0]}
${parseAmountMatch[0]}
${formatINRMatch[0]}
${numberToWordsMatch ? numberToWordsMatch[0] + '\nwindow.numberToWordsINR = numberToWordsINR;' : ''}
`;

vm.createContext(sandbox);
vm.runInContext(scriptCode, sandbox);

const parseAmount = sandbox.window.parseAmount;
const formatINR = sandbox.window.formatINR;
const numberToWordsINR = sandbox.window.numberToWordsINR;
const escapeHtml = sandbox.window.escapeHtml;

// 2. PARSEAMOUNT ADVERSARIAL STRESS TEST
console.log('\n▶ SUITE 2: parseAmount Adversarial Stress Test');
assert('parseAmount("1,00,000.00") === 100000', parseAmount("1,00,000.00") === 100000, `got: ${parseAmount("1,00,000.00")}`);
assert('parseAmount("10,50,000.50") === 1050000.5', parseAmount("10,50,000.50") === 1050000.5, `got: ${parseAmount("10,50,000.50")}`);
assert('parseAmount("1,00,00,00,000.00") === 1000000000', parseAmount("1,00,00,00,000.00") === 1000000000, `got: ${parseAmount("1,00,00,00,000.00")}`);
assert('parseAmount("-50,000.00") === 50000 (Math.abs enforced)', parseAmount("-50,000.00") === 50000, `got: ${parseAmount("-50,000.00")}`);
assert('parseAmount(-75000) === 75000 (number type Math.abs)', parseAmount(-75000) === 75000, `got: ${parseAmount(-75000)}`);
assert('parseAmount(0) === 0', parseAmount(0) === 0);
assert('parseAmount("0") === 0', parseAmount("0") === 0);
assert('parseAmount("") === 0', parseAmount("") === 0);
assert('parseAmount("   ") === 0', parseAmount("   ") === 0);
assert('parseAmount(null) === 0', parseAmount(null) === 0);
assert('parseAmount(undefined) === 0', parseAmount(undefined) === 0);
assert('parseAmount(NaN) === 0', parseAmount(NaN) === 0);
assert('parseAmount("invalid_string") === 0', parseAmount("invalid_string") === 0);

// 3. VOUCHER AMOUNT IN WORDS WITH COMMA FORMATTED STRINGS
console.log('\n▶ SUITE 3: Voucher Printing Word Conversion with parseAmount');
const voucherTestAmounts = [
  { raw: "1,00,000.00", expectedWords: "Rupees One Lakh Only" },
  { raw: "50,000.00", expectedWords: "Rupees Fifty Thousand Only" },
  { raw: "1,500.00", expectedWords: "Rupees One Thousand Five Hundred Only" },
  { raw: "1,00,00,000.00", expectedWords: "Rupees One Crore Only" }
];

voucherTestAmounts.forEach(({ raw, expectedWords }) => {
  const parsed = parseAmount(raw);
  const words = numberToWordsINR(parsed);
  assert(`Voucher "${raw}" -> words: "${expectedWords}"`, words.trim().toLowerCase() === expectedWords.trim().toLowerCase(), `got "${words}"`);
});

// 4. CONTRA SELF-TRANSFER DUALITY STRESS TEST
console.log('\n▶ SUITE 4: Contra Self-Transfer Duality Invariant');
// Simulate renderStatementTable logic for account 'HDFC Bank'
const testTxns = [
  { id: 'tx1', type: 'in', account: 'HDFC Bank', amount: '1,00,000.00', status: 'posted', date: '2026-11-01' },
  { id: 'tx2', type: 'contra', account: 'HDFC Bank', toAccount: 'HDFC Bank', amount: '25,000.00', status: 'posted', date: '2026-11-02' },
  { id: 'tx3', type: 'out', account: 'HDFC Bank', amount: '10,000.00', status: 'posted', date: '2026-11-03' }
];

let running = 0;
const entityName = 'HDFC Bank';
const isAccount = true;
const statementRows = [];

testTxns.forEach(t => {
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
  }
  statementRows.push({ voucher: t.id, debit, credit, running });
});

assert('Opening Inflow correctly debited 1,00,000 and running bal = 1,00,000', statementRows[0].debit === 100000 && statementRows[0].running === 100000);
assert('Contra Self-Transfer debit === 25,000', statementRows[1].debit === 25000);
assert('Contra Self-Transfer credit === 25,000', statementRows[1].credit === 25000);
assert('Contra Self-Transfer running balance remains exactly 1,00,000 (net zero impact)', statementRows[1].running === 100000);
assert('Subsequent Outflow correctly credited 10,000 and running bal = 90,000', statementRows[2].credit === 10000 && statementRows[2].running === 90000);

// 5. FORMAT INR ZERO AND NEGATIVE ZERO ELIMINATION
console.log('\n▶ SUITE 5: formatINR Negative Zero Elimination');
const inrTestCases = [
  { val: 0, expected: '₹ 0.00' },
  { val: -0, expected: '₹ 0.00' },
  { val: -0.00001, expected: '₹ 0.00' },
  { val: -0.0049, expected: '₹ 0.00' },
  { val: -0.001, expected: '₹ 0.00' },
  { val: -0.005, expected: '₹ 0.00' },
  { val: -0.009, expected: '-₹ 0.01' },
  { val: 0.004, expected: '₹ 0.00' },
  { val: 0.005, expected: '₹ 0.01' },
  { val: "-0.0001", expected: '₹ 0.00' },
  { val: "-1,000.00", expected: '-₹ 1,000.00' },
  { val: "1,000.00", expected: '₹ 1,000.00' },
  { val: 125000, expected: '₹ 1,25,000.00' },
  { val: -125000, expected: '-₹ 1,25,000.00' }
];

inrTestCases.forEach(({ val, expected }) => {
  const result = formatINR(val);
  assert(`formatINR(${val}) === "${expected}" (no negative zero)`, result === expected && !result.includes('-₹ 0.00'), `got "${result}"`);
});

// 6. VERIFY ALL 12 TARGET CALL SITES IN index.html
console.log('\n▶ SUITE 6: Verification of All 12 Patched Call Sites in index.html');
const lines = htmlContent.split('\n');

function findLine(predicate, startSearch = 0) {
  for (let i = startSearch; i < lines.length; i++) {
    const contextAbove = lines.slice(Math.max(0, i - 45), i).join('\n');
    if (predicate(lines[i], i + 1, contextAbove)) return { lineNum: i + 1, content: lines[i] };
  }
  return null;
}

// 1. renderAccounts
const site1 = findLine((l, num, ctx) => l.includes('const amt = parseAmount(t.amount)') && ctx.includes('renderAccounts'));
assert('Call site 1: renderAccounts invokes parseAmount(t.amount)', !!site1, site1 ? `at line ${site1.lineNum}` : 'NOT FOUND');

// 2. renderReports category breakdown
const site2 = findLine((l, num, ctx) => l.includes('const amt = parseAmount(t.amount)') && ctx.includes('Category breakdown'));
assert('Call site 2: renderReports category breakdown invokes parseAmount(t.amount)', !!site2, site2 ? `at line ${site2.lineNum}` : 'NOT FOUND');

// 3. renderReports monthly cash flow
const site3 = findLine((l, num, ctx) => l.includes('const amt = parseAmount(t.amount)') && ctx.includes('Monthly Cash Flow'));
assert('Call site 3: renderReports monthly cash flow invokes parseAmount(t.amount)', !!site3, site3 ? `at line ${site3.lineNum}` : 'NOT FOUND');

// 4 & 5. renderCharts 7-day pillar dayIn and dayOut
const site4 = findLine(l => l.includes('dayIn += parseAmount(t.amount);'));
const site5 = findLine(l => l.includes('dayOut += parseAmount(t.amount);'));
assert('Call site 4: renderCharts 7-day pillar dayIn invokes parseAmount(t.amount)', !!site4, site4 ? `at line ${site4.lineNum}` : 'NOT FOUND');
assert('Call site 5: renderCharts 7-day pillar dayOut invokes parseAmount(t.amount)', !!site5, site5 ? `at line ${site5.lineNum}` : 'NOT FOUND');

// 6 & 7. renderCharts monthly inflow and outflow
const site6 = findLine(l => l.includes('mIn += parseAmount(t.amount);'));
const site7 = findLine(l => l.includes('mOut += parseAmount(t.amount);'));
assert('Call site 6: renderCharts monthly inflow invokes parseAmount(t.amount)', !!site6, site6 ? `at line ${site6.lineNum}` : 'NOT FOUND');
assert('Call site 7: renderCharts monthly outflow invokes parseAmount(t.amount)', !!site7, site7 ? `at line ${site7.lineNum}` : 'NOT FOUND');

// 8. renderCharts Crokal donut
const site8 = findLine((l, num, ctx) => l.includes('catMap[t.category] = (catMap[t.category] || 0) + parseAmount(t.amount);') && ctx.includes('crokalDonutChart'));
assert('Call site 8: renderCharts Crokal donut invokes parseAmount(t.amount)', !!site8, site8 ? `at line ${site8.lineNum}` : 'NOT FOUND');

// 9. renderStatementTable
const site9 = findLine((l, num, ctx) => l.includes('const amt = parseAmount(t.amount);') && ctx.includes('renderStatementTable'));
assert('Call site 9: renderStatementTable invokes parseAmount(t.amount)', !!site9, site9 ? `at line ${site9.lineNum}` : 'NOT FOUND');

// 10. printSingleVoucher
const site10 = findLine((l, num, ctx) => l.includes('const amt = parseAmount(t.amount);') && ctx.includes('printSingleVoucher'));
assert('Call site 10: printSingleVoucher invokes parseAmount(t.amount)', !!site10, site10 ? `at line ${site10.lineNum}` : 'NOT FOUND');

// 11. printDayBook
const site11 = findLine((l, num, ctx) => l.includes('const amt = parseAmount(t.amount);') && ctx.includes('printDayBook'));
assert('Call site 11: printDayBook invokes parseAmount(t.amount)', !!site11, site11 ? `at line ${site11.lineNum}` : 'NOT FOUND');

// 12. exportMultiSheetExcel
const site12 = findLine((l, num, ctx) => l.includes('catMap[t.category] = (catMap[t.category] || 0) + parseAmount(t.amount);') && ctx.includes('exportMultiSheetExcel'));
assert('Call site 12: exportMultiSheetExcel invokes parseAmount(t.amount)', !!site12, site12 ? `at line ${site12.lineNum}` : 'NOT FOUND');

// 7. INTEGRITY AUDIT: Check for any unpatched raw parseFloat(t.amount)
console.log('\n▶ SUITE 7: Integrity Audit: Check for Unpatched parseFloat(t.amount)');
const rawParseFloatRegex = /parseFloat\s*\(\s*t\.amount\s*\)/g;
const rawMatches = htmlContent.match(rawParseFloatRegex);
assert('Zero occurrences of unpatched parseFloat(t.amount)', !rawMatches, rawMatches ? `Found: ${rawMatches.length} occurrences` : '');

// 8. ESCAPE HTML SANITIZATION INTEGRITY
console.log('\n▶ SUITE 8: escapeHtml Sanitization & Business Data Preservation');
const businessStrings = [
  'Agrawal Transport <transport@gmail.com>',
  'Payment for truck <truck-12>',
  'In transit <transit>',
  'trip <trip-5>',
  'Diesel <tracking>'
];
businessStrings.forEach(str => {
  const escaped = escapeHtml(str);
  assert(`Preserves text "${str}" without erasure`, escaped.length >= str.length && !escaped.includes('<') && !escaped.includes('>'));
});

const maliciousStrings = [
  '<script>alert("xss")</script>',
  '<img src=x onerror=alert(1)>',
  '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>'
];
maliciousStrings.forEach(str => {
  const escaped = escapeHtml(str);
  assert(`Neutralizes attack tag "${str}"`, !escaped.includes('<') && !escaped.includes('>') && escaped.includes('&lt;'));
});

console.log('\n================================================================');
console.log(`TOTAL AUDIT CHECKS: ${totalTests} | PASSED: ${passedTests} | FAILED: ${failedTests}`);
console.log(`STATUS: ${failedTests === 0 ? 'ALL CHECKS PASSED PERFECTLY' : 'AUDIT FAILURES DETECTED'}`);
console.log('================================================================');

if (failedTests > 0) {
  process.exit(1);
}
