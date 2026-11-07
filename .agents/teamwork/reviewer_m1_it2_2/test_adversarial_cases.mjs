import fs from 'fs';

const html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

// Extract escapeHtml
const match = html.match(/function escapeHtml[\s\S]*?window\.escapeHtml\s*=\s*escapeHtml;/);
if (!match) {
  console.error('escapeHtml not found');
  process.exit(1);
}

// Evaluate in local context
const fn = new Function('const window = {}; ' + match[0] + '; return escapeHtml;')();

const testCases = [
  'Agrawal Transport <transport@gmail.com>',
  'Payment for truck <truck-12>',
  'In transit <transit>',
  'trip <trip-5>',
  'Diesel <tracking>',
  'Regular Party Name',
  '<script>alert(1)</script>',
  '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>'
];

testCases.forEach(tc => {
  const res = fn(tc);
  console.log(`Input:  ${tc}`);
  console.log(`Output: ${JSON.stringify(res)}`);
  console.log('---');
});
