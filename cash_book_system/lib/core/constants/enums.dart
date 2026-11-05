// Core enumeration types for Cash Book Management System

enum TransactionType {
  cashReceived, // Money received in Cash (Income)
  cashPaid,     // Money paid in Cash (Expense)
  bankReceived, // Money received in Bank (Income)
  bankPayment,  // Money paid from Bank (Expense)
  transfer,     // Account-to-account transfer (Cash<->Bank, Bank<->Bank)
  openingBalance, // Initial balance setup
  adjustment,   // Authorized balance correction
}

extension TransactionTypeExtension on TransactionType {
  String get displayName {
    switch (this) {
      case TransactionType.cashReceived:
        return 'Cash Received (Income)';
      case TransactionType.cashPaid:
        return 'Cash Paid (Expense)';
      case TransactionType.bankReceived:
        return 'Bank Received (Income)';
      case TransactionType.bankPayment:
        return 'Bank Payment (Expense)';
      case TransactionType.transfer:
        return 'Account Transfer';
      case TransactionType.openingBalance:
        return 'Opening Balance';
      case TransactionType.adjustment:
        return 'Adjustment Entry';
    }
  }

  bool get isIncome =>
      this == TransactionType.cashReceived || this == TransactionType.bankReceived;

  bool get isExpense =>
      this == TransactionType.cashPaid || this == TransactionType.bankPayment;

  bool get isTransfer => this == TransactionType.transfer;
}

enum AccountType {
  cash,
  bank,
}

extension AccountTypeExtension on AccountType {
  String get displayName {
    switch (this) {
      case AccountType.cash:
        return 'Cash Account';
      case AccountType.bank:
        return 'Bank Account';
    }
  }
}

enum PartyType {
  customer,
  supplier,
  employee,
}

extension PartyTypeExtension on PartyType {
  String get displayName {
    switch (this) {
      case PartyType.customer:
        return 'Customer';
      case PartyType.supplier:
        return 'Supplier';
      case PartyType.employee:
        return 'Employee';
    }
  }
}

enum OpeningBalanceType {
  receivable, // Money to receive from party (Asset)
  payable,    // Money to pay to party (Liability)
}

enum TransactionStatus {
  draft,
  posted,
  reversed,
  cancelled,
}

enum UserRole {
  owner,
  admin,
  accountant,
  dataEntry,
  viewer,
}

extension UserRoleExtension on UserRole {
  String get displayName {
    switch (this) {
      case UserRole.owner:
        return 'Owner';
      case UserRole.admin:
        return 'Administrator';
      case UserRole.accountant:
        return 'Accountant';
      case UserRole.dataEntry:
        return 'Data Entry Operator';
      case UserRole.viewer:
        return 'Viewer';
    }
  }

  bool get canManageUsers => this == UserRole.owner;
  bool get canManageBusinessSettings => this == UserRole.owner || this == UserRole.admin;
  bool get canManageAccounts => this == UserRole.owner || this == UserRole.admin;
  bool get canManageBackups => this == UserRole.owner;
  bool get canPostTransactions =>
      this == UserRole.owner ||
      this == UserRole.admin ||
      this == UserRole.accountant ||
      this == UserRole.dataEntry;
  bool get canReverseTransactions =>
      this == UserRole.owner || this == UserRole.admin || this == UserRole.accountant;
  bool get canExportReports =>
      this == UserRole.owner || this == UserRole.admin || this == UserRole.accountant;
  bool get isReadOnly => this == UserRole.viewer;
}

enum SyncStatus {
  synced,
  pending,
  inFlight,
  failed,
}

enum RecurrenceFrequency {
  daily,
  weekly,
  monthly,
  yearly,
  custom,
}

