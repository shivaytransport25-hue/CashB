/**
 * test_helpers.mjs
 * Core Test Runner & Invariant Verification Engine for Cash Book Pro E2E Suite.
 *
 * Implements authoritative validation for:
 * 1. Visual Styling Invariants (CSS / Jetpack Compose / Flutter theme audit)
 * 2. Auth & Session Persistence Invariants (OAuth Web/Android IDs, dummy user eradication)
 * 3. Firestore Schema & Security Rules Whitelists (strict 12-key / 6-key / 7-key matching)
 * 4. Accounting & Money Math Precision (Integer paise arithmetic, contra, reversals)
 */

import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert';

// ANSI Colors for standalone CLI output
export const colors = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  dim: '\x1b[2m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  magenta: '\x1b[35m',
};

/**
 * Lightweight Standalone Test Runner
 */
export class TestRunner {
  constructor(tierName) {
    this.tierName = tierName;
    this.tests = [];
    this.passed = 0;
    this.failed = 0;
    this.skipped = 0;
    this.results = [];
  }

  test(name, fn, invariantId = 'N/A') {
    this.tests.push({ name, fn, invariantId });
  }

  async run() {
    console.log(`\n${colors.bold}${colors.cyan}================================================================${colors.reset}`);
    console.log(`${colors.bold}${colors.cyan}▶ RUNNING: ${this.tierName}${colors.reset}`);
    console.log(`${colors.bold}${colors.cyan}================================================================${colors.reset}`);

    const startTime = Date.now();

    for (let i = 0; i < this.tests.length; i++) {
      const { name, fn, invariantId } = this.tests[i];
      const testStart = Date.now();
      try {
        await fn();
        const duration = Date.now() - testStart;
        this.passed++;
        this.results.push({ name, status: 'PASS', invariantId, duration });
        console.log(`  ${colors.green}✔ PASS${colors.reset} [${invariantId}] ${name} ${colors.dim}(${duration}ms)${colors.reset}`);
      } catch (err) {
        const duration = Date.now() - testStart;
        this.failed++;
        this.results.push({ name, status: 'FAIL', invariantId, error: err.message, stack: err.stack, duration });
        console.log(`  ${colors.red}✖ FAIL${colors.reset} [${invariantId}] ${name}`);
        console.log(`    ${colors.red}Error: ${err.message}${colors.reset}`);
      }
    }

    const totalDuration = Date.now() - startTime;
    console.log(`\n${colors.bold}SUMMARY FOR ${this.tierName}:${colors.reset}`);
    console.log(`  Total: ${this.tests.length} | Passed: ${colors.green}${this.passed}${colors.reset} | Failed: ${this.failed > 0 ? colors.red + this.failed : colors.green + 0}${colors.reset} | Duration: ${totalDuration}ms`);

    return {
      tierName: this.tierName,
      total: this.tests.length,
      passed: this.passed,
      failed: this.failed,
      duration: totalDuration,
      results: this.results,
    };
  }
}

/**
 * 1. Financial Calculation Engine (Integer Paise Minor-Unit Math)
 */
export class AccountingEngine {
  /**
   * Converts Rupee string or number to exact integer paise
   * Example: '₹ 1,25,000.50' -> 12500050
   */
  static parseToPaise(amount) {
    if (typeof amount === 'number') {
      if (Number.isNaN(amount)) throw new Error('Invalid numeric amount NaN');
      return Math.round(amount * 100);
    }
    if (typeof amount !== 'string') {
      throw new Error(`Invalid amount type: ${typeof amount}`);
    }

    // Clean currency symbols, commas, spaces
    let clean = amount.replace(/[₹\s,]/g, '').trim();
    if (clean === '') throw new Error('Empty amount string');

    const isNegative = clean.startsWith('-');
    if (isNegative) clean = clean.substring(1).trim();

    if (!/^\d+(\.\d{1,2})?$/.test(clean)) {
      throw new Error(`Malformed currency string: "${amount}"`);
    }

    const parts = clean.split('.');
    const rupees = parseInt(parts[0], 10);
    let paise = 0;
    if (parts.length > 1) {
      paise = parseInt(parts[1].padEnd(2, '0').substring(0, 2), 10);
    }

    const totalPaise = rupees * 100 + paise;
    return isNegative ? -totalPaise : totalPaise;
  }

  /**
   * Formats integer paise into Indian Numbering convention: ₹ 1,00,000.00
   */
  static formatPaise(paise) {
    if (typeof paise !== 'number' || !Number.isInteger(paise)) {
      throw new Error(`Paise must be integer, got: ${paise}`);
    }

    const isNegative = paise < 0;
    const absPaise = Math.abs(paise);
    const rupees = Math.floor(absPaise / 100);
    const remainder = absPaise % 100;

    const rupeeStr = rupees.toString();
    let formattedRupees = '';

    if (rupeeStr.length <= 3) {
      formattedRupees = rupeeStr;
    } else {
      const lastThree = rupeeStr.substring(rupeeStr.length - 3);
      const remaining = rupeeStr.substring(0, rupeeStr.length - 3);
      const withCommas = remaining.replace(/\B(?=(\d{2})+(?!\d))/g, ',');
      formattedRupees = `${withCommas},${lastThree}`;
    }

    const decimalPart = remainder.toString().padStart(2, '0');
    return `${isNegative ? '-' : ''}₹ ${formattedRupees}.${decimalPart}`;
  }

  /**
   * Day Book calculation:
   * Closing Balance = Opening Balance + Inflow - Outflow
   */
  static calculateDayBook(openingBalancePaise, transactions) {
    let totalInflowPaise = 0;
    let totalOutflowPaise = 0;

    for (const txn of transactions) {
      const paise = this.parseToPaise(txn.amount);
      if (txn.type === 'CASH_IN') {
        totalInflowPaise += paise;
      } else if (txn.type === 'CASH_OUT') {
        totalOutflowPaise += paise;
      }
      // CONTRA transfers between internal accounts do not alter net liquid balance
    }

    const closingBalancePaise = openingBalancePaise + totalInflowPaise - totalOutflowPaise;

    return {
      openingBalancePaise,
      totalInflowPaise,
      totalOutflowPaise,
      netChangePaise: totalInflowPaise - totalOutflowPaise,
      closingBalancePaise,
      closingFormatted: this.formatPaise(closingBalancePaise),
      inflowFormatted: this.formatPaise(totalInflowPaise),
      outflowFormatted: this.formatPaise(totalOutflowPaise),
    };
  }

  /**
   * Creates an immutable reversal voucher
   */
  static createReversalVoucher(originalVoucher, reason, reversalDate = Date.now()) {
    if (!originalVoucher || !originalVoucher.id) {
      throw new Error('Original voucher ID is required for reversal');
    }
    if (originalVoucher.isReversal) {
      throw new Error(`Cannot reverse an already reversed voucher: ${originalVoucher.id}`);
    }

    let reversedType;
    if (originalVoucher.type === 'CASH_IN') reversedType = 'CASH_OUT';
    else if (originalVoucher.type === 'CASH_OUT') reversedType = 'CASH_IN';
    else if (originalVoucher.type === 'TRANSFER' || originalVoucher.type === 'CONTRA') reversedType = originalVoucher.type;
    else throw new Error(`Unknown voucher type: ${originalVoucher.type}`);

    return {
      id: `REV-${originalVoucher.id}-${Date.now().toString().slice(-4)}`,
      bookId: originalVoucher.bookId,
      userId: originalVoucher.userId,
      type: reversedType,
      amount: originalVoucher.amount,
      remark: `[REVERSAL of ${originalVoucher.id}] ${reason}`.substring(0, 500),
      category: 'Correction / Reversal',
      paymentMode: originalVoucher.paymentMode,
      partyId: originalVoucher.partyId || '',
      partyName: originalVoucher.partyName || '',
      transactionDate: reversalDate,
      createdAt: reversalDate,
      updatedAt: reversalDate,
    };
  }
}

/**
 * 2. Firestore Schema & Security Rules Validator (Mirror of firestore.rules)
 */
export class FirestoreSchemaValidator {
  static TRANSACTION_KEYS = [
    'userId', 'bookId', 'type', 'amount', 'remark',
    'category', 'paymentMode', 'partyId', 'partyName',
    'transactionDate', 'createdAt', 'updatedAt'
  ];

  static CASHBOOK_KEYS = [
    'userId', 'name', 'description', 'currencySymbol', 'createdAt', 'updatedAt'
  ];

  static PARTY_KEYS = [
    'userId', 'name', 'phone', 'role', 'notes', 'createdAt', 'updatedAt'
  ];

  static validateTransaction(doc, authUid) {
    const keys = Object.keys(doc);
    const missing = this.TRANSACTION_KEYS.filter(k => !keys.includes(k));
    if (missing.length > 0) {
      return { valid: false, reason: `Missing required schema keys: ${missing.join(', ')}` };
    }

    const illegal = keys.filter(k => !this.TRANSACTION_KEYS.includes(k));
    if (illegal.length > 0) {
      return { valid: false, reason: `Strict whitelist violation! Illegal extra keys: ${illegal.join(', ')}` };
    }

    if (doc.userId !== authUid) {
      return { valid: false, reason: `Permission Denied: doc.userId (${doc.userId}) does not match auth.uid (${authUid})` };
    }

    if (typeof doc.amount !== 'number' || doc.amount <= 0 || doc.amount > 1000000000) {
      return { valid: false, reason: `Amount must be > 0 and <= 1,000,000,000. Got: ${doc.amount}` };
    }

    if (!['CASH_IN', 'CASH_OUT', 'TRANSFER'].includes(doc.type)) {
      return { valid: false, reason: `Invalid transaction type: ${doc.type}` };
    }

    if (!['Cash', 'Bank Transfer', 'UPI', 'Cheque', 'Card'].includes(doc.paymentMode)) {
      return { valid: false, reason: `Invalid paymentMode: ${doc.paymentMode}` };
    }

    if (typeof doc.remark !== 'string' || doc.remark.length > 500) {
      return { valid: false, reason: `Remark exceeds 500 chars limit (length: ${doc.remark?.length})` };
    }

    if (typeof doc.partyName !== 'string' || doc.partyName.length > 80) {
      return { valid: false, reason: `PartyName exceeds 80 chars limit (length: ${doc.partyName?.length})` };
    }

    if (typeof doc.category !== 'string' || doc.category.length < 1 || doc.category.length > 60) {
      return { valid: false, reason: `Category must be 1-60 chars (length: ${doc.category?.length})` };
    }

    return { valid: true };
  }

  static validateCashBook(doc, authUid) {
    const keys = Object.keys(doc);
    const illegal = keys.filter(k => !this.CASHBOOK_KEYS.includes(k));
    if (illegal.length > 0) {
      return { valid: false, reason: `CashBook illegal keys: ${illegal.join(', ')}` };
    }
    if (doc.userId !== authUid) {
      return { valid: false, reason: `Permission Denied: userId mismatch` };
    }
    if (!doc.name || doc.name.length < 1 || doc.name.length > 80) {
      return { valid: false, reason: `Name must be 1-80 chars` };
    }
    return { valid: true };
  }

  static validateParty(doc, authUid) {
    const keys = Object.keys(doc);
    const illegal = keys.filter(k => !this.PARTY_KEYS.includes(k));
    if (illegal.length > 0) {
      return { valid: false, reason: `Party illegal keys: ${illegal.join(', ')}` };
    }
    if (doc.userId !== authUid) {
      return { valid: false, reason: `Permission Denied: userId mismatch` };
    }
    if (!['Customer', 'Supplier', 'Staff'].includes(doc.role)) {
      return { valid: false, reason: `Role must be Customer, Supplier, or Staff. Got: ${doc.role}` };
    }
    return { valid: true };
  }
}

/**
 * 3. Static Code Invariant Inspector
 */
export class CodebaseInspector {
  static scanFileForPatterns(filePath, patterns) {
    if (!fs.existsSync(filePath)) {
      return { exists: false, findings: [] };
    }
    const content = fs.readFileSync(filePath, 'utf8');
    const lines = content.split('\n');
    const findings = [];

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      for (const p of patterns) {
        if (p.regex.test(line)) {
          findings.push({
            rule: p.rule,
            invariantId: p.invariantId,
            lineNum: i + 1,
            lineText: line.trim(),
            prohibited: p.prohibited,
          });
        }
      }
    }

    return { exists: true, content, lineCount: lines.length, findings };
  }

  static getDesktopHtmlPath() {
    const paths = [
      'E:\\CashBookRelease\\app\\www\\index.html',
      'C:\\Users\\WIN\\AppData\\Local\\Programs\\CashBookPro\\www\\index.html',
      path.resolve(process.cwd(), 'installer/index.html'),
    ];
    for (const p of paths) {
      if (fs.existsSync(p)) return p;
    }
    return null;
  }
}

/**
 * 4. Mock Local Storage & Session State Simulator
 */
export class MockSessionEnvironment {
  constructor() {
    this.storage = new Map();
    this.session = null;
    this.isOnline = true;
    this.cloudDb = new Map(); // path -> document
  }

  setItem(key, value) {
    this.storage.set(key, String(value));
  }

  getItem(key) {
    return this.storage.has(key) ? this.storage.get(key) : null;
  }

  removeItem(key) {
    this.storage.delete(key);
  }

  clear() {
    this.storage.clear();
  }

  simulateLogin(user) {
    this.session = user;
    this.setItem('cashbook_auth_user', JSON.stringify(user));
  }

  simulateSignOut() {
    this.session = null;
    this.removeItem('cashbook_auth_user');
  }

  simulateAppRestart() {
    // Session in RAM is wiped, but persistent storage remains
    this.session = null;
    const stored = this.getItem('cashbook_auth_user');
    if (stored) {
      try {
        this.session = JSON.parse(stored);
      } catch (e) {
        this.session = null;
      }
    }
  }

  queueTransaction(txn) {
    const txns = JSON.parse(this.getItem('cashbook_txns') || '[]');
    txn._dirty = true;
    txns.push(txn);
    this.setItem('cashbook_txns', JSON.stringify(txns));
  }

  async syncWithCloud() {
    if (!this.isOnline) {
      return { success: false, reason: 'Network offline' };
    }
    if (!this.session || !this.session.uid) {
      return { success: false, reason: 'Unauthenticated session' };
    }

    const txns = JSON.parse(this.getItem('cashbook_txns') || '[]');
    let syncedCount = 0;

    for (const txn of txns) {
      if (txn._dirty) {
        // Strip _dirty and local id before cloud commit per 12-key whitelist (id is doc path, not a body field)
        const cloudPayload = { ...txn };
        delete cloudPayload._dirty;
        delete cloudPayload.id;

        const validation = FirestoreSchemaValidator.validateTransaction(cloudPayload, this.session.uid);
        if (!validation.valid) {
          throw new Error(`Cloud commit failed: ${validation.reason}`);
        }

        const docPath = `/users/${this.session.uid}/transactions/${txn.id}`;
        this.cloudDb.set(docPath, cloudPayload);
        delete txn._dirty;
        syncedCount++;
      }
    }

    this.setItem('cashbook_txns', JSON.stringify(txns));
    return { success: true, syncedCount };
  }
}
