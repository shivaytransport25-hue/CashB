import '../core/constants/enums.dart';
import '../models/account_model.dart';
import '../models/transaction_model.dart';

/// Centralized Financial Calculation Engine
/// Enforces integer paise arithmetic for absolute precision. Zero binary floating-point errors.
class FinancialEngine {
  FinancialEngine._();

  /// Calculates closing balance for an account given opening balance and all posted transactions
  /// Formula: Closing Balance = Opening Balance + Total Received - Total Paid
  static int calculateClosingBalance({
    required int openingBalancePaise,
    required List<TransactionModel> transactions,
    required String accountId,
  }) {
    int balance = openingBalancePaise;

    for (final txn in transactions) {
      if (txn.status != TransactionStatus.posted) continue;

      if (txn.accountId == accountId) {
        switch (txn.transactionType) {
          case TransactionType.cashReceived:
          case TransactionType.bankReceived:
            balance += txn.amountPaise;
            break;
          case TransactionType.cashPaid:
          case TransactionType.bankPayment:
            balance -= txn.amountPaise;
            break;
          case TransactionType.transfer:
            // Check if this is incoming or outgoing transfer leg
            if (txn.id.endsWith('_in') || (txn.description != null && txn.description!.startsWith('Transfer from'))) {
              balance += txn.amountPaise;
            } else {
              balance -= txn.amountPaise;
            }
            break;
          case TransactionType.adjustment:
            balance += txn.amountPaise; // Can be positive or negative
            break;
          case TransactionType.openingBalance:
            // Handled as part of opening balance
            break;
        }
      } else if (txn.targetAccountId == accountId && txn.transactionType == TransactionType.transfer) {
        // Destination account of transfer increases
        balance += txn.amountPaise;
      }
    }

    return balance;
  }

  /// Calculates financial summary metrics for a list of transactions
  /// Note: Transfers are strictly excluded from Income and Expense totals.
  static FinancialSummary calculateSummary(List<TransactionModel> transactions) {
    int totalIncomePaise = 0;
    int totalExpensePaise = 0;
    int totalTransfersPaise = 0;
    int totalReversalsPaise = 0;
    int cashIncomePaise = 0;
    int cashExpensePaise = 0;
    int bankIncomePaise = 0;
    int bankExpensePaise = 0;

    for (final txn in transactions) {
      if (txn.status == TransactionStatus.reversed) {
        totalReversalsPaise += txn.amountPaise;
        continue;
      }
      if (txn.status != TransactionStatus.posted) continue;

      switch (txn.transactionType) {
        case TransactionType.cashReceived:
          totalIncomePaise += txn.amountPaise;
          cashIncomePaise += txn.amountPaise;
          break;
        case TransactionType.bankReceived:
          totalIncomePaise += txn.amountPaise;
          bankIncomePaise += txn.amountPaise;
          break;
        case TransactionType.cashPaid:
          totalExpensePaise += txn.amountPaise;
          cashExpensePaise += txn.amountPaise;
          break;
        case TransactionType.bankPayment:
          totalExpensePaise += txn.amountPaise;
          bankExpensePaise += txn.amountPaise;
          break;
        case TransactionType.transfer:
          totalTransfersPaise += txn.amountPaise;
          break;
        case TransactionType.adjustment:
          if (txn.amountPaise > 0) {
            totalIncomePaise += txn.amountPaise;
          } else {
            totalExpensePaise += txn.amountPaise.abs();
          }
          break;
        case TransactionType.openingBalance:
          break;
      }
    }

    final int netCashFlowPaise = totalIncomePaise - totalExpensePaise;

    return FinancialSummary(
      totalIncomePaise: totalIncomePaise,
      totalExpensePaise: totalExpensePaise,
      netCashFlowPaise: netCashFlowPaise,
      totalTransfersPaise: totalTransfersPaise,
      totalReversalsPaise: totalReversalsPaise,
      cashIncomePaise: cashIncomePaise,
      cashExpensePaise: cashExpensePaise,
      bankIncomePaise: bankIncomePaise,
      bankExpensePaise: bankExpensePaise,
    );
  }

  /// Validates if an account would have an invalid negative balance after a transaction
  static ({bool isValid, String? errorMessage}) validateBalanceConstraints({
    required AccountModel account,
    required int prospectiveDeductionPaise,
  }) {
    if (!account.allowNegativeBalance && (account.currentBalancePaise - prospectiveDeductionPaise) < 0) {
      return (
        isValid: false,
        errorMessage:
            'Insufficient balance in account "${account.accountName}". Current balance is not allowed to go below zero.'
      );
    }
    return (isValid: true, errorMessage: null);
  }

  /// Creates a paired atomic transfer between two accounts
  /// Generates a single shared `transferId` linking both legs.
  static ({TransactionModel transferDebit, TransactionModel transferCredit}) createAtomicTransfer({
    required String transferId,
    required String businessId,
    required String sourceVoucher,
    required String targetVoucher,
    required String transactionDate,
    required String transactionTime,
    required AccountModel sourceAccount,
    required AccountModel targetAccount,
    required int amountPaise,
    required String paymentMode,
    String? referenceNumber,
    String? narration,
    required String createdBy,
    required String createdByName,
    required String deviceId,
    required String idempotencyKeyPrefix,
  }) {
    final now = DateTime.now();

    final transferDebit = TransactionModel(
      id: '${transferId}_out',
      businessId: businessId,
      voucherNumber: sourceVoucher,
      transactionDate: transactionDate,
      transactionTime: transactionTime,
      transactionType: TransactionType.transfer,
      accountId: sourceAccount.id,
      accountName: sourceAccount.accountName,
      targetAccountId: targetAccount.id,
      targetAccountName: targetAccount.accountName,
      transferId: transferId,
      description: narration ?? 'Transfer to ${targetAccount.accountName}',
      paymentMode: paymentMode,
      amountPaise: amountPaise,
      referenceNumber: referenceNumber,
      createdBy: createdBy,
      createdByName: createdByName,
      deviceId: deviceId,
      idempotencyKey: '${idempotencyKeyPrefix}_debit',
      createdAt: now,
      updatedAt: now,
      status: TransactionStatus.posted,
    );

    final transferCredit = TransactionModel(
      id: '${transferId}_in',
      businessId: businessId,
      voucherNumber: targetVoucher,
      transactionDate: transactionDate,
      transactionTime: transactionTime,
      transactionType: TransactionType.transfer,
      accountId: targetAccount.id,
      accountName: targetAccount.accountName,
      targetAccountId: sourceAccount.id,
      targetAccountName: sourceAccount.accountName,
      transferId: transferId,
      description: narration ?? 'Transfer from ${sourceAccount.accountName}',
      paymentMode: paymentMode,
      amountPaise: amountPaise,
      referenceNumber: referenceNumber,
      createdBy: createdBy,
      createdByName: createdByName,
      deviceId: deviceId,
      idempotencyKey: '${idempotencyKeyPrefix}_credit',
      createdAt: now,
      updatedAt: now,
      status: TransactionStatus.posted,
    );

    return (transferDebit: transferDebit, transferCredit: transferCredit);
  }

  /// Creates a reversal transaction that mathematically offsets an original transaction
  static TransactionModel createReversalTransaction({
    required TransactionModel originalTransaction,
    required String reversalVoucherNumber,
    required String reversalReason,
    required String reversedBy,
    required String reversedByName,
    required String deviceId,
    required String newTransactionId,
  }) {
    final now = DateTime.now();
    TransactionType reversalType;

    // Invert the type for the reversal
    switch (originalTransaction.transactionType) {
      case TransactionType.cashReceived:
        reversalType = TransactionType.cashPaid;
        break;
      case TransactionType.cashPaid:
        reversalType = TransactionType.cashReceived;
        break;
      case TransactionType.bankReceived:
        reversalType = TransactionType.bankPayment;
        break;
      case TransactionType.bankPayment:
        reversalType = TransactionType.bankReceived;
        break;
      case TransactionType.transfer:
        reversalType = TransactionType.transfer;
        break;
      case TransactionType.adjustment:
        reversalType = TransactionType.adjustment;
        break;
      case TransactionType.openingBalance:
        reversalType = TransactionType.adjustment;
        break;
    }

    return TransactionModel(
      id: newTransactionId,
      businessId: originalTransaction.businessId,
      voucherNumber: reversalVoucherNumber,
      transactionDate: originalTransaction.transactionDate,
      transactionTime: originalTransaction.transactionTime,
      transactionType: reversalType,
      accountId: originalTransaction.accountId,
      accountName: originalTransaction.accountName,
      targetAccountId: originalTransaction.targetAccountId,
      targetAccountName: originalTransaction.targetAccountName,
      categoryId: originalTransaction.categoryId,
      categoryName: originalTransaction.categoryName,
      partyId: originalTransaction.partyId,
      partyName: originalTransaction.partyName,
      description: 'Reversal of ${originalTransaction.voucherNumber}: $reversalReason',
      paymentMode: originalTransaction.paymentMode,
      amountPaise: originalTransaction.amountPaise,
      referenceNumber: originalTransaction.referenceNumber,
      status: TransactionStatus.posted,
      reversalReason: reversalReason,
      originalTransactionId: originalTransaction.id,
      createdBy: reversedBy,
      createdByName: reversedByName,
      deviceId: deviceId,
      idempotencyKey: 'reversal_${originalTransaction.id}_${now.millisecondsSinceEpoch}',
      createdAt: now,
      updatedAt: now,
    );
  }

  /// Calculates running balance for a ledger statement (ordered chronologically)
  static List<LedgerStatementLine> computeLedgerWithRunningBalances({
    required int initialOpeningBalancePaise,
    required List<TransactionModel> transactions,
  }) {
    int runningBalance = initialOpeningBalancePaise;
    final List<LedgerStatementLine> lines = [];

    for (final txn in transactions) {
      if (txn.status != TransactionStatus.posted) continue;

      int debitPaise = 0;
      int creditPaise = 0;

      if (txn.isIncome) {
        creditPaise = txn.amountPaise;
        runningBalance += creditPaise;
      } else if (txn.isExpense) {
        debitPaise = txn.amountPaise;
        runningBalance -= debitPaise;
      } else if (txn.isTransfer) {
        // Depending on whether it's incoming or outgoing for the account
        debitPaise = txn.amountPaise;
        runningBalance -= debitPaise;
      } else if (txn.transactionType == TransactionType.adjustment) {
        if (txn.amountPaise >= 0) {
          creditPaise = txn.amountPaise;
          runningBalance += creditPaise;
        } else {
          debitPaise = txn.amountPaise.abs();
          runningBalance -= debitPaise;
        }
      }

      lines.add(LedgerStatementLine(
        transaction: txn,
        debitPaise: debitPaise,
        creditPaise: creditPaise,
        runningBalancePaise: runningBalance,
      ));
    }

    return lines;
  }
}

class FinancialSummary {
  final int totalIncomePaise;
  final int totalExpensePaise;
  final int netCashFlowPaise;
  final int totalTransfersPaise;
  final int totalReversalsPaise;
  final int cashIncomePaise;
  final int cashExpensePaise;
  final int bankIncomePaise;
  final int bankExpensePaise;

  FinancialSummary({
    required this.totalIncomePaise,
    required this.totalExpensePaise,
    required this.netCashFlowPaise,
    required this.totalTransfersPaise,
    required this.totalReversalsPaise,
    required this.cashIncomePaise,
    required this.cashExpensePaise,
    required this.bankIncomePaise,
    required this.bankExpensePaise,
  });
}

class LedgerStatementLine {
  final TransactionModel transaction;
  final int debitPaise;
  final int creditPaise;
  final int runningBalancePaise;

  LedgerStatementLine({
    required this.transaction,
    required this.debitPaise,
    required this.creditPaise,
    required this.runningBalancePaise,
  });
}
