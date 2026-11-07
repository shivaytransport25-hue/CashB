import fs from 'fs';

const stagingPath = 'E:/CashBookRelease/app/www/index.html';
const installedPath = 'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html';

const staging = fs.readFileSync(stagingPath, 'utf8');
const installed = fs.readFileSync(installedPath, 'utf8');

console.log('=== 1. FILE EQUALITY & INTEGRITY ===');
console.log('Byte length match:', staging.length === installed.length, `(${staging.length} chars)`);
console.log('Content match:', staging === installed);

console.log('\n=== 2. TEST-SPECIFIC STRING INJECTION CHECK ===');
const testKeywords = [
  'challenger',
  'empirical',
  'stress_test',
  'sharma@corp.com',
  'urgent',
  '1000000000',
  'usr_google_default'
];

for (const kw of testKeywords) {
  const re = new RegExp(kw, 'gi');
  const matches = [...staging.matchAll(re)];
  console.log(`Keyword "${kw}": ${matches.length} matches`);
  for (const m of matches) {
    const start = Math.max(0, m.index - 40);
    const end = Math.min(staging.length, m.index + 40);
    console.log(`  Context: ...${staging.slice(start, end).replace(/\s+/g, ' ')}...`);
  }
}

console.log('\n=== 3. FACADE / HARDCODED FUNCTION RETURN CHECK ===');
// Check if escapeHtml has hardcoded branches
const escapeHtmlMatch = staging.match(/function escapeHtml[\s\S]*?^    \}/m);
if (escapeHtmlMatch) {
  console.log('escapeHtml implementation:');
  console.log(escapeHtmlMatch[0]);
}

// Check if formatINR has hardcoded branches
const formatINRMatch = staging.match(/function formatINR[\s\S]*?^    \}/m);
if (formatINRMatch) {
  console.log('formatINR implementation:');
  console.log(formatINRMatch[0]);
}

// Check if getTransactionsWithRunningBal has hardcoded branches
const runningBalMatch = staging.match(/function getTransactionsWithRunningBal[\s\S]*?^    \}/m);
if (runningBalMatch) {
  console.log('getTransactionsWithRunningBal implementation:');
  console.log(runningBalMatch[0]);
}
