const fs = require('fs');

const files = [
  'E:/CashBookRelease/app/www/index.html',
  'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html'
];

let allOk = true;

for (const f of files) {
  console.log('Testing file:', f);
  const content = fs.readFileSync(f, 'utf8');

  // Check prohibited
  const prohibited = [
    { name: 'pulseGlow', pattern: /pulseGlow/g },
    { name: 'filter: blur', pattern: /filter:\s*blur/g },
    { name: 'diffuse shadow 0 14px 32px', pattern: /0\s+14px\s+32px/g },
    { name: 'diffuse shadow 0 16px 40px', pattern: /0\s+16px\s+40px/g },
    { name: 'diffuse shadow 0 24px 48px', pattern: /0\s+24px\s+48px/g },
    { name: 'diffuse shadow 0 25px 60px', pattern: /0\s+25px\s+60px/g },
    { name: 'neon green glow', pattern: /box-shadow:[^;]*var\(--win-green\)/g },
    { name: 'linear-gradient on voucher btn', pattern: /\.sidebar-new-voucher-btn[^}]*linear-gradient/g }
  ];

  for (const p of prohibited) {
    const matches = content.match(p.pattern);
    if (matches && matches.length > 0) {
      console.error('  FAIL: Found prohibited ' + p.name + ' (' + matches.length + ' occurrences)');
      allOk = false;
    } else {
      console.log('  PASS: ' + p.name + ' = 0 matches');
    }
  }

  // Check required
  const required = [
    { name: 'font-variant-numeric: tabular-nums', pattern: /font-variant-numeric:\s*tabular-nums/g },
    { name: 'font-feature-settings: "tnum" 1', pattern: /font-feature-settings:\s*"tnum"\s*1/g },
    { name: '--color-border-hairline', pattern: /--color-border-hairline/g },
    { name: '--win-border-hairline', pattern: /--win-border-hairline/g },
    { name: 'amount-contra', pattern: /amount-contra/g },
    { name: 'badge-contra-amount', pattern: /badge-contra-amount/g },
    { name: 'contra-route-pill', pattern: /contra-route-pill/g },
    { name: 'Daylight canvas token (#F8FAFC)', pattern: /--color-bg-canvas:\s*#F8FAFC/g },
    { name: 'Dark Slate canvas token (#0B111B)', pattern: /--color-bg-canvas:\s*#0B111B/g },
    { name: 'Credit Emerald token (#047857)', pattern: /--color-credit:\s*#047857/g },
    { name: 'Debit Crimson token (#B91C1C)', pattern: /--color-debit:\s*#B91C1C/g },
    { name: 'Contra Navy token (#1D4ED8)', pattern: /--color-transfer:\s*#1D4ED8/g },
    { name: 'Wealth Amber token (#D97706)', pattern: /--color-accent:\s*#D97706/g }
  ];

  for (const r of required) {
    const matches = content.match(r.pattern);
    if (!matches || matches.length === 0) {
      console.error('  FAIL: Missing required ' + r.name);
      allOk = false;
    } else {
      console.log('  PASS: ' + r.name + ' (' + matches.length + ' occurrences)');
    }
  }

  console.log('Done testing ' + f + '\n');
}

if (!allOk) {
  console.error('VERIFICATION FAILED!');
  process.exit(1);
} else {
  console.log('ALL VERIFICATIONS PASSED SUCCESSFULLY!');
}
