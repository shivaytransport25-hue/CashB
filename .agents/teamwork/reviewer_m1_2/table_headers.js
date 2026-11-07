const fs = require('fs');
const content = fs.readFileSync('E:/CashBookRelease/app/www/index.html', 'utf8');

function checkTableHeaders(tbodyId) {
  const tbodyPos = content.indexOf(`id="${tbodyId}"`);
  if (tbodyPos === -1) {
    console.log('Tbody not found:', tbodyId);
    return;
  }
  const before = content.slice(Math.max(0, tbodyPos - 1500), tbodyPos);
  const theadMatch = before.match(/<thead>([\s\S]*?)<\/thead>/i);
  if (theadMatch) {
    const ths = theadMatch[1].match(/<th\b[^>]*>/gi);
    console.log(`\nTable for #${tbodyId}: ${ths ? ths.length : 0} <th> columns`);
    console.log(theadMatch[1].trim());
  } else {
    console.log('No thead found for ' + tbodyId);
  }
}

checkTableHeaders('dashboard-txn-tbody');
checkTableHeaders('daybook-txn-tbody');
