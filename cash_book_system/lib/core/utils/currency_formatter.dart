// High-precision financial currency formatter and converter for Indian Rupees (₹)
// Enforces integer paise arithmetic to completely eliminate binary floating-point rounding errors.

class CurrencyFormatter {
  CurrencyFormatter._();

  /// Converts a user entered string (e.g. "1,250.50" or "5000") to integer paise.
  /// 1 Rupee = 100 Paise.
  static int parseToPaise(String input) {
    if (input.trim().isEmpty) return 0;
    
    // Remove currency symbol, commas, and whitespace
    String sanitized = input
        .replaceAll('₹', '')
        .replaceAll(',', '')
        .replaceAll(' ', '')
        .trim();

    if (sanitized.isEmpty) return 0;

    bool isNegative = false;
    if (sanitized.startsWith('-')) {
      isNegative = true;
      sanitized = sanitized.substring(1).trim();
    }

    final parts = sanitized.split('.');
    final rupeePart = parts[0].isEmpty ? '0' : parts[0];
    
    int rupees = int.tryParse(rupeePart) ?? 0;
    int paise = 0;

    if (parts.length > 1) {
      String decimalStr = parts[1];
      if (decimalStr.length == 1) {
        decimalStr = '${decimalStr}0';
      } else if (decimalStr.length > 2) {
        decimalStr = decimalStr.substring(0, 2);
      }
      paise = int.tryParse(decimalStr) ?? 0;
    }

    final totalPaise = (rupees * 100) + paise;
    return isNegative ? -totalPaise : totalPaise;
  }

  /// Formats an integer paise value to Indian Rupee format with ₹ symbol.
  /// Example:
  /// 100000 paise -> "₹1,000.00"
  /// 12500000 paise -> "₹1,25,000.00"
  /// 1000000000 paise -> "₹1,00,00,000.00"
  static String formatPaise(int paise, {bool includeSymbol = true, bool showDecimals = true}) {
    final bool isNegative = paise < 0;
    final int absPaise = paise.abs();

    final int rupees = absPaise ~/ 100;
    final int remainingPaise = absPaise % 100;

    final String formattedRupees = _formatIndianGrouping(rupees);
    final String decimalStr = remainingPaise.toString().padLeft(2, '0');

    final String result = showDecimals
        ? '$formattedRupees.$decimalStr'
        : formattedRupees;

    final String prefix = isNegative ? '-' : '';
    final String symbol = includeSymbol ? '₹' : '';

    return '$prefix$symbol$result';
  }

  /// Implements Indian Numbering grouping:
  /// Last 3 digits grouped together, subsequent groups of 2 digits.
  /// Example: 12500000 -> "1,25,00,000"
  static String _formatIndianGrouping(int amount) {
    final String str = amount.toString();
    if (str.length <= 3) return str;

    final String lastThree = str.substring(str.length - 3);
    String remaining = str.substring(0, str.length - 3);

    final StringBuffer buffer = StringBuffer();
    while (remaining.length > 2) {
      buffer.insert(0, ',${remaining.substring(remaining.length - 2)}');
      remaining = remaining.substring(0, remaining.length - 2);
    }
    buffer.insert(0, remaining);
    buffer.append(',$lastThree');

    return buffer.toString();
  }

  /// Converts paise to words in Indian numbering convention (Crores, Lakhs, Thousands, Rupees, Paise).
  /// Example: 1250050 -> "Twelve Thousand Five Hundred Rupees and Fifty Paise Only"
  static String toWords(int paise) {
    if (paise == 0) return 'Zero Rupees Only';

    final bool isNegative = paise < 0;
    final int absPaise = paise.abs();

    final int rupees = absPaise ~/ 100;
    final int remainingPaise = absPaise % 100;

    String words = '';
    if (rupees > 0) {
      words = '${_convertRupeesToWords(rupees)} Rupees';
    }

    if (remainingPaise > 0) {
      final String paiseWords = _convertRupeesToWords(remainingPaise);
      if (words.isNotEmpty) {
        words = '$words and $paiseWords Paise';
      } else {
        words = '$paiseWords Paise';
      }
    }

    words = '${words.trim()} Only';
    return isNegative ? 'Negative $words' : words;
  }

  static String _convertRupeesToWords(int n) {
    if (n == 0) return '';

    final List<String> units = [
      '', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
      'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen',
      'Seventeen', 'Eighteen', 'Nineteen'
    ];

    final List<String> tens = [
      '', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'
    ];

    String result = '';

    if ((n ~/ 10000000) > 0) {
      result += '${_convertRupeesToWords(n ~/ 10000000)} Crore ';
      n %= 10000000;
    }

    if ((n ~/ 100000) > 0) {
      result += '${_convertRupeesToWords(n ~/ 100000)} Lakh ';
      n %= 100000;
    }

    if ((n ~/ 1000) > 0) {
      result += '${_convertRupeesToWords(n ~/ 1000)} Thousand ';
      n %= 1000;
    }

    if ((n ~/ 100) > 0) {
      result += '${_convertRupeesToWords(n ~/ 100)} Hundred ';
      n %= 100;
    }

    if (n > 0) {
      if (n < 20) {
        result += units[n];
      } else {
        result += tens[n ~/ 10];
        if ((n % 10) > 0) {
          result += ' ${units[n % 10]}';
        }
      }
    }

    return result.trim();
  }
}

extension StringBufferAppend on StringBuffer {
  void append(String str) {
    write(str);
  }

  void insert(int index, String str) {
    // Helper to simulate prepend/insert for string buffer
    final current = toString();
    clear();
    write(str + current);
  }
}

