import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert';

const STAGING_PATH = 'E:/CashBookRelease/app/www/index.html';
const stagingContent = fs.readFileSync(STAGING_PATH, 'utf8');

// Proposed formatINR
function formatINR(val) {
  const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
  const num = parseFloat(cleanVal) || 0;
  const isNeg = num < 0;
  const absVal = Math.abs(num);
  return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Proposed getTransactionsWithRunningBal
function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
  const sorted = [...filterList].sort((a, b) => (a.date + (a.time || '')).localeCompare(b.date + (b.time || '')));
  let running = 0;
  return sorted.map(t => {
    const cleanAmt = typeof t.amount === 'string' ? t.amount.replace(/,/g, '') : t.amount;
    const amt = Math.abs(parseFloat(cleanAmt) || 0);
    if (t.status !== 'voided') {
      if (selectedAcc === 'ALL') {
        if (t.type === 'in') running += amt;
        else if (t.type === 'out') running -= amt;
      } else {
        if (t.type === 'in' && t.account === selectedAcc) running += amt;
        else if (t.type === 'out' && t.account === selectedAcc) running -= amt;
        else if (t.type === 'contra') {
          if (t.toAccount === selectedAcc) running += amt;
          if (t.account === selectedAcc) running -= amt;
        }
      }
      running = Math.round((running + Number.EPSILON) * 100) / 100;
    }
    return { ...t, runningBal: running };
  });
}

console.log('Testing Suite 2 Assertions from Challenger M1-1:');

// S2.1
const accounts = ['Cash', 'SBI Bank', 'HDFC Bank', 'ICICI Bank', 'Petty Cash'];
const testTxs = [];
for (let i = 0; i < 2500; i++) {
  const acc = accounts[i % accounts.length];
  let toAcc = accounts[(i + 1) % accounts.length];
  const typeRand = i % 10;
  const type = typeRand < 4 ? 'in' : (typeRand < 7 ? 'out' : 'contra');
  const baseAmt = ((i * 37) % 50000) + 0.25;
  const amt = i % 2 === 0 ? baseAmt.toFixed(2) : baseAmt;

  testTxs.push({
    id: `tx_${i}`,
    date: `2026-11-${String((i % 28) + 1).padStart(2, '0')}`,
    time: `${String(i % 24).padStart(2, '0')}:${String(i % 60).padStart(2, '0')}`,
    amount: amt,
    type: type,
    account: acc,
    toAccount: type === 'contra' ? toAcc : undefined,
    status: i % 17 === 0 ? 'voided' : 'active',
  });
}
const res21 = getTransactionsWithRunningBal(testTxs, 'ALL');
assert.strictEqual(res21.length, 2500);
console.log('✔ S2.1 PASSED');

// S2.2
const s22Txs = [
  { date: '2026-11-01', time: '10:00', amount: 150000.50, type: 'in', account: 'Cash' },
  { date: '2026-11-01', time: '10:30', amount: 200000.00, type: 'in', account: 'SBI' },
  { date: '2026-11-01', time: '11:00', amount: 50000.25, type: 'in', account: 'HDFC' },
  { date: '2026-11-02', time: '09:00', amount: 35000.00, type: 'out', account: 'Cash' },
  { date: '2026-11-02', time: '10:00', amount: 75000.75, type: 'out', account: 'SBI' },
  { date: '2026-11-02', time: '11:00', amount: 40000.00, type: 'contra', account: 'Cash', toAccount: 'SBI' },
  { date: '2026-11-02', time: '12:00', amount: 15000.00, type: 'contra', account: 'SBI', toAccount: 'PettyCash' },
  { date: '2026-11-02', time: '13:00', amount: 25000.00, type: 'contra', account: 'HDFC', toAccount: 'Cash' },
  { date: '2026-11-02', time: '14:00', amount: 99999.00, type: 'in', account: 'Cash', status: 'voided' }
];
const s22Accounts = ['Cash', 'SBI', 'HDFC', 'PettyCash'];
const consolidated = getTransactionsWithRunningBal(s22Txs, 'ALL');
const consolidatedClosing = consolidated[consolidated.length - 1].runningBal;
let sumOfAccounts = 0;
s22Accounts.forEach(acc => {
  const accList = getTransactionsWithRunningBal(s22Txs, acc);
  const end = accList.length > 0 ? accList[accList.length - 1].runningBal : 0;
  sumOfAccounts += end;
});
const diffPaise = Math.abs(Math.round(consolidatedClosing * 100) - Math.round(sumOfAccounts * 100));
assert.strictEqual(diffPaise, 0);
assert.strictEqual(formatINR(consolidatedClosing), formatINR(sumOfAccounts));
console.log('✔ S2.2 PASSED');

// S2.3
const extremeTxs = [
  { date: '2026-11-01', amount: 0.01, type: 'in', account: 'Cash' },
  { date: '2026-11-02', amount: 1000000000.00, type: 'in', account: 'Cash' },
  { date: '2026-11-03', amount: 0.01, type: 'out', account: 'Cash' },
  { date: '2026-11-04', amount: 999999999.99, type: 'out', account: 'Cash' },
];
const res23 = getTransactionsWithRunningBal(extremeTxs, 'Cash');
assert.strictEqual(Math.round(res23[0].runningBal * 100), 1);
assert.strictEqual(Math.round(res23[1].runningBal * 100), 100000000001);
assert.strictEqual(Math.round(res23[2].runningBal * 100), 100000000000);
assert.strictEqual(Math.round(res23[3].runningBal * 100), 1);
console.log('✔ S2.3 PASSED');

// S2.4
const overdraftTxs = [
  { date: '2026-11-01', amount: 10000, type: 'in', account: 'SBI' },
  { date: '2026-11-02', amount: 50000, type: 'out', account: 'SBI' }
];
const res24 = getTransactionsWithRunningBal(overdraftTxs, 'SBI');
assert.strictEqual(res24[1].runningBal, -40000);
console.log('✔ S2.4 PASSED');

// S2.5
const cycleTxs = [
  { date: '2026-11-01', time: '10:00', amount: 25000, type: 'contra', account: 'Cash', toAccount: 'SBI' },
  { date: '2026-11-01', time: '11:00', amount: 25000, type: 'contra', account: 'SBI', toAccount: 'HDFC' },
  { date: '2026-11-01', time: '12:00', amount: 25000, type: 'contra', account: 'HDFC', toAccount: 'Cash' },
];
const consCycle = getTransactionsWithRunningBal(cycleTxs, 'ALL');
assert.strictEqual(consCycle[consCycle.length - 1].runningBal, 0);
['Cash', 'SBI', 'HDFC'].forEach(acc => {
  const accRes = getTransactionsWithRunningBal(cycleTxs, acc);
  assert.strictEqual(accRes[accRes.length - 1].runningBal, 0);
});
console.log('✔ S2.5 PASSED');

// S2.6
const voidTxs = [
  { date: '2026-11-01', amount: 10000, type: 'in', account: 'Cash' },
  { date: '2026-11-02', amount: 50000, type: 'in', account: 'Cash', status: 'voided' },
  { date: '2026-11-03', amount: 2000, type: 'out', account: 'Cash' },
  { date: '2026-11-04', amount: 10000, type: 'out', account: 'Cash', status: 'voided' },
  { date: '2026-11-05', amount: 5000, type: 'contra', account: 'Cash', toAccount: 'SBI', status: 'voided' },
];
const res26 = getTransactionsWithRunningBal(voidTxs, 'Cash');
assert.strictEqual(res26[0].runningBal, 10000);
assert.strictEqual(res26[1].runningBal, 10000);
assert.strictEqual(res26[2].runningBal, 8000);
assert.strictEqual(res26[3].runningBal, 8000);
assert.strictEqual(res26[4].runningBal, 8000);
console.log('✔ S2.6 PASSED');

// S2.7
const unsortedTxs = [
  { date: '2026-11-03', time: '14:00', amount: 300, type: 'in', account: 'Cash' },
  { date: '2026-11-01', time: '09:00', amount: 100, type: 'in', account: 'Cash' },
  { date: '2026-11-02', time: '12:00', amount: 200, type: 'in', account: 'Cash' }
];
const res27 = getTransactionsWithRunningBal(unsortedTxs, 'Cash');
assert.strictEqual(res27[0].amount, 100);
assert.strictEqual(res27[0].runningBal, 100);
assert.strictEqual(res27[1].amount, 200);
assert.strictEqual(res27[1].runningBal, 300);
assert.strictEqual(res27[2].amount, 300);
assert.strictEqual(res27[2].runningBal, 600);
console.log('✔ S2.7 PASSED');

// S2.8 (Defect 1)
const floatTxs = [];
for (let i = 0; i < 100; i++) {
  floatTxs.push({ date: '2026-11-01', time: '10:00', amount: 0.10, type: 'in', account: 'Cash' });
}
const res28 = getTransactionsWithRunningBal(floatTxs, 'Cash');
const finalBal = res28[99].runningBal;
assert.strictEqual(finalBal, 10.0, `Expected exact 10.0, got ${finalBal}`);
console.log('✔ S2.8 PASSED (Defect 1 Fixed)');

// S2.9 (Defect 2)
const formattedNeg = formatINR(-40000);
assert.strictEqual(formattedNeg.startsWith('-₹ '), true, `Expected -₹ prefix, got ${formattedNeg}`);
assert.strictEqual(formattedNeg, '-₹ 40,000.00');
console.log('✔ S2.9 PASSED (Defect 2 Fixed)');

// S2.10 (Defect 3)
const commaTx = [{ date: '2026-11-01', amount: '1,000.00', type: 'in', account: 'Cash' }];
const res30 = getTransactionsWithRunningBal(commaTx, 'Cash');
assert.strictEqual(res30[0].runningBal, 1000);
console.log('✔ S2.10 PASSED (Defect 3 Fixed)');

// S2.11 (Defect 4)
const selfTx = [{ date: '2026-11-01', amount: 5000, type: 'contra', account: 'Cash', toAccount: 'Cash' }];
const res31 = getTransactionsWithRunningBal(selfTx, 'Cash');
assert.strictEqual(res31[0].runningBal, 0);
console.log('✔ S2.11 PASSED (Defect 4 Fixed)');

console.log('\nAll Suite 2 tests PASSED 100%!');
