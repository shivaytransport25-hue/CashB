// Standardized date and time formatting utilities according to Indian convention (DD-MM-YYYY)

class DateFormatter {
  DateFormatter._();

  /// Formats a DateTime object to standard Indian format: DD-MM-YYYY
  /// Example: DateTime(2026, 11, 5) -> "05-11-2026"
  static String formatDate(DateTime date) {
    final String day = date.day.toString().padLeft(2, '0');
    final String month = date.month.toString().padLeft(2, '0');
    final String year = date.year.toString();
    return '$day-$month-$year';
  }

  /// Formats a DateTime object to 12-hour time format with AM/PM
  /// Example: DateTime(2026, 11, 5, 14, 30) -> "02:30 PM"
  static String formatTime(DateTime dateTime) {
    int hour = dateTime.hour;
    final int minute = dateTime.minute;
    final String period = hour >= 12 ? 'PM' : 'AM';

    hour = hour % 12;
    if (hour == 0) hour = 12;

    final String hourStr = hour.toString().padLeft(2, '0');
    final String minuteStr = minute.toString().padLeft(2, '0');

    return '$hourStr:$minuteStr $period';
  }

  /// Formats date and time together: "05-11-2026 02:30 PM"
  static String formatDateTime(DateTime dateTime) {
    return '${formatDate(dateTime)} ${formatTime(dateTime)}';
  }

  /// Parses a DD-MM-YYYY string into a DateTime object
  static DateTime? parseDate(String dateStr) {
    try {
      final parts = dateStr.trim().split('-');
      if (parts.length != 3) return null;

      final int day = int.parse(parts[0]);
      final int month = int.parse(parts[1]);
      final int year = int.parse(parts[2]);

      return DateTime(year, month, day);
    } catch (_) {
      return null;
    }
  }

  /// Returns the start and end DateTime for standard Indian Financial Year (01-04 to 31-03)
  static ({DateTime start, DateTime end}) getFinancialYear(DateTime referenceDate) {
    final int year = referenceDate.year;
    // In India, April (month 4) is the start of the financial year
    if (referenceDate.month >= 4) {
      final start = DateTime(year, 4, 1);
      final end = DateTime(year + 1, 3, 31, 23, 59, 59, 999);
      return (start: start, end: end);
    } else {
      final start = DateTime(year - 1, 4, 1);
      final end = DateTime(year, 3, 31, 23, 59, 59, 999);
      return (start: start, end: end);
    }
  }

  /// Formats Financial Year label e.g. "FY 2026-27"
  static String getFinancialYearLabel(DateTime referenceDate) {
    final fy = getFinancialYear(referenceDate);
    final int startYear = fy.start.year;
    final int endYear = fy.end.year % 100;
    return 'FY $startYear-$endYear';
  }

  /// Predefined date filter ranges
  static ({DateTime start, DateTime end}) getToday() {
    final now = DateTime.now();
    return (
      start: DateTime(now.year, now.month, now.day),
      end: DateTime(now.year, now.month, now.day, 23, 59, 59, 999)
    );
  }

  static ({DateTime start, DateTime end}) getYesterday() {
    final yesterday = DateTime.now().subtract(const Duration(days: 1));
    return (
      start: DateTime(yesterday.year, yesterday.month, yesterday.day),
      end: DateTime(yesterday.year, yesterday.month, yesterday.day, 23, 59, 59, 999)
    );
  }

  static ({DateTime start, DateTime end}) getThisMonth() {
    final now = DateTime.now();
    final start = DateTime(now.year, now.month, 1);
    final end = DateTime(now.year, now.month + 1, 0, 23, 59, 59, 999);
    return (start: start, end: end);
  }

  static ({DateTime start, DateTime end}) getLastMonth() {
    final now = DateTime.now();
    final start = DateTime(now.year, now.month - 1, 1);
    final end = DateTime(now.year, now.month, 0, 23, 59, 59, 999);
    return (start: start, end: end);
  }
}

