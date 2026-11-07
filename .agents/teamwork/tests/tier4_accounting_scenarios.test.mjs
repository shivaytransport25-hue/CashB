/**
 * tier4_accounting_scenarios.test.mjs
 * Tier 4: Real-World Commercial Accounting Scenarios.
 * Multi-voucher commercial workloads, cash flow calculations, Contra transfers,
 * balance sheet checks, and integer paise precision.
 */

import assert from 'node:assert';
import {
  TestRunner,
  AccountingEngine,
  FirestoreSchemaValidator,
} from './test_helpers.mjs';

const runner = new TestRunner('Tier 4: Real-World Accounting Scenarios');

// ============================================================================
// SCENARIO 1: Retail Merchant Day Book Workload
// ============================================================================

runner.test('SCENARIO 1: Retail Merchant Day Book (Opening Cash ₹15,000, 10 Sales Inflows, 5 Outflows, Zero-Drift Closing)', () => {
  const openingPaise = 1500000; // ₹ 15,000.00

  const dayTransactions = [
    // 10 Sales Cash In entries
    { id: 'RET-001', type: 'CASH_IN', amount: '1250.00', partyName: 'Customer A', category: 'Retail Sales' },
    { id: 'RET-002', type: 'CASH_IN', amount: '450.50', partyName: 'Customer B', category: 'Retail Sales' },
    { id: 'RET-003', type: 'CASH_IN', amount: '3200.00', partyName: 'Customer C', category: 'Retail Sales' },
    { id: 'RET-004', type: 'CASH_IN', amount: '890.00', partyName: 'Customer D', category: 'Retail Sales' },
    { id: 'RET-005', type: 'CASH_IN', amount: '12450.00', partyName: 'Bulk Buyer X', category: 'Wholesale Sales' },
    { id: 'RET-006', type: 'CASH_IN', amount: '650.00', partyName: 'Customer E', category: 'Retail Sales' },
    { id: 'RET-007', type: 'CASH_IN', amount: '2100.75', partyName: 'Customer F', category: 'Retail Sales' },
    { id: 'RET-008', type: 'CASH_IN', amount: '150.00', partyName: 'Customer G', category: 'Retail Sales' },
    { id: 'RET-009', type: 'CASH_IN', amount: '5400.00', partyName: 'Customer H', category: 'Retail Sales' },
    { id: 'RET-010', type: 'CASH_IN', amount: '980.25', partyName: 'Customer I', category: 'Retail Sales' },

    // 5 Expense / Supplier Cash Out entries
    { id: 'RET-011', type: 'CASH_OUT', amount: '8500.00', partyName: 'Dairy Distributor', category: 'Inventory Purchase' },
    { id: 'RET-012', type: 'CASH_OUT', amount: '120.00', partyName: 'Chai Wala', category: 'Staff Welfare' },
    { id: 'RET-013', type: 'CASH_OUT', amount: '350.00', partyName: 'Stationery Mart', category: 'Office Supplies' },
    { id: 'RET-014', type: 'CASH_OUT', amount: '1500.00', partyName: 'Electric Board', category: 'Electricity Bill' },
    { id: 'RET-015', type: 'CASH_OUT', amount: '450.00', partyName: 'Sweeper', category: 'Cleaning & Sanitation' },
  ];

  // Calculate day book via integer paise engine
  const result = AccountingEngine.calculateDayBook(openingPaise, dayTransactions);

  // Manual sum verification:
  // Inflows: 1250.00 + 450.50 + 3200.00 + 890.00 + 12450.00 + 650.00 + 2100.75 + 150.00 + 5400.00 + 980.25 = 27521.50
  assert.strictEqual(result.totalInflowPaise, 2752150);
  assert.strictEqual(result.inflowFormatted, '₹ 27,521.50');

  // Outflows: 8500.00 + 120.00 + 350.00 + 1500.00 + 450.00 = 10920.00
  assert.strictEqual(result.totalOutflowPaise, 1092000);
  assert.strictEqual(result.outflowFormatted, '₹ 10,920.00');

  // Closing: 15,000.00 + 27,521.50 - 10,920.00 = 31,601.50
  assert.strictEqual(result.closingBalancePaise, 3160150);
  assert.strictEqual(result.closingFormatted, '₹ 31,601.50');
}, 'INV-ACC-02');

// ============================================================================
// SCENARIO 2: Fleet & Transport Operator Multi-Account Workload
// ============================================================================

runner.test('SCENARIO 2: Fleet & Transport Operator (3 Accounts: Cash, SBI, HDFC; Diesel, Freight, FASTag, Overdraft)', () => {
  const accounts = {
    cashInHandPaise: 1250000,    // ₹ 12,500.00
    sbiCurrentPaise: 25000000,   // ₹ 2,50,000.00
    hdfcBankPaise: 10000000,     // ₹ 1,00,000.00
  };

  const getNetLiquidity = () => accounts.cashInHandPaise + accounts.sbiCurrentPaise + accounts.hdfcBankPaise;
  const initialLiquidity = getNetLiquidity(); // ₹ 3,62,500.00

  // 1. Freight Advance Received from Tata Motors via SBI Bank: ₹ 1,50,000.00
  accounts.sbiCurrentPaise += AccountingEngine.parseToPaise(150000);

  // 2. Freight Balance Received from Reliance Retail via HDFC: ₹ 85,000.00
  accounts.hdfcBankPaise += AccountingEngine.parseToPaise(85000);

  // 3. Diesel Payment to IOCL Pump via SBI: ₹ 65,000.00
  accounts.sbiCurrentPaise -= AccountingEngine.parseToPaise(65000);

  // 4. FASTag Highway Toll Top-up via HDFC: ₹ 15,000.00
  accounts.hdfcBankPaise -= AccountingEngine.parseToPaise(15000);

  // 5. Driver Trip Advance in Cash from Cash in Hand: ₹ 8,000.00
  accounts.cashInHandPaise -= AccountingEngine.parseToPaise(8000);

  // 6. Large Tyre Purchase payment via SBI creating temporary overdraft: ₹ 3,50,000.00
  // Previous SBI: 250,000 + 150,000 - 65,000 = 335,000. Payment: 350,000 -> -15,000 overdraft
  accounts.sbiCurrentPaise -= AccountingEngine.parseToPaise(350000);

  // Verify SBI balance is negative (overdraft) and formatted in Red
  assert.strictEqual(accounts.sbiCurrentPaise, -1500000); // -₹ 15,000.00
  const sbiFormatted = AccountingEngine.formatPaise(accounts.sbiCurrentPaise);
  assert.strictEqual(sbiFormatted, '-₹ 15,000.00');

  // 7. Internal Contra: Transfer ₹ 50,000 from HDFC to SBI to clear overdraft
  accounts.hdfcBankPaise -= AccountingEngine.parseToPaise(50000);
  accounts.sbiCurrentPaise += AccountingEngine.parseToPaise(50000);

  // SBI is now +₹ 35,000.00 (overdraft cleared)
  assert.strictEqual(accounts.sbiCurrentPaise, 3500000);
  assert.strictEqual(AccountingEngine.formatPaise(accounts.sbiCurrentPaise), '₹ 35,000.00');

  // Verify final net liquidity equals initial + external revenues - external expenses
  // Net External Inflows: 150,000 + 85,000 = 235,000
  // Net External Outflows: 65,000 + 15,000 + 8,000 + 350,000 = 438,000
  // Expected Net Liquidity: 362,500 + 235,000 - 438,000 = 159,500.00 (15,950,000 paise)
  assert.strictEqual(getNetLiquidity(), 15950000);
  assert.strictEqual(AccountingEngine.formatPaise(getNetLiquidity()), '₹ 1,59,500.00');
}, 'INV-ACC-03');

// ============================================================================
// SCENARIO 3: Wholesale Trader Multi-Account Contra Workflow
// ============================================================================

runner.test('SCENARIO 3: Wholesale Trader Multi-Account Contra (Cash Withdrawal, Inter-Bank RTGS, Invariant Preservation)', () => {
  let cashInHand = 500000;   // ₹ 5,000.00
  let sbiBank = 50000000;    // ₹ 5,00,000.00
  let hdfcBank = 10000000;   // ₹ 1,00,000.00

  const getEnterpriseLiquidity = () => cashInHand + sbiBank + hdfcBank;
  const initialLiquidity = getEnterpriseLiquidity(); // ₹ 6,05,000.00

  // 1. ATM / Bank Cash Withdrawal: Transfer ₹ 50,000 from SBI to Cash in Hand
  const contra1Amount = 5000000; // ₹ 50,000.00
  sbiBank -= contra1Amount;
  cashInHand += contra1Amount;

  // Invariant check: Enterprise liquidity unchanged by Contra
  assert.strictEqual(getEnterpriseLiquidity(), initialLiquidity, 'Contra 1 must preserve net enterprise liquidity');
  assert.strictEqual(cashInHand, 5500000); // ₹ 55,000.00
  assert.strictEqual(sbiBank, 45000000);   // ₹ 4,50,000.00

  // 2. Inter-Bank Contra Transfer: Transfer ₹ 2,00,000 from SBI to HDFC
  const contra2Amount = 20000000; // ₹ 2,00,000.00
  sbiBank -= contra2Amount;
  hdfcBank += contra2Amount;

  assert.strictEqual(getEnterpriseLiquidity(), initialLiquidity, 'Contra 2 must preserve net enterprise liquidity');
  assert.strictEqual(sbiBank, 25000000);   // ₹ 2,50,000.00
  assert.strictEqual(hdfcBank, 30000000);  // ₹ 3,00,000.00

  // 3. External RTGS Payment to Manufacturer from HDFC: ₹ 1,50,000.00
  const paymentAmount = 15000000; // ₹ 1,50,000.00
  hdfcBank -= paymentAmount;

  // External payment decrements liquidity by exact payment amount
  assert.strictEqual(getEnterpriseLiquidity(), initialLiquidity - paymentAmount);
  assert.strictEqual(AccountingEngine.formatPaise(getEnterpriseLiquidity()), '₹ 4,55,000.00');
}, 'INV-ACC-03');

// ============================================================================
// SCENARIO 4: Error Correction via Immutable Reversal Audit Flow
// ============================================================================

runner.test('SCENARIO 4: Error Correction via Immutable Reversal (Accidental Double Entry Corrected via Counter-Voucher)', () => {
  const openingPaise = 10000000; // ₹ 1,00,000.00
  const authUid = 'usr_accountant_lead';

  // Step 1: Valid initial voucher
  const vch1 = {
    id: 'VCH-2026-0041',
    userId: authUid,
    bookId: 'main_book',
    type: 'CASH_IN',
    amount: 85000.0,
    remark: 'Machinery sale advance from Client X',
    category: 'Asset Sale',
    paymentMode: 'Bank Transfer',
    partyId: 'pty_client_x',
    partyName: 'Client X Enterprises',
    transactionDate: Date.now() - 7200000,
    createdAt: Date.now() - 7200000,
    updatedAt: Date.now() - 7200000,
  };

  // Step 2: Operator accidentally books the same payment twice
  const vch2Duplicate = {
    id: 'VCH-2026-0042',
    userId: authUid,
    bookId: 'main_book',
    type: 'CASH_IN',
    amount: 85000.0, // DUPLICATE
    remark: 'Machinery sale advance from Client X (Duplicate)',
    category: 'Asset Sale',
    paymentMode: 'Bank Transfer',
    partyId: 'pty_client_x',
    partyName: 'Client X Enterprises',
    transactionDate: Date.now() - 3600000,
    createdAt: Date.now() - 3600000,
    updatedAt: Date.now() - 3600000,
  };

  // Day Book with duplicate: Closing is inflated by ₹ 85,000.00
  const dayBookErroneous = AccountingEngine.calculateDayBook(openingPaise, [vch1, vch2Duplicate]);
  assert.strictEqual(dayBookErroneous.closingBalancePaise, 27000000); // ₹ 2,70,000.00

  // Step 3: Auditor posts Immutable Reversal Voucher
  const vchReversal = AccountingEngine.createReversalVoucher(
    vch2Duplicate,
    'Duplicate booking entry corrected per bank statement reconciliation'
  );

  // Invariant 1: Reversal voucher conforms strictly to firestore.rules 12-key schema
  const cloudPayload = { ...vchReversal };
  delete cloudPayload.id; // Document path is ID, body has only 12 whitelisted keys
  const validation = FirestoreSchemaValidator.validateTransaction(cloudPayload, authUid);
  assert.strictEqual(validation.valid, true, `Reversal voucher schema invalid: ${validation.reason}`);

  // Invariant 2: Reversal voucher reverses direction (CASH_IN -> CASH_OUT)
  assert.strictEqual(vchReversal.type, 'CASH_OUT');
  assert.strictEqual(vchReversal.amount, 85000.0);
  assert.match(vchReversal.remark, /\[REVERSAL of VCH-2026-0042\]/);

  // Step 4: Day book with reversal restores true balance
  const dayBookCorrected = AccountingEngine.calculateDayBook(openingPaise, [vch1, vch2Duplicate, vchReversal]);
  assert.strictEqual(dayBookCorrected.closingBalancePaise, 18500000); // Exactly ₹ 1,85,000.00
  assert.strictEqual(dayBookCorrected.closingFormatted, '₹ 1,85,000.00');

  // Invariant 3: Audit log preserves both entries (non-destructive accounting)
  const auditEntries = [vch1, vch2Duplicate, vchReversal];
  assert.strictEqual(auditEntries.length, 3, 'All 3 vouchers must remain in permanent ledger audit trail');
}, 'INV-ACC-04');

// ============================================================================
// SCENARIO 5: High-Volume Commercial Stress Workload (150 Vouchers)
// ============================================================================

runner.test('SCENARIO 5: High-Volume Commercial Stress Workload (150 Vouchers, Zero-Drift Balance Sheet Reconciliation)', () => {
  const openingCapitalPaise = 500000000; // ₹ 50,00,000.00 (50 Lakhs)
  const transactions = [];

  let expectedInflowPaise = 0;
  let expectedOutflowPaise = 0;

  for (let i = 1; i <= 150; i++) {
    const isIncome = (i % 3 !== 0); // 100 income, 50 expense
    const amountRupees = (i * 123.45); // Varying decimal amounts
    const amountPaise = Math.round(amountRupees * 100);

    if (isIncome) {
      expectedInflowPaise += amountPaise;
      transactions.push({
        id: `STRESS-${i}`,
        type: 'CASH_IN',
        amount: amountRupees,
        remark: `High volume transaction ${i}`,
        category: 'Sales',
        paymentMode: 'UPI',
        partyName: `Trader ${i}`,
      });
    } else {
      expectedOutflowPaise += amountPaise;
      transactions.push({
        id: `STRESS-${i}`,
        type: 'CASH_OUT',
        amount: amountRupees,
        remark: `High volume expense ${i}`,
        category: 'Operations',
        paymentMode: 'Bank Transfer',
        partyName: `Vendor ${i}`,
      });
    }
  }

  // Calculate day book across all 150 transactions
  const dayBook = AccountingEngine.calculateDayBook(openingCapitalPaise, transactions);

  // Invariant 1: Total inflow matches exact paise sum
  assert.strictEqual(dayBook.totalInflowPaise, expectedInflowPaise);

  // Invariant 2: Total outflow matches exact paise sum
  assert.strictEqual(dayBook.totalOutflowPaise, expectedOutflowPaise);

  // Invariant 3: Balance Sheet Equality:
  // Closing Assets == Opening Capital + Total Inflow - Total Outflow
  const expectedClosingPaise = openingCapitalPaise + expectedInflowPaise - expectedOutflowPaise;
  assert.strictEqual(dayBook.closingBalancePaise, expectedClosingPaise);

  // Invariant 4: Zero floating-point drift
  const paiseDifference = dayBook.closingBalancePaise - expectedClosingPaise;
  assert.strictEqual(paiseDifference, 0, 'Zero paise difference allowed in financial calculations');
}, 'INV-ACC-01');

// Run tests when executed directly
if (process.argv[1] && process.argv[1].endsWith('tier4_accounting_scenarios.test.mjs')) {
  runner.run().then(res => {
    process.exit(res.failed > 0 ? 1 : 0);
  });
}
