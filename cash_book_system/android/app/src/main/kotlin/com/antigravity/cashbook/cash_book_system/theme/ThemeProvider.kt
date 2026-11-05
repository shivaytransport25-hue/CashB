package com.antigravity.cashbook.cash_book_system.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class FinancialPalette {
    EMERALD,
    ROYAL,
    WEALTH_AMBER,
    MONOCHROME
}

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val palette: FinancialPalette = FinancialPalette.WEALTH_AMBER
) {
    fun isDark(systemInDark: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.SYSTEM -> systemInDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }
}

/**
 * Centralized Theme Provider / State Controller for Jetpack Compose.
 * Persists user preference across app sessions using SharedPreferences.
 */
class ThemeProvider(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cashbook_theme_prefs", Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(loadInitialState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    private fun loadInitialState(): ThemeState {
        val modeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val dynamic = prefs.getBoolean("use_dynamic_color", true)
        val paletteStr = prefs.getString("financial_palette", FinancialPalette.WEALTH_AMBER.name)
            ?: FinancialPalette.WEALTH_AMBER.name

        return ThemeState(
            themeMode = runCatching { ThemeMode.valueOf(modeStr) }.getOrDefault(ThemeMode.SYSTEM),
            useDynamicColor = dynamic,
            palette = runCatching { FinancialPalette.valueOf(paletteStr) }.getOrDefault(FinancialPalette.WEALTH_AMBER)
        )
    }

    /**
     * Dedicated 'Dark Mode' toggle:
     * - [ThemeMode.LIGHT] = Force Light Mode
     * - [ThemeMode.DARK] = Force Dark Mode
     * - [ThemeMode.SYSTEM] = Match OS appearance
     */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeState.update { it.copy(themeMode = mode) }
    }

    /**
     * Enable / disable Android 12+ dynamic color themes (Monet / Wallpaper extraction).
     */
    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("use_dynamic_color", enabled).apply()
        _themeState.update { it.copy(useDynamicColor = enabled) }
    }

    /**
     * Select a specific financial color palette (Emerald, Royal, Amber, etc.).
     */
    fun setPalette(palette: FinancialPalette) {
        prefs.edit().putString("financial_palette", palette.name).apply()
        _themeState.update { it.copy(palette = palette) }
    }
}

