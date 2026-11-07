import fs from 'node:fs';
import crypto from 'node:crypto';
import vm from 'node:vm';

console.log('================================================================');
console.log('  REVIEWER & ADVERSARIAL CRITIC — INDEPENDENT VERIFICATION SUITE');
console.log('================================================================');

let passed = 0;
let failed = 0;
const findings = [];

function assert(condition, name, details, severity = 'CRITICAL') {
  if (condition) {
    passed++;
    console.log(`  ✔ PASS: ${name}`);
  } else {
    failed++;
    findings.push({ severity, name, details });
    console.error(`  ✖ FAIL [${severity}]: ${name} — ${details}`);
  }
}

// -----------------------------------------------------------------------------
// 1. FILE PARITY & INTEGRITY
// -----------------------------------------------------------------------------
console.log('\n[SECTION 1: File Checksum Parity & Identity]');

const releasePath = 'E:/CashBookRelease/app/www/index.html';
const appDataPath = 'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html';

assert(fs.existsSync(releasePath), 'Release index.html exists', `Path: ${releasePath}`);
assert(fs.existsSync(appDataPath), 'AppData index.html exists', `Path: ${appDataPath}`);

const releaseBuf = fs.readFileSync(releasePath);
const appDataBuf = fs.readFileSync(appDataPath);

const releaseHash = crypto.createHash('sha256').update(releaseBuf).digest('hex').toUpperCase();
const appDataHash = crypto.createHash('sha256').update(appDataBuf).digest('hex').toUpperCase();

console.log(`  Release SHA256 : ${releaseHash} (${releaseBuf.length} bytes)`);
console.log(`  AppData SHA256 : ${appDataHash} (${appDataBuf.length} bytes)`);

assert(releaseHash === appDataHash, 'SHA256 Checksum Parity', `Release: ${releaseHash}, AppData: ${appDataHash}`);
assert(releaseBuf.length === appDataBuf.length, 'File Size Parity', `Size: ${releaseBuf.length} bytes`);

// -----------------------------------------------------------------------------
// 2. EXTRACTION & EVALUATION OF CORE IMPLEMENTATIONS
// -----------------------------------------------------------------------------
console.log('\n[SECTION 2: Extraction of Core Functions from index.html]');

const htmlContent = releaseBuf.toString('utf8');

// Extract escapeHtml
const escapeMatch = htmlContent.match(/function escapeHtml\([\s\S]*?window\.escapeHtml\s*=\s*escapeHtml;/);
assert(Boolean(escapeMatch), 'escapeHtml function present in index.html', 'Could not locate escapeHtml definition');

// Extract parseAmount
const parseMatch = htmlContent.match(/function parseAmount\([\s\S]*?window\.parseAmount\s*=\s*parseAmount;/);
assert(Boolean(parseMatch), 'parseAmount function present in index.html', 'Could not locate parseAmount definition');

// Extract formatINR
const formatMatch = htmlContent.match(/function formatINR\([\s\S]*?window\.formatINR\s*=\s*formatINR;/);
assert(Boolean(formatMatch), 'formatINR function present in index.html', 'Could not locate formatINR definition');

// Extract numberToWordsINR
const wordsMatch = htmlContent.match(/function numberToWordsINR\([\s\S]*?return result \+ ' Only';\s*\}/);
assert(Boolean(wordsMatch), 'numberToWordsINR function present in index.html', 'Could not locate numberToWordsINR definition');

// Create sandbox to execute extracted functions
const sandbox = {
  window: {},
  Math: Math,
  Number: Number,
  String: String,
  parseFloat: parseFloat,
  parseInt: parseInt,
  isNaN: isNaN
};
vm.createContext(sandbox);

vm.runInContext(escapeMatch[0], sandbox);
vm.runInContext(parseMatch[0], sandbox);
vm.runInContext(formatMatch[0], sandbox);
vm.runInContext(wordsMatch[0] + '; window.numberToWordsINR = numberToWordsINR;', sandbox);

const { escapeHtml, parseAmount, formatINR, numberToWordsINR } = sandbox.window;

// -----------------------------------------------------------------------------
// 3. INTEGRITY & ANTI-CHEAT AUDIT
// -----------------------------------------------------------------------------
console.log('\n[SECTION 3: Code Integrity & Anti-Cheat Audit]');

// Check that escapeHtml has no shortcut regex or table-stripping returning ''
const escapeFnCode = escapeMatch[0];
const hasDestructiveTableRegex = /<\/?(?:td|tr|table|tbody|thead)[^>]*>/i.test(escapeFnCode);
assert(!hasDestructiveTableRegex, 'Absence of destructive table-stripping regex in escapeHtml', 'Found table regex inside escapeHtml');

const hasReturnEmptyShortcut = /return\s*['"]['"]\s*;/.test(escapeFnCode.replace(/if\s*\(\s*str\s*===?\s*null[\s\S]*?\)\s*return\s*['"]['"]\s*;/, ''));
assert(!hasReturnEmptyShortcut, 'Absence of fake/empty return shortcut in escapeHtml', 'escapeHtml contains suspicious empty string return shortcut');

// Check that parseAmount does not have hardcoded test values
const parseFnCode = parseMatch[0];
const hasHardcodedAmount = /100000|1,00,000|50000/.test(parseFnCode);
assert(!hasHardcodedAmount, 'Absence of hardcoded amount values in parseAmount', 'parseAmount appears to have hardcoded test numbers');

// -----------------------------------------------------------------------------
// 4. ADVERSARIAL STRESS-TESTING: escapeHtml
// -----------------------------------------------------------------------------
console.log('\n[SECTION 4: Adversarial Stress-Testing — escapeHtml]');

const adversarialInputs = [
  // 1. Dangerous XSS vectors
  { input: '<script>alert("XSS")</script>', mustNotContain: ['<script>', '</script>'], desc: 'Standard script tag' },
  { input: '<img src=x onerror=alert(1)>', mustNotContain: ['<img', '>'], desc: 'Image onerror payload' },
  { input: '<svg/onload=alert("SVG")>', mustNotContain: ['<svg', '>'], desc: 'SVG onload payload' },
  { input: '"><script>alert(1)</script>', mustNotContain: ['<script>'], desc: 'Attribute escaping breakout' },
  { input: '\'<script>alert(1)</script>', mustNotContain: ['<script>'], desc: 'Single quote breakout' },
  { input: '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>', mustNotContain: ['<td', '</td>', '<tr', '</tr>'], desc: 'Table structural breakout payload' },
  { input: '<!-- <script>alert(1)</script> -->', mustNotContain: ['<script>'], desc: 'HTML comment wrapping script' },
  { input: '<a href="javascript:alert(1)">Click</a>', mustNotContain: ['<a', '</a>'], desc: 'JavaScript URI in link' },

  // 2. Legitimate Transport & Business Logistics strings (MUST NOT BE ERASED!)
  { input: 'Agrawal Transport <transport@gmail.com>', mustPreserve: 'Agrawal Transport &lt;transport@gmail.com&gt;', desc: 'Transport company email' },
  { input: 'Payment for truck <truck-12>', mustPreserve: 'Payment for truck &lt;truck-12&gt;', desc: 'Truck identifier tag' },
  { input: 'In transit <transit>', mustPreserve: 'In transit &lt;transit&gt;', desc: 'Transit status tag' },
  { input: 'Trip freight <trip-5>', mustPreserve: 'Trip freight &lt;trip-5&gt;', desc: 'Trip identifier tag' },
  { input: 'Diesel refill <tracking>', mustPreserve: 'Diesel refill &lt;tracking&gt;', desc: 'Tracking tag' },
  { input: 'Container # <container-40ft>', mustPreserve: 'Container # &lt;container-40ft&gt;', desc: 'Container tag' },
  { input: 'Toll plaza receipt 10 < 20 & 30 > 15', mustPreserve: 'Toll plaza receipt 10 &lt; 20 &amp; 30 &gt; 15', desc: 'Mathematical comparisons' },
  { input: 'Driver: "Ramesh" & \'Suresh\'', mustPreserve: 'Driver: &quot;Ramesh&quot; &amp; &#39;Suresh&#39;', desc: 'Quotes and ampersands' },
  { input: 'श्री कृष्णा लॉजिस्टिक्स (ट्रांसपोर्ट & वेयरहाउस)', mustPreserve: 'श्री कृष्णा लॉजिस्टिक्स (ट्रांसपोर्ट &amp; वेयरहाउस)', desc: 'Unicode Devanagari script' },
  { input: 'Special / \\ [ ] { } ( ) * + ? . ^ $ |', mustPreserve: 'Special / \\ [ ] { } ( ) * + ? . ^ $ |', desc: 'Regex special characters' },
  { input: null, mustPreserve: '', desc: 'null value handling' },
  { input: undefined, mustPreserve: '', desc: 'undefined value handling' },
  { input: 0, mustPreserve: '0', desc: 'numeric zero handling' }
];

adversarialInputs.forEach((test, idx) => {
  const result = escapeHtml(test.input);

  if (test.mustNotContain) {
    let passedCheck = true;
    for (const forbidden of test.mustNotContain) {
      if (result.includes(forbidden)) {
        passedCheck = false;
        assert(false, `XSS vector neutralized (${test.desc})`, `Result still contains raw "${forbidden}": ${result}`);
        break;
      }
    }
    if (passedCheck) {
      assert(true, `XSS vector neutralized: ${test.desc}`, '');
    }
  }

  if (test.mustPreserve !== undefined) {
    const matched = result === test.mustPreserve;
    assert(matched, `Data preserved faithfully: ${test.desc}`, `Expected: "${test.mustPreserve}", Got: "${result}"`);
  }
});

// -----------------------------------------------------------------------------
// 5. ADVERSARIAL STRESS-TESTING: parseAmount
// -----------------------------------------------------------------------------
console.log('\n[SECTION 5: Adversarial Stress-Testing — parseAmount]');

const amountCases = [
  { input: '1,00,000.00', expected: 100000, desc: 'Standard Indian 1 Lakh with commas' },
  { input: '1,25,00,000.50', expected: 12500000.50, desc: 'Indian 1.25 Crore with decimals' },
  { input: '-50,000.00', expected: 50000, desc: 'Negative comma amount coerced to positive magnitude' },
  { input: '  10,000  ', expected: 10000, desc: 'Padded whitespace' },
  { input: '0', expected: 0, desc: 'String zero' },
  { input: 0, expected: 0, desc: 'Number zero' },
  { input: -0, expected: 0, desc: 'Number negative zero' },
  { input: null, expected: 0, desc: 'null value' },
  { input: undefined, expected: 0, desc: 'undefined value' },
  { input: '', expected: 0, desc: 'Empty string' },
  { input: 'invalid', expected: 0, desc: 'Non-numeric string' },
  { input: NaN, expected: 0, desc: 'NaN value' },
  { input: 999999999.99, expected: 999999999.99, desc: 'Large numeric value' },
  { input: '1,000,000.00', expected: 1000000, desc: 'Western 1 Million notation' }
];

amountCases.forEach(tc => {
  const parsed = parseAmount(tc.input);
  const ok = Math.abs(parsed - tc.expected) < 0.0001;
  assert(ok, `parseAmount: ${tc.desc}`, `Input: ${JSON.stringify(tc.input)}, Expected: ${tc.expected}, Got: ${parsed}`);
});

// -----------------------------------------------------------------------------
// 6. ADVERSARIAL STRESS-TESTING: formatINR & Negative Zero
// -----------------------------------------------------------------------------
console.log('\n[SECTION 6: Adversarial Stress-Testing — formatINR & Negative Zero]');

const formatTestCases = [
  { val: 0, expected: '₹ 0.00', desc: 'Positive Zero' },
  { val: -0, expected: '₹ 0.00', desc: 'Negative Zero' },
  { val: -0.0000001, expected: '₹ 0.00', desc: 'Micro negative value' },
  { val: -0.0049, expected: '₹ 0.00', desc: 'Sub-half-cent negative' },
  { val: -0.0051, expected: '-₹ 0.01', desc: 'Half-cent roundable negative' },
  { val: -40000, expected: '-₹ 40,000.00', desc: 'Large negative round number' },
  { val: 10000000, expected: '₹ 1,00,00,000.00', desc: 'One Crore positive' },
  { val: '1,00,000.00', expected: '₹ 1,00,000.00', desc: 'Pre-formatted string input' },
  { val: '-1,00,000.00', expected: '-₹ 1,00,000.00', desc: 'Pre-formatted negative string' },
  { val: null, expected: '₹ 0.00', desc: 'null value' },
  { val: undefined, expected: '₹ 0.00', desc: 'undefined value' },
  { val: NaN, expected: '₹ 0.00', desc: 'NaN value' }
];

formatTestCases.forEach(tc => {
  const formatted = formatINR(tc.val);
  const hasNegativeZero = formatted.includes('-₹ 0.00') || formatted.includes('₹ -0.00');
  assert(!hasNegativeZero, `No negative zero in formatINR (${tc.desc})`, `Got: "${formatted}"`);
  assert(formatted === tc.expected, `formatINR output match (${tc.desc})`, `Expected: "${tc.expected}", Got: "${formatted}"`);
});

// -----------------------------------------------------------------------------
// 7. ADVERSARIAL STRESS-TESTING: Voucher Printing Words with Comma String
// -----------------------------------------------------------------------------
console.log('\n[SECTION 7: Voucher Printing Words with Comma String]');

const voucherAmountStr = '1,00,000.00';
const parsedVoucherAmt = parseAmount(voucherAmountStr);
const words = numberToWordsINR(parsedVoucherAmt);

console.log(`  Voucher amount: "${voucherAmountStr}" -> parseAmount: ${parsedVoucherAmt} -> words: "${words}"`);
assert(parsedVoucherAmt === 100000, 'Voucher amount parsed as 100,000 (not truncated to 1)', `Got: ${parsedVoucherAmt}`);
assert(words === 'Rupees One Lakh Only', 'Voucher words converted to "Rupees One Lakh Only"', `Got: "${words}"`);

// -----------------------------------------------------------------------------
// 8. ADVERSARIAL STRESS-TESTING: Contra Self-Transfer Balance Invariant
// -----------------------------------------------------------------------------
console.log('\n[SECTION 8: Contra Self-Transfer Symmetry & Balance Invariants]');

// Test statement table contra self-transfer simulation
function computeStatementRunning(txns, entityName) {
  let running = 50000;
  for (const t of txns) {
    const amt = parseAmount(t.amount);
    let debit = 0, credit = 0;
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
  return running;
}

const selfContraTxn = [
  { type: 'contra', account: 'HDFC Bank', toAccount: 'HDFC Bank', amount: '25,000.00', status: 'posted' }
];

const balanceAfterSelfContra = computeStatementRunning(selfContraTxn, 'HDFC Bank');
assert(balanceAfterSelfContra === 50000, 'Contra self-transfer running balance unchanged', `Expected 50000, got ${balanceAfterSelfContra}`);

const interContraTxn = [
  { type: 'contra', account: 'HDFC Bank', toAccount: 'Cash in Hand', amount: '10,000.00', status: 'posted' }
];
const hdfcAfterInter = computeStatementRunning(interContraTxn, 'HDFC Bank');
const cashAfterInter = computeStatementRunning(interContraTxn, 'Cash in Hand');
assert(hdfcAfterInter === 40000, 'Inter-account contra: source account debited/reduced by 10,000', `Got ${hdfcAfterInter}`);
assert(cashAfterInter === 60000, 'Inter-account contra: destination account credited/increased by 10,000', `Got ${cashAfterInter}`);

// -----------------------------------------------------------------------------
// 9. AUDIT FOR BANNED AI VISUAL ARTIFACTS IN CSS
// -----------------------------------------------------------------------------
console.log('\n[SECTION 9: Banned AI Visual Artifacts Audit in CSS]');

const bannedCssPatterns = [
  { name: '@keyframes pulseGlow', regex: /@keyframes\s+pulseGlow/i },
  { name: 'Oversaturated filter blur halo (>= 16px)', regex: /filter\s*:\s*blur\(\s*(?:1[6-9]|[2-9]\d|\d{3,})px\s*\)/i },
  { name: 'Oversaturated diffuse drop-shadow (>= 32px)', regex: /box-shadow\s*:[^;]*(?:3[2-9]|[4-9]\d|\d{3,})px/i }
];

bannedCssPatterns.forEach(pattern => {
  const match = htmlContent.match(pattern.regex);
  assert(!match, `Absence of banned AI artifact: ${pattern.name}`, match ? `Found: ${match[0]}` : '');
});

// -----------------------------------------------------------------------------
// SUMMARY & VERDICT
// -----------------------------------------------------------------------------
console.log('\n================================================================');
console.log('ADVERSARIAL VERIFICATION SUMMARY');
console.log('================================================================');
console.log(`Passed: ${passed} | Failed: ${failed}`);

if (findings.length > 0) {
  console.log('\nCRITICAL FINDINGS:');
  findings.forEach((f, i) => console.log(`  ${i + 1}. [${f.severity}] ${f.name}: ${f.details}`));
}

const finalVerdict = failed === 0 ? 'APPROVE' : 'REQUEST_CHANGES';
console.log(`\nINDEPENDENT REVIEWER VERDICT: ${finalVerdict}\n`);

process.exit(failed > 0 ? 1 : 0);
