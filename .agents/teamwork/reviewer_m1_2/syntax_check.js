const fs = require('fs');
const vm = require('vm');

const targets = [
  'E:/CashBookRelease/app/www/index.html',
  'C:/Users/WIN/AppData/Local/Programs/CashBookPro/www/index.html'
];

for (const file of targets) {
  console.log(`\n========================================`);
  console.log(`Checking syntax for: ${file}`);
  console.log(`========================================`);
  const content = fs.readFileSync(file, 'utf8');

  // Check style blocks
  const styleRegex = /<style\b[^>]*>([\s\S]*?)<\/style>/gi;
  let match;
  let styleIdx = 0;
  while ((match = styleRegex.exec(content)) !== null) {
    styleIdx++;
    const css = match[1];
    let openCount = 0;
    let closeCount = 0;
    let inString = false;
    let stringChar = '';
    let inComment = false;
    for (let i = 0; i < css.length; i++) {
      const ch = css[i];
      const next = css[i + 1];
      if (inComment) {
        if (ch === '*' && next === '/') {
          inComment = false;
          i++;
        }
        continue;
      }
      if (inString) {
        if (ch === '\\') {
          i++;
        } else if (ch === stringChar) {
          inString = false;
        }
        continue;
      }
      if (ch === '/' && next === '*') {
        inComment = true;
        i++;
        continue;
      }
      if (ch === '"' || ch === "'") {
        inString = true;
        stringChar = ch;
        continue;
      }
      if (ch === '{') openCount++;
      if (ch === '}') closeCount++;
    }
    console.log(`Style block ${styleIdx}: { = ${openCount}, } = ${closeCount} (Brace Match: ${openCount === closeCount})`);
  }

  // Check script blocks
  const scriptRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
  let scriptIdx = 0;
  while ((match = scriptRegex.exec(content)) !== null) {
    scriptIdx++;
    const js = match[1].trim();
    if (!js) {
      console.log(`Script block ${scriptIdx}: empty or external src only`);
      continue;
    }
    try {
      new vm.Script(js, { filename: `inline_script_${scriptIdx}.js` });
      console.log(`Script block ${scriptIdx}: JS syntax VALID (${js.length} chars)`);
    } catch (err) {
      console.error(`Script block ${scriptIdx}: JS SYNTAX ERROR at line ${err.stack}`);
    }
  }
}
