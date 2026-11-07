function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const party = '</td></tr><tr><td colspan="11">ROGUE ROW</td></tr><tr><td>';
const partyCell = escapeHtml(party);
const daybookHtml = `<tr><td>${partyCell}</td></tr>`;

console.log('daybookHtml includes ROGUE ROW:', daybookHtml.includes('ROGUE ROW'));
const rows = daybookHtml.match(/<tr[\s\S]*?<\/tr>/gi) || [];
console.log('rows count:', rows.length);
for (const r of rows) {
  if (r.includes('ROGUE ROW')) {
    console.log('r includes ROGUE ROW!');
  }
}
