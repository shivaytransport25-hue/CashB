// WCAG 2.1 Contrast Ratio Calculator

function hexToRgb(hex) {
  let clean = hex.replace('#', '').trim();
  if (clean.length === 3) {
    clean = clean.split('').map(c => c + c).join('');
  }
  const num = parseInt(clean, 16);
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255
  };
}

// Alpha blend fg onto bg
function blend(fgHex, bgHex, alpha) {
  const fg = hexToRgb(fgHex);
  const bg = hexToRgb(bgHex);
  return {
    r: Math.round(fg.r * alpha + bg.r * (1 - alpha)),
    g: Math.round(fg.g * alpha + bg.g * (1 - alpha)),
    b: Math.round(fg.b * alpha + bg.b * (1 - alpha))
  };
}

function sRgbToLinear(c) {
  const v = c / 255;
  return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
}

function relativeLuminance(rgb) {
  const r = sRgbToLinear(rgb.r);
  const g = sRgbToLinear(rgb.g);
  const b = sRgbToLinear(rgb.b);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrastRatio(rgb1, rgb2) {
  const l1 = relativeLuminance(rgb1);
  const l2 = relativeLuminance(rgb2);
  const lighter = Math.max(l1, l2);
  const darker = Math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

function evalContrast(name, fgRgb, bgRgb, targetAA = 4.5, targetAAA = 7.0) {
  const cr = contrastRatio(fgRgb, bgRgb);
  const passAA = cr >= targetAA;
  const passAAA = cr >= targetAAA;
  console.log(`- ${name}: ${cr.toFixed(2)}:1 | AA (>=${targetAA}): ${passAA ? 'PASS' : 'FAIL'} | AAA (>=${targetAAA}): ${passAAA ? 'PASS' : 'FAIL'}`);
  return { name, cr, passAA, passAAA };
}

console.log('====================================================');
console.log('1. LIGHT MODE CONTRAST AUDIT (Office Daylight)');
console.log('====================================================');

const cardLight = hexToRgb('#FFFFFF');
const canvasLight = hexToRgb('#F8FAFC');
const subtleLight = hexToRgb('#F1F5F9');
const raisedLight = hexToRgb('#E2E8F0');

evalContrast('Primary Text (#0F172A) on Card (#FFFFFF)', hexToRgb('#0F172A'), cardLight);
evalContrast('Primary Text (#0F172A) on Canvas (#F8FAFC)', hexToRgb('#0F172A'), canvasLight);
evalContrast('Secondary Text (#334155) on Card (#FFFFFF)', hexToRgb('#334155'), cardLight);
evalContrast('Secondary Text (#334155) on Header/Subtle (#F1F5F9)', hexToRgb('#334155'), subtleLight);
evalContrast('Muted Text (#64748B) on Card (#FFFFFF)', hexToRgb('#64748B'), cardLight);
evalContrast('Form Label (#475569) on Card (#FFFFFF)', hexToRgb('#475569'), cardLight);

console.log('\n--- Light Mode Semantic Badges & Amounts ---');
evalContrast('Credit Text (#047857) on Credit Surface (#ECFDF5)', hexToRgb('#047857'), hexToRgb('#ECFDF5'));
evalContrast('Credit Text (#047857) on White Card (#FFFFFF)', hexToRgb('#047857'), cardLight);
evalContrast('Credit Badge Text (#065F46) on Credit Badge BG (#D1FAE5)', hexToRgb('#065F46'), hexToRgb('#D1FAE5'));

evalContrast('Debit Text (#B91C1C) on Debit Surface (#FEF2F2)', hexToRgb('#B91C1C'), hexToRgb('#FEF2F2'));
evalContrast('Debit Text (#B91C1C) on White Card (#FFFFFF)', hexToRgb('#B91C1C'), cardLight);
evalContrast('Debit Badge Text (#991B1B) on Debit Badge BG (#FECACA)', hexToRgb('#991B1B'), hexToRgb('#FECACA'));

evalContrast('Contra Transfer Text (#1D4ED8) on Transfer Surface (#EFF6FF)', hexToRgb('#1D4ED8'), hexToRgb('#EFF6FF'));
evalContrast('Contra Transfer Text (#1D4ED8) on White Card (#FFFFFF)', hexToRgb('#1D4ED8'), cardLight);
evalContrast('Contra Badge Text (#1E40AF) on Contra Badge BG (#BFDBFE)', hexToRgb('#1E40AF'), hexToRgb('#BFDBFE'));

evalContrast('Wealth Amber Text (#B45309) on White Card (#FFFFFF)', hexToRgb('#B45309'), cardLight);
evalContrast('Wealth Amber Button FG (#FFFFFF) on Solid Amber (#D97706)', hexToRgb('#FFFFFF'), hexToRgb('#D97706'), 3.0, 4.5);

console.log('\n====================================================');
console.log('2. DARK MODE CONTRAST AUDIT (Dark Slate Ledger)');
console.log('====================================================');

const cardDark = hexToRgb('#111A28');
const canvasDark = hexToRgb('#0B111B');
const subtleDark = hexToRgb('#182438');

evalContrast('Primary Text (#F8FAFC) on Dark Card (#111A28)', hexToRgb('#F8FAFC'), cardDark);
evalContrast('Primary Text (#F8FAFC) on Dark Canvas (#0B111B)', hexToRgb('#F8FAFC'), canvasDark);
evalContrast('Secondary Text (#CBD5E1) on Dark Card (#111A28)', hexToRgb('#CBD5E1'), cardDark);
evalContrast('Muted Text (#94A3B8) on Dark Card (#111A28)', hexToRgb('#94A3B8'), cardDark);

console.log('\n--- Dark Mode Semantic Badges & Amounts ---');
// In dark mode: credit surface is rgba(6, 78, 59, 0.30) blended over #111A28
const darkCreditSurface = blend('#064E3B', '#111A28', 0.30);
evalContrast('Credit Text (#22C55E) on Dark Card (#111A28)', hexToRgb('#22C55E'), cardDark);
evalContrast('Credit Text (#22C55E) on Dark Credit Surface', hexToRgb('#22C55E'), darkCreditSurface);
const darkCreditBadgeBg = blend('#22C55E', '#111A28', 0.20);
evalContrast('Credit Badge Text (#4ADE80) on Dark Credit Badge BG', hexToRgb('#4ADE80'), darkCreditBadgeBg);

const darkDebitSurface = blend('#7F1D1D', '#111A28', 0.30);
evalContrast('Debit Text (#F87171) on Dark Card (#111A28)', hexToRgb('#F87171'), cardDark);
evalContrast('Debit Text (#F87171) on Dark Debit Surface', hexToRgb('#F87171'), darkDebitSurface);
const darkDebitBadgeBg = blend('#F87171', '#111A28', 0.20);
evalContrast('Debit Badge Text (#FCA5A5) on Dark Debit Badge BG', hexToRgb('#FCA5A5'), darkDebitBadgeBg);

const darkTransferSurface = blend('#1E3A5F', '#111A28', 0.30);
evalContrast('Contra Transfer Text (#60A5FA) on Dark Card (#111A28)', hexToRgb('#60A5FA'), cardDark);
evalContrast('Contra Transfer Text (#60A5FA) on Dark Transfer Surface', hexToRgb('#60A5FA'), darkTransferSurface);
const darkTransferBadgeBg = blend('#60A5FA', '#111A28', 0.20);
evalContrast('Contra Badge Text (#93C5FD) on Dark Transfer Badge BG', hexToRgb('#93C5FD'), darkTransferBadgeBg);

evalContrast('Wealth Amber Text (#F59E0B) on Dark Card (#111A28)', hexToRgb('#F59E0B'), cardDark);
evalContrast('Wealth Amber Button FG (#0B111B) on Solid Amber (#F59E0B)', hexToRgb('#0B111B'), hexToRgb('#F59E0B'), 3.0, 4.5);
