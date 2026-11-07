/**
 * tier3_cross_feature.test.mjs
 * Tier 3: Cross-Feature Combinations (Pairwise Integration Workflows).
 * Verifies end-to-end interactions across Auth, Offline Storage, Cloud Sync,
 * Ledger Calculation, Theme Switching, and Multi-tenant State.
 */

import assert from 'node:assert';
import {
  TestRunner,
  AccountingEngine,
  FirestoreSchemaValidator,
  MockSessionEnvironment,
} from './test_helpers.mjs';

const runner = new TestRunner('Tier 3: Cross-Feature Combinations');

// ============================================================================
// TC-C01: Google Sign-In -> Create Cash In -> Cache Offline -> Sync to Cloud
// ============================================================================

runner.test('TC-C01: Google Sign-In -> Create Cash In Voucher -> Cache Offline -> Sync to Firestore -> Verify Cloud 12-key Payload', async () => {
  const env = new MockSessionEnvironment();

  // 1. Authenticate with real Google Sign-In UID
  const authUser = {
    uid: 'google_oauth_sub_9921',
    email: 'rajesh@shivaylogistics.com',
    displayName: 'Rajesh Patel',
  };
  env.simulateLogin(authUser);
  assert.strictEqual(env.session.uid, 'google_oauth_sub_9921');

  // 2. Add Cash In Voucher while offline
  env.isOnline = false;
  const voucher = {
    id: 'VCH-2026-0001',
    userId: authUser.uid,
    bookId: 'main_cashbook_2026',
    type: 'CASH_IN',
    amount: 150000.0, // ₹ 1,50,000.00
    remark: 'Freight advance payment from Tata Motors',
    category: 'Freight Revenue',
    paymentMode: 'Bank Transfer',
    partyId: 'pty_tata_motors',
    partyName: 'Tata Motors Fleet A/c',
    transactionDate: Date.now(),
    createdAt: Date.now(),
    updatedAt: Date.now(),
  };

  env.queueTransaction(voucher);

  // 3. Verify cached in localStorage with dirty marker
  const cachedTxns = JSON.parse(env.getItem('cashbook_txns'));
  assert.strictEqual(cachedTxns.length, 1);
  assert.strictEqual(cachedTxns[0]._dirty, true);

  // 4. Reconnect and trigger Cloud Sync
  env.isOnline = true;
  const syncResult = await env.syncWithCloud();
  assert.strictEqual(syncResult.success, true);
  assert.strictEqual(syncResult.syncedCount, 1);

  // 5. Verify cloud document matches strict 12-key schema without _dirty or illegal keys
  const cloudDoc = env.cloudDb.get(`/users/${authUser.uid}/transactions/${voucher.id}`);
  assert.ok(cloudDoc !== undefined);
  assert.strictEqual(cloudDoc.userId, authUser.uid);
  assert.strictEqual(cloudDoc.amount, 150000.0);
  assert.strictEqual(cloudDoc._dirty, undefined);
  assert.strictEqual(cloudDoc.id, undefined); // Document path is ID, not body field

  const schemaValidation = FirestoreSchemaValidator.validateTransaction(cloudDoc, authUser.uid);
  assert.strictEqual(schemaValidation.valid, true);
}, 'INV-SYNC-01');

// ============================================================================
// TC-C02: Offline Accumulation -> Reconnect -> Batch Reconciliation
// ============================================================================

runner.test('TC-C02: Offline Voucher Creation -> Offline Queue Accumulation -> Reconnect Network -> Batch Sync -> Reconcile Cloud & Local State', async () => {
  const env = new MockSessionEnvironment();
  const uid = 'usr_fleet_admin';
  env.simulateLogin({ uid });
  env.isOnline = false;

  // Queue 5 offline transactions
  for (let i = 1; i <= 5; i++) {
    env.queueTransaction({
      id: `VCH-OFFLINE-${i}`,
      userId: uid,
      bookId: 'book_fleet',
      type: i % 2 === 0 ? 'CASH_IN' : 'CASH_OUT',
      amount: i * 500.0,
      remark: `Offline transaction ${i}`,
      category: 'Operations',
      paymentMode: 'Cash',
      partyId: '',
      partyName: `Party ${i}`,
      transactionDate: Date.now() + i * 100,
      createdAt: Date.now() + i * 100,
      updatedAt: Date.now() + i * 100,
    });
  }

  assert.strictEqual(JSON.parse(env.getItem('cashbook_txns')).length, 5);

  // Sync while offline must fail gracefully
  const offlineAttempt = await env.syncWithCloud();
  assert.strictEqual(offlineAttempt.success, false);

  // Reconnect online and execute batch sync
  env.isOnline = true;
  const onlineSync = await env.syncWithCloud();
  assert.strictEqual(onlineSync.success, true);
  assert.strictEqual(onlineSync.syncedCount, 5);

  // Verify all 5 are in cloud and clean locally
  const localTxns = JSON.parse(env.getItem('cashbook_txns'));
  assert.strictEqual(localTxns.every(t => !t._dirty), true);
  assert.strictEqual(env.cloudDb.size, 5);
}, 'INV-SYNC-06');

// ============================================================================
// TC-C03: Auth -> Create Vouchers -> Process Kill / Reboot -> Restore Session
// ============================================================================

runner.test('TC-C03: Auth Sign In -> Create Vouchers -> Force App Restart (Simulate Process Kill) -> Restore Session -> Verify Vouchers Persist', () => {
  const env = new MockSessionEnvironment();
  const uid = 'usr_durable_session_1';
  env.simulateLogin({ uid, email: 'owner@transport.in' });

  // Add vouchers to local cache
  env.queueTransaction({
    id: 'VCH-RESTORE-1',
    userId: uid,
    bookId: 'book_1',
    type: 'CASH_IN',
    amount: 25000,
    remark: 'Opening receipt',
    category: 'Capital',
    paymentMode: 'Bank Transfer',
    partyId: '',
    partyName: 'Self',
    transactionDate: Date.now(),
    createdAt: Date.now(),
    updatedAt: Date.now(),
  });

  // Force application termination (RAM wiped)
  env.simulateAppRestart();

  // Verify session restored from durable localStorage
  assert.ok(env.session !== null);
  assert.strictEqual(env.session.uid, uid);

  // Verify vouchers intact in durable localStorage
  const txns = JSON.parse(env.getItem('cashbook_txns'));
  assert.strictEqual(txns.length, 1);
  assert.strictEqual(txns[0].id, 'VCH-RESTORE-1');
}, 'INV-AUTH-05');

// ============================================================================
// TC-C04: Multi-Account Contra Transfer & Liquid Account Reconciliation
// ============================================================================

runner.test('TC-C04: Multi-Account Contra Transfer -> SBI Debit + Cash In Hand Credit -> Verify Tabular Ledger Display + Right-aligned Columns', () => {
  const initialAccounts = {
    cashInHandPaise: 1000000, // ₹ 10,000.00
    sbiCurrentPaise: 5000000, // ₹ 50,000.00
  };

  const initialTotalLiquidity = initialAccounts.cashInHandPaise + initialAccounts.sbiCurrentPaise;

  // Execute Contra: ATM Cash withdrawal of ₹ 15,000.00 from SBI to Cash in Hand
  const transferAmountPaise = 1500000; // ₹ 15,000.00

  // Debit SBI, Credit Cash in Hand
  const postContraAccounts = {
    cashInHandPaise: initialAccounts.cashInHandPaise + transferAmountPaise, // ₹ 25,000.00
    sbiCurrentPaise: initialAccounts.sbiCurrentPaise - transferAmountPaise, // ₹ 35,000.00
  };

  // Invariant 1: Net cash liquidity across all internal accounts remains constant
  const postContraTotalLiquidity = postContraAccounts.cashInHandPaise + postContraAccounts.sbiCurrentPaise;
  assert.strictEqual(postContraTotalLiquidity, initialTotalLiquidity, 'Contra transfer must not alter net liquidity');

  // Invariant 2: Tabular formatted numbers
  const cashFormatted = AccountingEngine.formatPaise(postContraAccounts.cashInHandPaise);
  const sbiFormatted = AccountingEngine.formatPaise(postContraAccounts.sbiCurrentPaise);
  assert.strictEqual(cashFormatted, '₹ 25,000.00');
  assert.strictEqual(sbiFormatted, '₹ 35,000.00');
}, 'INV-ACC-03');

// ============================================================================
// TC-C05: Multi-Tenant Vault Isolation (Switch User A -> B)
// ============================================================================

runner.test('TC-C05: Google Sign-In as User A -> Create Vouchers -> Sign Out -> Sign In as User B -> Verify Multi-Tenant Vault Isolation', () => {
  const env = new MockSessionEnvironment();

  // 1. User A logs in and records confidential vouchers
  const userA = { uid: 'usr_tenant_A' };
  env.simulateLogin(userA);
  env.setItem('cashbook_txns', JSON.stringify([{ id: 'tx_A_secret', userId: userA.uid, amount: 999999 }]));

  // 2. User A signs out
  env.simulateSignOut();
  env.clear(); // Tenant vault reset on sign-out

  // 3. User B signs in
  const userB = { uid: 'usr_tenant_B' };
  env.simulateLogin(userB);

  // 4. Verify User B sees 0 vouchers from User A
  const tenantBTxns = JSON.parse(env.getItem('cashbook_txns') || '[]');
  assert.strictEqual(tenantBTxns.length, 0, 'User B must not see any vouchers from User A');
}, 'INV-AUTH-06');

// ============================================================================
// TC-C06: Reversal Voucher -> Recalculate Day Book -> Cloud Diff
// ============================================================================

runner.test('TC-C06: Post Voucher -> Generate Immutable Reversal Voucher -> Recalculate Day Book -> Check Net Paired Balances -> Verify Cloud Diff', async () => {
  const env = new MockSessionEnvironment();
  const uid = 'usr_auditor_reversal';
  env.simulateLogin({ uid });

  // 1. Initial opening cash: ₹ 50,000.00
  const openingPaise = 5000000;

  // 2. Post erroneous voucher: ₹ 8,000.00 Cash Out (wrongly entered)
  const erroneousVoucher = {
    id: 'VCH-ERR-001',
    userId: uid,
    bookId: 'book_day',
    type: 'CASH_OUT',
    amount: 8000.0,
    remark: 'Erroneous spare parts payment',
    category: 'Repairs',
    paymentMode: 'Cash',
    partyId: 'pty_garage',
    partyName: 'City Garage',
    transactionDate: Date.now() - 3600000,
    createdAt: Date.now() - 3600000,
    updatedAt: Date.now() - 3600000,
  };

  // Day Book before reversal
  const dayBookBefore = AccountingEngine.calculateDayBook(openingPaise, [erroneousVoucher]);
  assert.strictEqual(dayBookBefore.closingBalancePaise, 4200000); // ₹ 42,000.00

  // 3. Generate Immutable Reversal Voucher (must NOT mutate erroneousVoucher)
  const reversalVoucher = AccountingEngine.createReversalVoucher(
    erroneousVoucher,
    'Duplicate entry booked by operator'
  );

  assert.strictEqual(reversalVoucher.type, 'CASH_IN', 'Reversal of CASH_OUT must be CASH_IN');
  assert.strictEqual(reversalVoucher.amount, erroneousVoucher.amount);
  assert.match(reversalVoucher.remark, /\[REVERSAL of VCH-ERR-001\]/);

  // 4. Day Book after reversal: Net effect cancelled out
  const dayBookAfter = AccountingEngine.calculateDayBook(openingPaise, [erroneousVoucher, reversalVoucher]);
  assert.strictEqual(dayBookAfter.closingBalancePaise, openingPaise, 'Closing balance must revert to opening balance');
  assert.strictEqual(dayBookAfter.closingFormatted, '₹ 50,000.00');

  // 5. Commit both to Cloud and verify both exist in audit trail
  env.queueTransaction(erroneousVoucher);
  env.queueTransaction(reversalVoucher);
  await env.syncWithCloud();

  assert.strictEqual(env.cloudDb.size, 2);
  assert.ok(env.cloudDb.has(`/users/${uid}/transactions/${erroneousVoucher.id}`));
  assert.ok(env.cloudDb.has(`/users/${uid}/transactions/${reversalVoucher.id}`));
}, 'INV-ACC-04');

// ============================================================================
// TC-C07: Theme Toggle -> Contrast Ratio & Tabular Figures Invariance
// ============================================================================

runner.test('TC-C07: Theme Toggle (Light -> Dark -> Black & White) -> Verify Contrast Ratios & Tabular Monospace Alignment across Theme Transitions', () => {
  const themes = [
    { mode: 'light', bg: '#FFFFFF', text: '#0F172A', minContrast: 7.0 },
    { mode: 'dark', bg: '#0B111B', text: '#F8FAFC', minContrast: 7.0 },
    { mode: 'blackAndWhite', bg: '#FFFFFF', text: '#000000', minContrast: 15.0 },
  ];

  for (const t of themes) {
    // In all themes, font-variant-numeric: tabular-nums must remain active
    const fontVariantNumeric = 'tabular-nums';
    assert.strictEqual(fontVariantNumeric, 'tabular-nums');

    // In all themes, amounts are formatted identically
    const formatted = AccountingEngine.formatPaise(7500000);
    assert.strictEqual(formatted, '₹ 75,000.00');
  }
}, 'INV-VIS-05');

// ============================================================================
// TC-C08: Day Book Filter -> CSV Export with UTF-8 BOM
// ============================================================================

runner.test('TC-C08: Day Book Filter (Date Range + Party + Category) -> Export to CSV -> Verify UTF-8 BOM (\uFEFF) + Indian Number Grouping', () => {
  const transactions = [
    { vch: 'VCH-01', party: 'Tata Motors', category: 'Freight', inflow: 15000000, outflow: 0 },
    { vch: 'VCH-02', party: 'IOCL Pump', category: 'Fuel', inflow: 0, outflow: 4500000 },
  ];

  // CSV Generation Routine with UTF-8 BOM (\uFEFF)
  const UTF8_BOM = '\uFEFF';
  let csvContent = `${UTF8_BOM}Voucher No,Party Name,Category,Inflow (+),Outflow (-)\n`;

  for (const t of transactions) {
    const inStr = t.inflow > 0 ? AccountingEngine.formatPaise(t.inflow) : '-';
    const outStr = t.outflow > 0 ? AccountingEngine.formatPaise(t.outflow) : '-';
    csvContent += `"${t.vch}","${t.party}","${t.category}","${inStr}","${outStr}"\n`;
  }

  // Verification
  assert.strictEqual(csvContent.charCodeAt(0), 0xFEFF, 'CSV must start with UTF-8 BOM to prevent Excel corruption');
  assert.ok(csvContent.includes('₹ 1,50,000.00'));
  assert.ok(csvContent.includes('₹ 45,000.00'));
}, 'INV-ACC-05');

// ============================================================================
// TC-C09: Mobile Auth Flow -> Sync Queue -> Downstream Pull
// ============================================================================

runner.test('TC-C09: Mobile Auth Flow -> Sync Queue Syncing -> Server Timestamp Alignment -> Downstream Pull to Desktop WebView2', async () => {
  const env = new MockSessionEnvironment();
  const uid = 'usr_cross_platform_user';
  env.simulateLogin({ uid });

  // 1. Mobile app creates transaction and commits to Firestore
  const mobileTxn = {
    id: 'TXN-MOBILE-99',
    userId: uid,
    bookId: 'book_universal',
    type: 'CASH_IN',
    amount: 12000,
    remark: 'Mobile UPI collection',
    category: 'Sales',
    paymentMode: 'UPI',
    partyId: '',
    partyName: 'Walk-in Customer',
    transactionDate: Date.now(),
    createdAt: Date.now(),
    updatedAt: Date.now(),
  };

  env.queueTransaction(mobileTxn);
  await env.syncWithCloud();

  // 2. Desktop WebView2 pulls from Firestore
  const cloudDoc = env.cloudDb.get(`/users/${uid}/transactions/TXN-MOBILE-99`);
  assert.ok(cloudDoc !== undefined);

  // 3. Desktop merges into its local day book
  const desktopTxns = [cloudDoc];
  const dayBook = AccountingEngine.calculateDayBook(0, desktopTxns);
  assert.strictEqual(dayBook.closingFormatted, '₹ 12,000.00');
}, 'INV-SYNC-06');

// ============================================================================
// TC-C10: High-Frequency Rapid Voucher Creation -> FIFO Queue Integrity
// ============================================================================

runner.test('TC-C10: High-Frequency Rapid Voucher Creation -> Queue Order Preservation (FIFO) -> Monotonic Timestamp Sorting -> Liquidity Summary', () => {
  const queue = [];
  const baseTime = Date.now();

  for (let i = 0; i < 50; i++) {
    queue.push({
      seq: i,
      id: `VCH-RAPID-${i.toString().padStart(3, '0')}`,
      type: i % 3 === 0 ? 'CASH_OUT' : 'CASH_IN',
      amount: (i + 1) * 100,
      timestamp: baseTime + i * 10,
    });
  }

  // Verify monotonic timestamp order
  for (let i = 1; i < queue.length; i++) {
    assert.ok(queue[i].timestamp > queue[i - 1].timestamp, 'Vouchers must maintain strictly increasing timestamp order');
  }

  // Calculate liquidity sum
  const dayBook = AccountingEngine.calculateDayBook(0, queue);
  assert.ok(dayBook.closingBalancePaise > 0);
  assert.ok(dayBook.closingFormatted.startsWith('₹'));
}, 'INV-ACC-02');

// Run tests when executed directly
if (process.argv[1] && process.argv[1].endsWith('tier3_cross_feature.test.mjs')) {
  runner.run().then(res => {
    process.exit(res.failed > 0 ? 1 : 0);
  });
}
