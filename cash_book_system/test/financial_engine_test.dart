import 'package:flutter_test/flutter_test.dart';
import 'package:cash_book_system/accounting/financial_engine.dart';
import 'package:cash_book_system/core/constants/enums.dart';
import 'package:cash_book_system/core/utils/currency_formatter.dart';
import 'package:cash_book_system/models/account_model.dart';
import 'package:cash_book_system/models/transaction_model.dart';

void main() {
  group('1. Currency Formatter & Integer Paise Precision Tests', () {
    test('Converts formatted Indian Rupee strings to exact integer paise without float errors', () {
      expect(CurrencyFormatter.parseToPaise('₹1,25,000.00'), 12500000);
      expect(CurrencyFormatter.parseToPaise('5000.50'), 500050);
      expect(CurrencyFormatter.parseToPaise('100'), 10000);
      expect(CurrencyFormatter.parseToPaise('₹ 0.75'), 75);
      expect(CurrencyFormatter.parseToPaise('-500.00'), -50000);
    });

    test('Formats integer paise into Indian Numbering convention (Lakhs & Crores)', () {
      expect(CurrencyFormatter.formatPaise(100000), '₹1,000.00');
      expect(CurrencyFormatter.formatPaise(12500000), '₹1,25,000.00');
      expect(CurrencyFormatter.formatPaise(1000000000), '₹1,00,00,000.00');
      expect(CurrencyFormatter.formatPaise(50), '₹0.50');
      expect(CurrencyFormatter.formatPaise(-250000), '-₹2,500.00');
    });

    test('Converts paise to Indian currency words properly', () {
      expect(
        CurrencyFormatter.toWords(1250050),
        'Twelve Thousand Five Hundred Rupees and Fifty Paise Only',
      );
      expect(
        CurrencyFormatter.toWords(10000000),
        'One Lakh Rupees Only',
      );
      expect(
        CurrencyFormatter.toWords(1000000000),
        'One Crore Rupees Only',
      );
    });
  });

  group('2. Financial Calculation Engine Core Math Tests', () {
    final now = DateTime.now();

    final testAccount = AccountModel(
      id: 'acc_cash',
      businessId: 'biz_test',
      accountName: 'Main Cash',
      accountType: AccountType.cash,
      openingBalancePaise: 5000000, // ₹50,000.00
      openingDate: '01-04-2026',
      currentBalancePaise: 5000000,
      createdAt: now,
      updatedAt: now,
    );

    test('Formula: Closing Balance = Opening Balance + Total Received - Total Paid', () {
      final transactions = [
        TransactionModel(
          id: 'txn_1',
          businessId: 'biz_test',
          voucherNumber: 'VCH-001',
          transactionDate: '05-11-2026',
          transactionTime: '10:00 AM',
          transactionType: TransactionType.cashReceived,
          accountId: 'acc_cash',
          paymentMode: 'Cash',
          amountPaise: 2000000, // ₹20,000.00 Income
          createdBy: 'user_1',
          createdByName: 'Admin',
          deviceId: 'dev_1',
          idempotencyKey: 'idem_1',
          createdAt: now,
          updatedAt: now,
        ),
        TransactionModel(
          id: 'txn_2',
          businessId: 'biz_test',
          voucherNumber: 'VCH-002',
          transactionDate: '05-11-2026',
          transactionTime: '11:00 AM',
          transactionType: TransactionType.cashPaid,
          accountId: 'acc_cash',
          paymentMode: 'Cash',
          amountPaise: 500000, // ₹5,000.00 Expense
          createdBy: 'user_1',
          createdByName: 'Admin',
          deviceId: 'dev_1',
          idempotencyKey: 'idem_2',
          createdAt: now,
          updatedAt: now,
        ),
      ];

      // Closing = 50,000 + 20,000 - 5,000 = 65,000 (6500000 paise)
      final closing = FinancialEngine.calculateClosingBalance(
        openingBalancePaise: testAccount.openingBalancePaise,
        transactions: transactions,
        accountId: 'acc_cash',
      );

      expect(closing, 6500000);
    });

    test('Transfers do NOT affect Total Income or Total Expense summaries', () {
      final transactions = [
        TransactionModel(
          id: 'txn_inc',
          businessId: 'biz_test',
          voucherNumber: 'VCH-001',
          transactionDate: '05-11-2026',
          transactionTime: '10:00 AM',
          transactionType: TransactionType.cashReceived,
          accountId: 'acc_cash',
          paymentMode: 'Cash',
          amountPaise: 1000000, // ₹10,000.00
          createdBy: 'user_1',
          createdByName: 'Admin',
          deviceId: 'dev_1',
          idempotencyKey: 'idem_inc',
          createdAt: now,
          updatedAt: now,
        ),
        TransactionModel(
          id: 'txn_exp',
          businessId: 'biz_test',
          voucherNumber: 'VCH-002',
          transactionDate: '05-11-2026',
          transactionTime: '11:00 AM',
          transactionType: TransactionType.cashPaid,
          accountId: 'acc_cash',
          paymentMode: 'Cash',
          amountPaise: 300000, // ₹3,000.00
          createdBy: 'user_1',
          createdByName: 'Admin',
          deviceId: 'dev_1',
          idempotencyKey: 'idem_exp',
          createdAt: now,
          updatedAt: now,
        ),
        TransactionModel(
          id: 'txn_xfer',
          businessId: 'biz_test',
          voucherNumber: 'VCH-003',
          transactionDate: '05-11-2026',
          transactionTime: '12:00 PM',
          transactionType: TransactionType.transfer,
          accountId: 'acc_cash',
          targetAccountId: 'acc_bank',
          paymentMode: 'Cash Deposit',
          amountPaise: 5000000, // ₹50,000 Transfer
          createdBy: 'user_1',
          createdByName: 'Admin',
          deviceId: 'dev_1',
          idempotencyKey: 'idem_xfer',
          createdAt: now,
          updatedAt: now,
        ),
      ];

      final summary = FinancialEngine.calculateSummary(transactions);

      // Income must strictly be ₹10,000
      expect(summary.totalIncomePaise, 1000000);
      // Expense must strictly be ₹3,000
      expect(summary.totalExpensePaise, 300000);
      // Net Cash Flow = 10,000 - 3,000 = 7,000
      expect(summary.netCashFlowPaise, 700000);
      // Transfer recorded separately
      expect(summary.totalTransfersPaise, 5000000);
    });

    test('Atomic transfer pairs update source and destination accounts accurately', () {
      final bankAcc = AccountModel(
        id: 'acc_bank',
        businessId: 'biz_test',
        accountName: 'SBI Bank',
        accountType: AccountType.bank,
        openingBalancePaise: 10000000, // ₹1,00,000
        openingDate: '01-04-2026',
        currentBalancePaise: 10000000,
        createdAt: now,
        updatedAt: now,
      );

      final pair = FinancialEngine.createAtomicTransfer(
        transferId: 'xfer_001',
        businessId: 'biz_test',
        sourceVoucher: 'VCH-100',
        targetVoucher: 'VCH-100-B',
        transactionDate: '05-11-2026',
        transactionTime: '02:00 PM',
        sourceAccount: testAccount,
        targetAccount: bankAcc,
        amountPaise: 2500000, // ₹25,000
        paymentMode: 'Bank Deposit',
        createdBy: 'user_1',
        createdByName: 'Admin',
        deviceId: 'dev_1',
        idempotencyKeyPrefix: 'idem_xfer_001',
      );

      expect(pair.transferDebit.transferId, 'xfer_001');
      expect(pair.transferCredit.transferId, 'xfer_001');
      expect(pair.transferDebit.amountPaise, 2500000);
      expect(pair.transferCredit.amountPaise, 2500000);

      // Source account balance decreases
      final sourceClosing = FinancialEngine.calculateClosingBalance(
        openingBalancePaise: testAccount.openingBalancePaise,
        transactions: [pair.transferDebit],
        accountId: testAccount.id,
      );
      expect(sourceClosing, 5000000 - 2500000);

      // Target account balance increases
      final targetClosing = FinancialEngine.calculateClosingBalance(
        openingBalancePaise: bankAcc.openingBalancePaise,
        transactions: [pair.transferCredit],
        accountId: bankAcc.id,
      );
      expect(targetClosing, 10000000 + 2500000);
    });

    test('Non-negative balance rule prevents invalid overdraft when disallowed', () {
      final constrainedAcc = testAccount.copyWith(
        currentBalancePaise: 100000, // Only ₹1,000 available
        allowNegativeBalance: false,
      );

      // Attempt to deduct ₹5,000
      final validation = FinancialEngine.validateBalanceConstraints(
        account: constrainedAcc,
        prospectiveDeductionPaise: 500000,
      );

      expect(validation.isValid, false);
      expect(validation.errorMessage, contains('Insufficient balance'));
    });

    test('Reversal creates offsetting entry and restores account balance', () {
      final originalTxn = TransactionModel(
        id: 'txn_orig',
        businessId: 'biz_test',
        voucherNumber: 'VCH-999',
        transactionDate: '05-11-2026',
        transactionTime: '01:00 PM',
        transactionType: TransactionType.cashPaid, // ₹10,000 paid
        accountId: 'acc_cash',
        paymentMode: 'Cash',
        amountPaise: 1000000,
        createdBy: 'user_1',
        createdByName: 'Admin',
        deviceId: 'dev_1',
        idempotencyKey: 'idem_orig',
        createdAt: now,
        updatedAt: now,
      );

      final reversal = FinancialEngine.createReversalTransaction(
        originalTransaction: originalTxn,
        reversalVoucherNumber: 'VCH-999-REV',
        reversalReason: 'Wrong vendor entered',
        reversedBy: 'user_1',
        reversedByName: 'Admin',
        deviceId: 'dev_1',
        newTransactionId: 'txn_rev_1',
      );

      expect(reversal.originalTransactionId, originalTxn.id);
      expect(reversal.transactionType, TransactionType.cashReceived); // Inverted to restore cash
      expect(reversal.amountPaise, originalTxn.amountPaise);
      expect(reversal.reversalReason, 'Wrong vendor entered');
    });
  });
}

