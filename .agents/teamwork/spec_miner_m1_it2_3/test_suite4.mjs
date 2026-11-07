import fs from 'node:fs';

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function sanitizeTableCell(str) {
  if (!str) return '';
  // If an attacker attempts to inject raw HTML table breakout tags (td, tr, table), neutralize it
  if (/<\/?(?:td|tr|table|tbody|thead)[^>]*>/i.test(str)) {
    return '';
  }
  return escapeHtml(str);
}

const attackVouchers = [
  {
    id: 'tx_xss_script',
    voucher: 'VCH-XSS-01',
    date: '2026-11-06',
    time: '12:00 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: '<script>alert("XSS Party")</script>',
    category: 'Sales',
    desc: '<script>alert("XSS Remark")</script>',
    mode: 'Cash',
    amount: 1000,
    status: 'active'
  },
  {
    id: 'tx_html_img',
    voucher: 'VCH-XSS-02',
    date: '2026-11-06',
    time: '12:05 PM',
    type: 'out',
    account: 'Cash in Hand',
    party: '<img src=x onerror=alert("pwn")>',
    category: 'Office',
    desc: '<b onmouseover=alert("hover")>Bold Narration</b>',
    mode: 'Cash',
    amount: 2000,
    status: 'active'
  },
  {
    id: 'tx_table_breakout',
    voucher: 'VCH-BREAKOUT',
    date: '2026-11-06',
    time: '12:10 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>',
    category: 'Testing',
    desc: '</td><td>Injected Col</td>',
    mode: 'Cash',
    amount: 3000,
    status: 'active'
  },
  {
    id: 'tx_amp_quotes',
    voucher: 'VCH-CHARS',
    date: '2026-11-06',
    time: '12:15 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: 'Sharma & Sons <sharma@corp.com> "VIP" \'Client\'',
    category: 'Sales',
    desc: 'Payment for 100 & 200 units <urgent> & "confidential"',
    mode: 'Cash',
    amount: 4000,
    status: 'active'
  },
  {
    id: 'tx_hindi_unicode',
    voucher: 'VCH-HINDI',
    date: '2026-11-06',
    time: '12:20 PM',
    type: 'in',
    account: 'Cash in Hand',
    party: 'श्री कृष्णा लॉजिस्टिक्स (ट्रांसपोर्ट & वेयरहाउस)',
    category: 'भाड़ा',
    desc: 'भाड़ा भुगतान अग्रिम',
    mode: 'Cash',
    amount: 50000,
    status: 'active'
  }
];

// Build Day Book rows
const rows = attackVouchers.map(t => {
  const isContra = t.type === 'contra';
  const partyCell = isContra ? `...` : (sanitizeTableCell(t.party) || '-');
  const catText = t.category ? escapeHtml(t.category) : '';
  const descText = t.desc ? '<br><small style="color:var(--win-text-muted);">' + escapeHtml(t.desc) + '</small>' : '';
  return `<tr>
    <td>${escapeHtml(t.voucher)}</td>
    <td>${escapeHtml(t.date)}</td>
    <td>${escapeHtml(t.type)}</td>
    <td>${escapeHtml(t.account)}</td>
    <td>${partyCell}</td>
    <td>${catText} ${descText}</td>
    <td>${escapeHtml(t.mode)}</td>
    <td>${t.amount}</td>
    <td>-</td>
    <td>${t.amount}</td>
    <td>actions</td>
  </tr>`;
}).join('');

console.log('daybookHtml includes script:', rows.includes('<script>alert("XSS Party")</script>'));
console.log('daybookHtml includes img onerror:', rows.includes('<img src=x onerror='));
console.log('daybookHtml includes ROGUE ROW:', rows.includes('ROGUE ROW'));
console.log('daybookHtml includes raw sharma email:', rows.includes('<sharma@corp.com>'));
console.log('daybookHtml includes escaped sharma email:', rows.includes('&lt;sharma@corp.com&gt;'));
console.log('daybookHtml includes raw urgent:', rows.includes('<urgent>'));
console.log('daybookHtml includes escaped urgent:', rows.includes('&lt;urgent&gt;'));
console.log('daybookHtml includes Hindi:', rows.includes('श्री कृष्णा लॉजिस्टिक्स'));
