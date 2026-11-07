import fs from 'fs';
import vm from 'vm';

const html = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

// 1. Extract CSS and count braces
const styleMatches = html.match(/<style[^>]*>([\s\S]*?)<\/style>/gi) || [];
console.log('Style tags found:', styleMatches.length);

let totalOpenBraces = 0;
let totalCloseBraces = 0;

styleMatches.forEach((styleTag, idx) => {
  const content = styleTag.replace(/<style[^>]*>|<\/style>/gi, '');
  const openCount = (content.match(/\{/g) || []).length;
  const closeCount = (content.match(/\}/g) || []).length;
  console.log(`Style block ${idx}: { count = ${openCount}, } count = ${closeCount}`);
  totalOpenBraces += openCount;
  totalCloseBraces += closeCount;
});

console.log('Total CSS Open Braces:', totalOpenBraces, 'Total Close Braces:', totalCloseBraces);
console.log('CSS Braces Match:', totalOpenBraces === totalCloseBraces);

// 2. Extract JS and validate syntax
const scriptRegex = /<script([\s\S]*?)>([\s\S]*?)<\/script>/gi;
let match;
let scriptIdx = 0;
let jsErrors = 0;

while ((match = scriptRegex.exec(html)) !== null) {
  const openTagAttrs = match[1];
  const innerCode = match[2];
  const srcMatch = openTagAttrs.match(/src=["']([^"']+)["']/i);
  if (srcMatch) {
    console.log(`Script ${scriptIdx}: external -> ${srcMatch[1]}`);
  } else {
    console.log(`Script ${scriptIdx}: INLINE SCRIPT (length: ${innerCode.length} chars)`);
    try {
      new vm.Script(innerCode, { filename: `inline-script-${scriptIdx}.js` });
      console.log(`Script ${scriptIdx}: AST syntax verification PASSED (0 syntax errors)`);
    } catch (err) {
      console.error(`Script ${scriptIdx} SYNTAX ERROR:`, err.message);
      jsErrors++;
    }
  }
  scriptIdx++;
}

console.log('Total JS Syntax Errors:', jsErrors);
