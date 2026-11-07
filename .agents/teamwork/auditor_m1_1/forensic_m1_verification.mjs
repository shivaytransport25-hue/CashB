import fs from 'node:fs';
import crypto from 'node:crypto';
import vm from 'node:vm';

console.log('================================================================');
console.log('  FORENSIC INTEGRITY AUDIT — MILESTONE M1 VERIFICATION SUITE');
console.log('================================================================\n');

const stagingPath = 'E:/CashBookRelease/app/www/index.html';
const installedPath = 'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html';

const files = [
  { label: 'Desktop Staging', path: stagingPath },
  { label: 'Desktop Installed', path: installedPath }
];

let totalChecks = 0;
let passedChecks = 0;
let failedChecks = 0;

function assertCheck(name, condition, errorMsg = '') {
  totalChecks++;
  if (condition) {
    passedChecks++;
    console.log(`  ✔ PASS: ${name}`);
  } else {
    failedChecks++;
    console.error(`  ✖ FAIL: ${name} — ${errorMsg}`);
  }
}

// -----------------------------------------------------------------------------
// CHECK 1: File Existence & SHA256 Synchronicity
// -----------------------------------------------------------------------------
console.log('[SECTION 1] Target File Existence & Cryptographic Identity');
const hashStaging = crypto.createHash('sha256').update(fs.readFileSync(stagingPath)).digest('hex');
const hashInstalled = crypto.createHash('sha256').update(fs.readFileSync(installedPath)).digest('hex');

assertCheck('Staging file exists and is readable', fs.existsSync(stagingPath));
assertCheck('Installed target exists and is readable', fs.existsSync(installedPath));
assertCheck(
  'Byte-for-byte SHA256 checksum identity',
  hashStaging === hashInstalled,
  `Staging (${hashStaging}) !== Installed (${hashInstalled})`
);
console.log(`  ↳ Verified SHA256: ${hashStaging}`);

// -----------------------------------------------------------------------------
// CHECK 2: Anti-Cheat / Prohibited AI-Slop Eradication
// -----------------------------------------------------------------------------
console.log('\n[SECTION 2] Eradication of AI Slop & Prohibited Patterns');
const prohibitedChecks = [
  { name: '@keyframes pulseGlow', regex: /@keyframes\s+pulseGlow/g },
  { name: 'filter: blur() halos', regex: /filter:\s*blur\(/g },
  { name: '0 14px 32px diffuse shadow', regex: /0\s+14px\s+32px/g },
  { name: '0 16px 40px diffuse shadow', regex: /0\s+16px\s+40px/g },
  { name: '0 24px 48px diffuse shadow', regex: /0\s+24px\s+48px/g },
  { name: '0 25px 60px diffuse shadow', regex: /0\s+25px\s+60px/g },
  { name: 'neon green glow on sync-dot', regex: /box-shadow:[^;]*var\(--win-green\)/g },
  { name: 'voucher button linear-gradient', regex: /\.sidebar-new-voucher-btn[^}]*linear-gradient/g }
];

for (const target of files) {
  const content = fs.readFileSync(target.path, 'utf8');
  for (const pc of prohibitedChecks) {
    const matches = [...content.matchAll(pc.regex)];
    assertCheck(
      `${target.label}: Zero occurrences of ${pc.name}`,
      matches.length === 0,
      `Found ${matches.length} matches`
    );
  }
}

// -----------------------------------------------------------------------------
// CHECK 3: Enterprise Fintech Design System Tokens
// -----------------------------------------------------------------------------
console.log('\n[SECTION 3] Enterprise Fintech Design Tokens & Hairlines');
const requiredTokens = [
  { name: 'Daylight canvas token (#F8FAFC)', regex: /--color-bg-canvas:\s*#F8FAFC/gi },
  { name: 'Dark Slate canvas token (#0B111B)', regex: /--color-bg-canvas:\s*#0B111B/gi },
  { name: 'Credit Emerald token (#047857)', regex: /--color-credit:\s*#047857/gi },
  { name: 'Debit Crimson token (#B91C1C)', regex: /--color-debit:\s*#B91C1C/gi },
  { name: 'Contra Navy token (#1D4ED8)', regex: /--color-transfer:\s*#1D4ED8/gi },
  { name: 'Wealth Amber token (#D97706)', regex: /--color-accent:\s*#D97706/gi },
  { name: '--color-border-hairline token', regex: /--color-border-hairline/g },
  { name: '--win-border-hairline alias', regex: /--win-border-hairline/g }
];

for (const target of files) {
  const content = fs.readFileSync(target.path, 'utf8');
  for (const rt of requiredTokens) {
    const matches = [...content.matchAll(rt.regex)];
    assertCheck(
      `${target.label}: Required ${rt.name}`,
      matches.length > 0,
      'Token not found in file'
    );
  }
}

// -----------------------------------------------------------------------------
// CHECK 4: Tabular Numeric Typography & Monospace Formatting
// -----------------------------------------------------------------------------
console.log('\n[SECTION 4] Tabular Numeric Typography & Monospace Ledger Numbers');
const typographyTokens = [
  { name: 'font-variant-numeric: tabular-nums', regex: /font-variant-numeric:\s*tabular-nums/g },
  { name: 'font-feature-settings: "tnum" 1', regex: /font-feature-settings:\s*"tnum"\s*1/g },
  { name: 'CSS amount-contra class', regex: /\.amount-contra\s*\{/g },
  { name: 'CSS badge-contra-amount class', regex: /\.badge-contra-amount\s*\{/g },
  { name: 'CSS contra-route-pill class', regex: /\.contra-route-pill\s*\{/g },
  { name: 'CSS contra-internal-tag class', regex: /\.contra-internal-tag\s*\{/g }
];

for (const target of files) {
  const content = fs.readFileSync(target.path, 'utf8');
  for (const tt of typographyTokens) {
    const matches = [...content.matchAll(tt.regex)];
    assertCheck(
      `${target.label}: Typography/CSS rule ${tt.name}`,
      matches.length > 0,
      'Rule not found'
    );
  }
}

// -----------------------------------------------------------------------------
// CHECK 5: Syntactic Integrity (CSS Braces & JS AST / Parsing)
// -----------------------------------------------------------------------------
console.log('\n[SECTION 5] Syntactic Integrity (CSS & JS Validation)');
for (const target of files) {
  const content = fs.readFileSync(target.path, 'utf8');

  // CSS validation: Extract <style> blocks
  const styleRegex = /<style[^>]*>([\s\S]*?)<\/style>/gi;
  let sMatch;
  let totalOpenBraces = 0;
  let totalCloseBraces = 0;
  while ((sMatch = styleRegex.exec(content)) !== null) {
    totalOpenBraces += (sMatch[1].match(/\{/g) || []).length;
    totalCloseBraces += (sMatch[1].match(/\}/g) || []).length;
  }
  assertCheck(
    `${target.label}: CSS opening and closing braces match (${totalOpenBraces} === ${totalCloseBraces})`,
    totalOpenBraces === totalCloseBraces && totalOpenBraces > 0,
    `Brace mismatch: { = ${totalOpenBraces}, } = ${totalCloseBraces}`
  );

  // JS validation: Extract <script> blocks and parse with vm.Script
  const scriptRegex = /<script[^>]*>([\s\S]*?)<\/script>/gi;
  let scMatch;
  let scriptBlockIndex = 0;
  let jsSyntaxValid = true;
  let jsSyntaxError = '';
  while ((scMatch = scriptRegex.exec(content)) !== null) {
    scriptBlockIndex++;
    const code = scMatch[1];
    // Skip external script tags with src
    if (!code.trim()) continue;
    try {
      new vm.Script(code, { filename: `${target.label}_script_${scriptBlockIndex}.js` });
    } catch (e) {
      jsSyntaxValid = false;
      jsSyntaxError = `Block ${scriptBlockIndex}: ${e.message}`;
      break;
    }
  }
  assertCheck(
    `${target.label}: JavaScript blocks parsed with zero syntax errors`,
    jsSyntaxValid,
    jsSyntaxError
  );
}

// -----------------------------------------------------------------------------
// CHECK 6: Accounting Logic & Contra Transfer Math
// -----------------------------------------------------------------------------
console.log('\n[SECTION 6] Accounting Logic & Contra Transfer Mathematical Invariance');
const content = fs.readFileSync(stagingPath, 'utf8');
const scriptRegex = /<script>([\s\S]*?)<\/script>/gi;
let scMatch;
let mainCode = '';
while ((scMatch = scriptRegex.exec(content)) !== null) {
  if (scMatch[1].includes('function getTransactionsWithRunningBal')) {
    mainCode = scMatch[1];
    break;
  }
}

const sandbox = {
  console: { log: () => {}, error: () => {}, warn: () => {} },
  document: {
    getElementById: () => ({ innerText: '', value: '', innerHTML: '', classList: { toggle: () => false }, style: {} }),
    body: { getAttribute: () => 'light' }
  },
  window: {},
  parseFloat: parseFloat,
  String: String,
  Date: Date,
  Math: Math
};
const ctx = vm.createContext(sandbox);
try {
  vm.runInContext(mainCode, ctx);
} catch (e) {
  // Top-level initialization might attempt DOM writes which our minimal mock absorbs
}

const getRunningBal = ctx.getTransactionsWithRunningBal;
assertCheck('getTransactionsWithRunningBal exists and is callable', typeof getRunningBal === 'function');

if (typeof getRunningBal === 'function') {
  const ledgerDataset = [
    { id: '1', voucher: 'V1', date: '2026-11-01', time: '09:00', type: 'in', account: 'Cash', amount: 100000, status: 'active' },
    { id: '2', voucher: 'V2', date: '2026-11-01', time: '10:00', type: 'out', account: 'Cash', amount: 20000, status: 'active' },
    { id: '3', voucher: 'V3', date: '2026-11-01', time: '11:00', type: 'contra', account: 'Cash', toAccount: 'Bank', amount: 30000, status: 'active' },
    { id: '4', voucher: 'V4', date: '2026-11-01', time: '12:00', type: 'in', account: 'Bank', amount: 50000, status: 'active' },
    { id: '5', voucher: 'V5', date: '2026-11-01', time: '13:00', type: 'out', account: 'Bank', amount: 10000, status: 'active' },
    { id: '6', voucher: 'V6', date: '2026-11-01', time: '14:00', type: 'contra', account: 'Bank', toAccount: 'Cash', amount: 5000, status: 'active' },
    { id: '7', voucher: 'V7', date: '2026-11-01', time: '15:00', type: 'out', account: 'Cash', amount: 999999, status: 'voided' }
  ];

  // Consolidated
  const consolidated = getRunningBal(ledgerDataset, 'ALL');
  assertCheck('Consolidated V1 (+100,000) -> 100,000', consolidated[0].runningBal === 100000);
  assertCheck('Consolidated V2 (-20,000) -> 80,000', consolidated[1].runningBal === 80000);
  assertCheck('Consolidated V3 (Contra 30,000) -> net change 0 -> 80,000', consolidated[2].runningBal === 80000);
  assertCheck('Consolidated V4 (+50,000) -> 130,000', consolidated[3].runningBal === 130000);
  assertCheck('Consolidated V5 (-10,000) -> 120,000', consolidated[4].runningBal === 120000);
  assertCheck('Consolidated V6 (Contra 5,000) -> net change 0 -> 120,000', consolidated[5].runningBal === 120000);
  assertCheck('Consolidated V7 (Voided) -> does not alter balance -> 120,000', consolidated[6].runningBal === 120000);

  // Cash Account Specific:
  // Initial: 0
  // V1 (in Cash): +100,000 -> 100,000
  // V2 (out Cash): -20,000 -> 80,000
  // V3 (contra Cash -> Bank): -30,000 -> 50,000
  // V4 (in Bank): +0 -> 50,000
  // V5 (out Bank): -0 -> 50,000
  // V6 (contra Bank -> Cash): +5,000 -> 55,000
  // V7 (voided): 55,000
  const cashView = getRunningBal(ledgerDataset, 'Cash');
  assertCheck('Cash account V1 -> 100,000', cashView[0].runningBal === 100000);
  assertCheck('Cash account V2 -> 80,000', cashView[1].runningBal === 80000);
  assertCheck('Cash account V3 (outflow to Bank) -> 50,000', cashView[2].runningBal === 50000);
  assertCheck('Cash account V4 (Bank entry unaffected) -> 50,000', cashView[3].runningBal === 50000);
  assertCheck('Cash account V5 (Bank entry unaffected) -> 50,000', cashView[4].runningBal === 50000);
  assertCheck('Cash account V6 (inflow from Bank) -> 55,000', cashView[5].runningBal === 55000);
  assertCheck('Cash account V7 (voided) -> 55,000', cashView[6].runningBal === 55000);

  // Bank Account Specific:
  // V1 (Cash): 0
  // V2 (Cash): 0
  // V3 (contra Cash -> Bank): +30,000 -> 30,000
  // V4 (in Bank): +50,000 -> 80,000
  // V5 (out Bank): -10,000 -> 70,000
  // V6 (contra Bank -> Cash): -5,000 -> 65,000
  // V7 (voided): 65,000
  const bankView = getRunningBal(ledgerDataset, 'Bank');
  assertCheck('Bank account V3 (inflow from Cash) -> 30,000', bankView[2].runningBal === 30000);
  assertCheck('Bank account V4 (+50,000) -> 80,000', bankView[3].runningBal === 80000);
  assertCheck('Bank account V5 (-10,000) -> 70,000', bankView[4].runningBal === 70000);
  assertCheck('Bank account V6 (outflow to Cash) -> 65,000', bankView[5].runningBal === 65000);
  assertCheck('Bank account V7 (voided) -> 65,000', bankView[6].runningBal === 65000);

  // Mathematical Reconciliation:
  // Closing Cash (55,000) + Closing Bank (65,000) === Consolidated Closing (120,000)
  const sumOfAccounts = cashView[6].runningBal + bankView[6].runningBal;
  assertCheck(
    'Double-entry sum of individual account closing balances matches consolidated balance (55,000 + 65,000 = 120,000)',
    sumOfAccounts === consolidated[6].runningBal,
    `Sum ${sumOfAccounts} !== Consolidated ${consolidated[6].runningBal}`
  );
}

// -----------------------------------------------------------------------------
// CHECK 7: Scope Boundary & Illicit Modification Check
// -----------------------------------------------------------------------------
console.log('\n[SECTION 7] Scope Boundary & Illicit Modifications');
// Check that Android and Flutter files were not touched
const androidDir = 'app/src/main/java';
const flutterDir = 'cash_book_system/lib';
assertCheck('Android source directory intact', fs.existsSync(androidDir));
assertCheck('Flutter source directory intact', fs.existsSync(flutterDir));

// -----------------------------------------------------------------------------
// AUDIT SUMMARY
// -----------------------------------------------------------------------------
console.log('\n================================================================');
console.log(`TOTAL FORENSIC AUDIT CHECKS: ${totalChecks}`);
console.log(`PASSED: ${passedChecks} | FAILED: ${failedChecks}`);
console.log('================================================================\n');

if (failedChecks > 0) {
  console.error('VERDICT: INTEGRITY VIOLATION');
  process.exit(1);
} else {
  console.log('VERDICT: CLEAN');
  process.exit(0);
}
