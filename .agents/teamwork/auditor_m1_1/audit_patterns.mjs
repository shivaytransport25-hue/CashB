import fs from 'node:fs';

const files = [
  'E:/CashBookRelease/app/www/index.html',
  'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html'
];

const patterns = [
  { name: '@keyframes pulseGlow', regex: /@keyframes\s+pulseGlow/g },
  { name: 'filter: blur()', regex: /filter:\s*blur\(/g },
  { name: 'diffuse shadow 0 14px 32px', regex: /0\s+14px\s+32px/g },
  { name: 'diffuse shadow 0 16px 40px', regex: /0\s+16px\s+40px/g },
  { name: 'diffuse shadow 0 24px 48px', regex: /0\s+24px\s+48px/g },
  { name: 'diffuse shadow 0 25px 60px', regex: /0\s+25px\s+60px/g },
  { name: 'neon green glow on sync-dot', regex: /box-shadow:[^;]*var\(--win-green\)/g },
  { name: 'sidebar-new-voucher-btn linear-gradient', regex: /\.sidebar-new-voucher-btn[^}]*linear-gradient/g }
];

let totalViolations = 0;

for (const file of files) {
  console.log(`Auditing file: ${file}`);
  if (!fs.existsSync(file)) {
    console.error(`ERROR: File does not exist: ${file}`);
    totalViolations++;
    continue;
  }
  const content = fs.readFileSync(file, 'utf8');
  for (const p of patterns) {
    const matches = [...content.matchAll(p.regex)];
    if (matches.length > 0) {
      console.error(`  VIOLATION: Found prohibited ${p.name} (${matches.length} matches)`);
      totalViolations += matches.length;
    } else {
      console.log(`  CLEAN: Prohibited ${p.name} -> 0 matches`);
    }
  }
}

if (totalViolations > 0) {
  console.error(`\nFAILED with ${totalViolations} violations`);
  process.exit(1);
} else {
  console.log('\nALL PROHIBITED PATTERNS CLEAN (0 VIOLATIONS)');
  process.exit(0);
}
