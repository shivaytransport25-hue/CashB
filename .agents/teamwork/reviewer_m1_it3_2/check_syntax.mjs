import fs from 'fs';
import vm from 'vm';

const html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');
const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
let match;
let count = 0;
while ((match = scriptRegex.exec(html)) !== null) {
  count++;
  const code = match[1];
  if (!code.trim()) continue;
  try {
    new vm.Script(code);
    console.log(`Script block ${count} parsed successfully (${code.length} chars)`);
  } catch (e) {
    console.error(`Syntax error in script block ${count}:`, e.message);
    process.exit(1);
  }
}
console.log(`All ${count} script blocks in index.html are syntactically valid!`);
