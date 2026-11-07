import fs from 'node:fs';
import assert from 'node:assert';

const STAGING_PATH = 'E:/CashBookRelease/app/www/index.html';
let content = fs.readFileSync(STAGING_PATH, 'utf8');

// Patch 1: formatINR
const targetFormatINR = `    function formatINR(val) {
      const num = parseFloat(val) || 0;
      return '₹ ' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }`;

const replaceFormatINR = `    function formatINR(val) {
      const cleanVal = typeof val === 'string' ? val.replace(/,/g, '') : val;
      const num = parseFloat(cleanVal) || 0;
      const isNeg = num < 0;
      const absVal = Math.abs(num);
      return (isNeg ? '-₹ ' : '₹ ') + absVal.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }`;

// Patch 2: getTransactionsWithRunningBal
const targetGetTransactionsWithRunningBal = `    function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
      const sorted = [...filterList].sort((a, b) => (a.date + (a.time || '')).localeCompare(b.date + (b.time || '')));
      let running = 0;
      return sorted.map(t => {
        const amt = parseFloat(t.amount) || 0;
        if (t.status !== 'voided') {
          if (selectedAcc === 'ALL') {
            // Consolidated Cash Book: Inflow adds, Outflow subtracts, Contra has net 0 cash impact
            if (t.type === 'in') running += amt;
            else if (t.type === 'out') running -= amt;
          } else {
            // Account Specific Ledger:
            if (t.type === 'in' && t.account === selectedAcc) running += amt;
            else if (t.type === 'out' && t.account === selectedAcc) running -= amt;
            else if (t.type === 'contra') {
              if (t.toAccount === selectedAcc) running += amt; // Inflow to destination
              else if (t.account === selectedAcc) running -= amt; // Outflow from source
            }
          }
        }
        return { ...t, runningBal: running };
      });
    }`;

const replaceGetTransactionsWithRunningBal = `    function getTransactionsWithRunningBal(filterList, selectedAcc = 'ALL') {
      const sorted = [...filterList].sort((a, b) => (a.date + (a.time || '')).localeCompare(b.date + (b.time || '')));
      let running = 0;
      return sorted.map(t => {
        const cleanAmt = typeof t.amount === 'string' ? t.amount.replace(/,/g, '') : t.amount;
        const amt = Math.abs(parseFloat(cleanAmt) || 0);
        if (t.status !== 'voided') {
          if (selectedAcc === 'ALL') {
            // Consolidated Cash Book: Inflow adds, Outflow subtracts, Contra has net 0 cash impact
            if (t.type === 'in') running += amt;
            else if (t.type === 'out') running -= amt;
          } else {
            // Account Specific Ledger:
            if (t.type === 'in' && t.account === selectedAcc) running += amt;
            else if (t.type === 'out' && t.account === selectedAcc) running -= amt;
            else if (t.type === 'contra') {
              if (t.toAccount === selectedAcc) running += amt; // Inflow to destination
              if (t.account === selectedAcc) running -= amt; // Outflow from source
            }
          }
          running = Math.round((running + Number.EPSILON) * 100) / 100;
        }
        return { ...t, runningBal: running };
      });
    }`;

// Patch 3: applyFilters totals & Contra
const targetApplyFiltersTotals = `      let totIn = 0, totOut = 0;
      filtered.forEach(t => {
        if (t.status === 'voided') return;
        const amt = parseFloat(t.amount) || 0;
        if (acc === 'ALL') {
          if (t.type === 'in') totIn += amt;
          else if (t.type === 'out') totOut += amt;
        } else {
          // Account specific filter totals
          if (t.type === 'in' && t.account === acc) totIn += amt;
          else if (t.type === 'out' && t.account === acc) totOut += amt;
          else if (t.type === 'contra') {
            if (t.toAccount === acc) totIn += amt; // Inflow to destination
            else if (t.account === acc) totOut += amt; // Outflow from source
          }
        }
      });`;

const replaceApplyFiltersTotals = `      let totIn = 0, totOut = 0;
      filtered.forEach(t => {
        if (t.status === 'voided') return;
        const cleanAmt = typeof t.amount === 'string' ? t.amount.replace(/,/g, '') : t.amount;
        const amt = Math.abs(parseFloat(cleanAmt) || 0);
        if (acc === 'ALL') {
          if (t.type === 'in') totIn += amt;
          else if (t.type === 'out') totOut += amt;
        } else {
          // Account specific filter totals
          if (t.type === 'in' && t.account === acc) totIn += amt;
          else if (t.type === 'out' && t.account === acc) totOut += amt;
          else if (t.type === 'contra') {
            if (t.toAccount === acc) totIn += amt; // Inflow to destination
            if (t.account === acc) totOut += amt; // Outflow from source
          }
        }
      });
      totIn = Math.round((totIn + Number.EPSILON) * 100) / 100;
      totOut = Math.round((totOut + Number.EPSILON) * 100) / 100;`;

// Patch 4: applyFilters row rendering
const targetApplyFiltersRowAmt = `      tbody.innerHTML = [...withBal].reverse().map(t => {
        const amt = parseFloat(t.amount) || 0;`;

const replaceApplyFiltersRowAmt = `      tbody.innerHTML = [...withBal].reverse().map(t => {
        const cleanAmt = typeof t.amount === 'string' ? t.amount.replace(/,/g, '') : t.amount;
        const amt = Math.abs(parseFloat(cleanAmt) || 0);`;

// Patch 5: renderDashboardTable row rendering
const targetDashboardRowAmt = `      tbody.innerHTML = recent.map(t => {
        const amt = parseFloat(t.amount) || 0;`;

const replaceDashboardRowAmt = `      tbody.innerHTML = recent.map(t => {
        const cleanAmt = typeof t.amount === 'string' ? t.amount.replace(/,/g, '') : t.amount;
        const amt = Math.abs(parseFloat(cleanAmt) || 0);`;

assert.ok(content.includes(targetFormatINR), 'targetFormatINR present');
assert.ok(content.includes(targetGetTransactionsWithRunningBal), 'targetGetTransactionsWithRunningBal present');
assert.ok(content.includes(targetApplyFiltersTotals), 'targetApplyFiltersTotals present');
assert.ok(content.includes(targetApplyFiltersRowAmt), 'targetApplyFiltersRowAmt present');
assert.ok(content.includes(targetDashboardRowAmt), 'targetDashboardRowAmt present');

console.log('All 5 target code blocks strictly match index.html content!');
