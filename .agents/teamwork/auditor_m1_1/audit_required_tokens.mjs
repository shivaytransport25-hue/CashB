import fs from 'node:fs';

const files = [
  'E:/CashBookRelease/app/www/index.html',
  'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html'
];

const requiredTokens = [
  { name: 'Daylight canvas (#F8FAFC)', regex: /--color-bg-canvas:\s*#F8FAFC/gi },
  { name: 'Dark Slate canvas (#0B111B)', regex: /--color-bg-canvas:\s*#0B111B/gi },
  { name: 'Credit Emerald (#047857)', regex: /--color-credit:\s*#047857/gi },
  { name: 'Debit Crimson (#B91C1C)', regex: /--color-debit:\s*#B91C1C/gi },
  { name: 'Contra Navy (#1D4ED8)', regex: /--color-transfer:\s*#1D4ED8/gi },
  { name: 'Wealth Amber (#D97706)', regex: /--color-accent:\s*#D97706/gi },
  { name: '--win-border-hairline token', regex: /--win-border-hairline/g },
  { name: '--color-border-hairline token', regex: /--color-border-hairline/g },
  { name: 'font-variant-numeric: tabular-nums', regex: /font-variant-numeric:\s*tabular-nums/g },
  { name: 'font-feature-settings: "tnum" 1', regex: /font-feature-settings:\s*"tnum"\s*1/g },
  { name: 'CSS amount-contra class', regex: /\.amount-contra/g },
  { name: 'CSS badge-contra-amount class', regex: /\.badge-contra-amount/g },
  { name: 'CSS contra-route-pill class', regex: /\.contra-route-pill/g },
  { name: 'CSS contra-internal-tag class', regex: /\.contra-internal-tag/g }
];

let allOk = true;

for (const file of files) {
  console.log(`Auditing required tokens in: ${file}`);
  const content = fs.readFileSync(file, 'utf8');
  for (const t of requiredTokens) {
    const matches = [...content.matchAll(t.regex)];
    if (!matches || matches.length === 0) {
      console.error(`  MISSING: ${t.name}`);
      allOk = false;
    } else {
      console.log(`  PRESENT: ${t.name} (${matches.length} occurrences)`);
    }
  }
}

if (!allOk) {
  console.error('\nFAIL: Some required tokens/classes are missing');
  process.exit(1);
} else {
  console.log('\nSUCCESS: All required tokens & classes verified');
  process.exit(0);
}
