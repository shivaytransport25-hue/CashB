import fs from 'fs';

const html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

const match = html.match(/function formatINR\([\s\S]*?\n    \}/);
if (!match) {
  console.error('formatINR not found');
  process.exit(1);
}

const formatINR = new Function(match[0] + '; return formatINR;')();

const inputs = [
  0,
  -0,
  -0.00001,
  -0.004,
  -0.009,
  -40000,
  40000,
  '1,000.00',
  '-1,000.00',
  '₹ 1,000.00',
  '-₹ 1,000.00',
  NaN,
  undefined,
  null,
  'invalid',
  1000000000,
  -1000000000
];

inputs.forEach(inp => {
  console.log(`Input: ${String(inp).padEnd(15)} => Output: ${formatINR(inp)}`);
});
