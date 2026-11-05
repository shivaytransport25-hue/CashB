import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum AppColorTheme {
  emeraldGreen('Emerald Green', Color(0xFF1B5E20), Color(0xFF00695C)),
  royalBlue('Royal Blue', Color(0xFF0D47A1), Color(0xFF1565C0)),
  indigoPurple('Indigo Purple', Color(0xFF4A148C), Color(0xFF6A1B9A)),
  warmAmber('Warm Amber', Color(0xFFE65100), Color(0xFFF57C00)),
  crimsonRose('Crimson Rose', Color(0xFF880E4F), Color(0xFFAD1457)),
  oceanTeal('Ocean Teal', Color(0xFF00695C), Color(0xFF00897B));

  final String displayName;
  final Color primaryColor;
  final Color secondaryColor;

  const AppColorTheme(this.displayName, this.primaryColor, this.secondaryColor);
}

enum AppAppearanceMode {
  system('System Default (Auto)', Icons.brightness_auto),
  light('Light Mode', Icons.light_mode),
  dark('Dark Mode', Icons.dark_mode),
  blackAndWhite('Black & White (Monochrome)', Icons.contrast);

  final String displayName;
  final IconData icon;

  const AppAppearanceMode(this.displayName, this.icon);
}

class ThemeProvider extends ChangeNotifier with WidgetsBindingObserver {
  static const String _prefKeyTheme = 'cashbook_color_theme';
  static const String _prefKeyAppearance = 'cashbook_appearance_mode';

  AppColorTheme _colorTheme = AppColorTheme.emeraldGreen;
  AppAppearanceMode _appearanceMode = AppAppearanceMode.light;

  ThemeProvider() {
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangePlatformBrightness() {
    if (_appearanceMode == AppAppearanceMode.system) {
      notifyListeners();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  AppColorTheme get colorTheme => _colorTheme;
  AppAppearanceMode get appearanceMode => _appearanceMode;
  bool get isDarkMode {
    if (_appearanceMode == AppAppearanceMode.dark) return true;
    if (_appearanceMode == AppAppearanceMode.system) {
      return WidgetsBinding.instance.platformDispatcher.platformBrightness == Brightness.dark;
    }
    return false;
  }
  bool get isBlackAndWhite => _appearanceMode == AppAppearanceMode.blackAndWhite;

  Future<void> loadPreferences() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedTheme = prefs.getString(_prefKeyTheme);
      final savedAppearance = prefs.getString(_prefKeyAppearance);

      if (savedTheme != null) {
        _colorTheme = AppColorTheme.values.firstWhere(
          (t) => t.name == savedTheme,
          orElse: () => AppColorTheme.emeraldGreen,
        );
      }

      if (savedAppearance != null) {
        _appearanceMode = AppAppearanceMode.values.firstWhere(
          (a) => a.name == savedAppearance,
          orElse: () => AppAppearanceMode.light,
        );
      }
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading theme preferences: $e');
    }
  }

  Future<void> setColorTheme(AppColorTheme theme) async {
    _colorTheme = theme;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_prefKeyTheme, theme.name);
    } catch (e) {
      debugPrint('Error saving color theme: $e');
    }
  }

  Future<void> setAppearanceMode(AppAppearanceMode mode) async {
    _appearanceMode = mode;
    notifyListeners();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_prefKeyAppearance, mode.name);
    } catch (e) {
      debugPrint('Error saving appearance mode: $e');
    }
  }

  void toggleTheme() {
    if (_appearanceMode == AppAppearanceMode.light) {
      setAppearanceMode(AppAppearanceMode.dark);
    } else if (_appearanceMode == AppAppearanceMode.dark) {
      setAppearanceMode(AppAppearanceMode.system);
    } else if (_appearanceMode == AppAppearanceMode.system) {
      setAppearanceMode(AppAppearanceMode.blackAndWhite);
    } else {
      setAppearanceMode(AppAppearanceMode.light);
    }
  }

  ThemeData get themeData {
    if (_appearanceMode == AppAppearanceMode.blackAndWhite) {
      return _buildBlackAndWhiteTheme();
    } else if (_appearanceMode == AppAppearanceMode.dark) {
      return _buildDarkTheme();
    } else if (_appearanceMode == AppAppearanceMode.system) {
      final isDark = WidgetsBinding.instance.platformDispatcher.platformBrightness == Brightness.dark;
      return isDark ? _buildDarkTheme() : _buildLightTheme();
    } else {
      return _buildLightTheme();
    }
  }

  ThemeData _buildLightTheme() {
    final primary = _colorTheme.primaryColor;
    final secondary = _colorTheme.secondaryColor;

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      colorScheme: ColorScheme.fromSeed(
        seedColor: primary,
        brightness: Brightness.light,
        primary: primary,
        secondary: secondary,
        surface: const Color(0xFFF9FAFB),
        surfaceContainerHighest: const Color(0xFFEBEFF2),
      ),
      scaffoldBackgroundColor: const Color(0xFFF3F4F6),
      cardTheme: CardThemeData(
        elevation: 0.5,
        color: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: EdgeInsets.zero,
      ),
      appBarTheme: AppBarTheme(
        elevation: 0,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          elevation: 0.5,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: Color(0xFFE5E7EB),
        thickness: 1,
      ),
    );
  }

  ThemeData _buildDarkTheme() {
    final primary = _colorTheme.primaryColor;

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: ColorScheme.fromSeed(
        seedColor: primary,
        brightness: Brightness.dark,
        surface: const Color(0xFF1E1E2E),
        surfaceContainerHighest: const Color(0xFF2A2B3D),
      ),
      scaffoldBackgroundColor: const Color(0xFF12121E),
      cardTheme: CardThemeData(
        elevation: 1,
        color: const Color(0xFF1E1E2E),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        margin: EdgeInsets.zero,
      ),
      appBarTheme: const AppBarTheme(
        elevation: 0,
        backgroundColor: Color(0xFF1E1E2E),
        foregroundColor: Colors.white,
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          elevation: 1,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: Color(0xFF2E2F40),
        thickness: 1,
      ),
    );
  }

  ThemeData _buildBlackAndWhiteTheme() {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      colorScheme: const ColorScheme(
        brightness: Brightness.light,
        primary: Color(0xFF000000),
        onPrimary: Color(0xFFFFFFFF),
        secondary: Color(0xFF424242),
        onSecondary: Color(0xFFFFFFFF),
        error: Color(0xFF000000),
        onError: Color(0xFFFFFFFF),
        surface: Color(0xFFFFFFFF),
        onSurface: Color(0xFF000000),
        surfaceContainerHighest: Color(0xFFEEEEEE),
      ),
      scaffoldBackgroundColor: const Color(0xFFFFFFFF),
      cardTheme: CardThemeData(
        elevation: 0,
        color: Colors.white,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: const BorderSide(color: Color(0xFF000000), width: 1.5),
        ),
        margin: EdgeInsets.zero,
      ),
      appBarTheme: const AppBarTheme(
        elevation: 0,
        backgroundColor: Color(0xFF000000),
        foregroundColor: Color(0xFFFFFFFF),
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFF000000),
          foregroundColor: const Color(0xFFFFFFFF),
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(6),
            side: const BorderSide(color: Color(0xFF000000), width: 1.5),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: const Color(0xFF000000),
          side: const BorderSide(color: Color(0xFF000000), width: 1.5),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: Color(0xFF000000), width: 1.5),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: Color(0xFF000000), width: 1.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: Color(0xFF000000), width: 2.0),
        ),
        labelStyle: const TextStyle(color: Color(0xFF000000), fontWeight: FontWeight.w600),
      ),
      dividerTheme: const DividerThemeData(
        color: Color(0xFF000000),
        thickness: 1.2,
      ),
    );
  }
}
