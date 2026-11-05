package com.antigravity.cashbook.cash_book_system.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light Color Schemes
private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8)
)

private val RoyalLightColorScheme = lightColorScheme(
    primary = RoyalPrimary,
    onPrimary = RoyalOnPrimary,
    primaryContainer = RoyalPrimaryContainer,
    onPrimaryContainer = RoyalOnPrimaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8)
)

private val AmberLightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8)
)

// Dark Color Schemes
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkPrimaryContainer,
    onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = Color(0xFF334155)
)

private val RoyalDarkColorScheme = darkColorScheme(
    primary = RoyalDarkPrimary,
    onPrimary = RoyalDarkOnPrimary,
    primaryContainer = RoyalDarkPrimaryContainer,
    onPrimaryContainer = RoyalDarkOnPrimaryContainer,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = Color(0xFF334155)
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = AmberDarkPrimary,
    onPrimary = AmberDarkOnPrimary,
    primaryContainer = AmberDarkPrimaryContainer,
    onPrimaryContainer = AmberDarkOnPrimaryContainer,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = Color(0xFF334155)
)

/**
 * CashBook Pro Centralized Jetpack Compose Material 3 Theme.
 *
 * Supports:
 * 1. Android 12+ [dynamicColor] using Material3 [DynamicColorScheme].
 * 2. Dedicated [darkTheme] override toggle (Light Mode vs Dark Mode vs System).
 * 3. Custom Financial Palettes ([FinancialPalette]: Emerald, Royal, Amber/Wealth Growth).
 */
@Composable
fun CashBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: FinancialPalette = FinancialPalette.WEALTH_AMBER,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme = when {
        // Dynamic color is available on Android 12 (API 31)+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            when (palette) {
                FinancialPalette.EMERALD -> EmeraldDarkColorScheme
                FinancialPalette.ROYAL -> RoyalDarkColorScheme
                FinancialPalette.WEALTH_AMBER -> AmberDarkColorScheme
                FinancialPalette.MONOCHROME -> darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    background = Color.Black,
                    surface = Color(0xFF121212)
                )
            }
        }
        else -> {
            when (palette) {
                FinancialPalette.EMERALD -> EmeraldLightColorScheme
                FinancialPalette.ROYAL -> RoyalLightColorScheme
                FinancialPalette.WEALTH_AMBER -> AmberLightColorScheme
                FinancialPalette.MONOCHROME -> lightColorScheme(
                    primary = Color.Black,
                    onPrimary = Color.White,
                    background = Color.White,
                    surface = Color.White
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

