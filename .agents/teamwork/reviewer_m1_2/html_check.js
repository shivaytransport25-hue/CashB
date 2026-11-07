const fs = require('fs');

const file = 'E:/CashBookRelease/app/www/index.html';
const content = fs.readFileSync(file, 'utf8');

// Void / self-closing tags in HTML5
const voidTags = new Set([
  'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
  'link', 'meta', 'param', 'source', 'track', 'wbr'
]);

// Strip comments, style, and script tags for structural HTML check
let stripped = content
  .replace(/<!--[\s\S]*?-->/g, '')
  .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '')
  .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '');

const tagRegex = /<\/?([a-zA-Z0-9\-]+)(\s+[^>]*)?\/?>/g;
let match;
const stack = [];
let errorCount = 0;

while ((match = tagRegex.exec(stripped)) !== null) {
  const fullTag = match[0];
  const tagName = match[1].toLowerCase();
  const isClosing = fullTag.startsWith('</');
  const isSelfClosing = fullTag.endsWith('/>') || voidTags.has(tagName);

  if (isClosing) {
    if (voidTags.has(tagName)) {
      console.warn(`Spurious closing tag for void element: </${tagName}>`);
      continue;
    }
    if (stack.length === 0) {
      console.error(`Unexpected closing tag: </${tagName}> at index ${match.index}`);
      errorCount++;
    } else {
      const top = stack.pop();
      if (top.name !== tagName) {
        console.error(`Mismatched closing tag: expected </${top.name}>, got </${tagName}> at index ${match.index} (opened at ${top.index})`);
        errorCount++;
      }
    }
  } else if (!isSelfClosing) {
    stack.push({ name: tagName, index: match.index });
  }
}

if (stack.length > 0) {
  console.warn(`Unclosed tags remaining on stack: ${stack.map(s => s.name).join(', ')}`);
} else {
  console.log(`All non-void HTML tags perfectly balanced! Stack size: 0`);
}
console.log(`Total tag nesting errors: ${errorCount}`);
