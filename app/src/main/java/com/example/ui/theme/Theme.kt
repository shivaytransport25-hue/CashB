package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------
// CENTRALIZED THEME CONTROLLER & COMPOSITION LOCAL
// ---------------------------------------------------------------------
@Immutable
data class ThemeController(
    val themeMode: AppThemeMode = AppThemeMode.CLASSIC_NAVY,
    val appearanceMode: ThemeAppearanceMode = ThemeAppearanceMode.SYSTEM,
    val isDarkMode: Boolean = false,
    val useDynamicColor: Boolean = false,
    val isDynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val onSelectThemeMode: (AppThemeMode) -> Unit = {},
    val onSelectAppearanceMode: (ThemeAppearanceMode) -> Unit = {},
    val onToggleDarkMode: (Boolean) -> Unit = {},
    val onToggleDynamicColor: (Boolean) -> Unit = {}
)

val LocalThemeController = staticCompositionLocalOf { ThemeController() }

// ---------------------------------------------------------------------
// COLOR SCHEME GENERATOR PER THEME & MATERIAL 3 DYNAMIC COLOR
// ---------------------------------------------------------------------
fun createLedgerColorScheme(
    context: Context,
    themeMode: AppThemeMode,
    isDark: Boolean,
    useDynamicColor: Boolean = false
): ColorScheme {
    // Material 3 Dynamic Color (Android 12+ / API 31+)
    val isDynamicRequested = useDynamicColor || themeMode == AppThemeMode.DYNAMIC_WALLPAPER
    if (isDynamicRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }

    return when (themeMode) {
        AppThemeMode.CLASSIC_NAVY, AppThemeMode.DYNAMIC_WALLPAPER -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF60A5FA),
                    onPrimary = Color(0xFF0B192C),
                    primaryContainer = Color(0xFF1E3A5F),
                    onPrimaryContainer = Color(0xFFDCE6F2),
                    secondary = CashInGreenDark,
                    onSecondary = Color(0xFF052E16),
                    secondaryContainer = CashInContainerDark,
                    onSecondaryContainer = Color(0xFFBBF7D0),
                    tertiary = Color(0xFFFBBF24),
                    onTertiary = Color(0xFF451A03),
                    error = CashOutRedDark,
                    onError = Color(0xFF450A0A),
                    background = NavyBackgroundDark,
                    onBackground = Color(0xFFF8FAFC),
                    surface = NavySurfaceDark,
                    onSurface = Color(0xFFF8FAFC),
                    surfaceVariant = Color(0xFF1E293B),
                    onSurfaceVariant = Color(0xFF94A3B8),
                    outline = LedgerBorderDark,
                    outlineVariant = Color(0xFF1E293B)
                )
            } else {
                lightColorScheme(
                    primary = NavyPrimary,
                    onPrimary = Color.White,
                    primaryContainer = NavyContainerLight,
                    onPrimaryContainer = Color(0xFF0D213A),
                    secondary = CashInGreen,
                    onSecondary = Color.White,
                    secondaryContainer = CashInContainerLight,
                    onSecondaryContainer = Color(0xFF064E3B),
                    tertiary = AmberWarning,
                    onTertiary = Color.White,
                    error = CashOutRed,
                    onError = Color.White,
                    background = NavyBackgroundLight,
                    onBackground = Color(0xFF0F172A),
                    surface = PaperWhite,
                    onSurface = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFFE8ECF2),
                    onSurfaceVariant = Color(0xFF475569),
                    outline = LedgerBorderLight,
                    outlineVariant = Color(0xFFE2E8F0)
                )
            }
        }

        AppThemeMode.EMERALD_GREEN -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF34D399),
                    onPrimary = Color(0xFF022C22),
                    primaryContainer = EmeraldContainerDark,
                    onPrimaryContainer = Color(0xFFA7F3D0),
                    secondary = Color(0xFF10B981),
                    onSecondary = Color.White,
                    error = CashOutRedDark,
                    background = EmeraldBackgroundDark,
                    onBackground = Color(0xFFECFDF5),
                    surface = EmeraldSurfaceDark,
                    onSurface = Color(0xFFECFDF5),
                    surfaceVariant = Color(0xFF133E2E),
                    onSurfaceVariant = Color(0xFFA7F3D0),
                    outline = Color(0xFF1B4D3E)
                )
            } else {
                lightColorScheme(
                    primary = EmeraldPrimary,
                    onPrimary = Color.White,
                    primaryContainer = EmeraldContainerLight,
                    onPrimaryContainer = Color(0xFF064E3B),
                    secondary = Color(0xFF059669),
                    onSecondary = Color.White,
                    error = CashOutRed,
                    background = EmeraldBackgroundLight,
                    onBackground = Color(0xFF06281C),
                    surface = PaperWhite,
                    onSurface = Color(0xFF06281C),
                    surfaceVariant = Color(0xFFDCFCE7),
                    onSurfaceVariant = Color(0xFF065F46),
                    outline = Color(0xFFA7F3D0)
                )
            }
        }

        AppThemeMode.ROYAL_BURGUNDY -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFB7185),
                    onPrimary = Color(0xFF4C0519),
                    primaryContainer = BurgundyContainerDark,
                    onPrimaryContainer = Color(0xFFFECDD3),
                    secondary = CashInGreenDark,
                    onSecondary = Color.White,
                    error = CashOutRedDark,
                    background = BurgundyBackgroundDark,
                    onBackground = Color(0xFFFFF1F2),
                    surface = BurgundySurfaceDark,
                    onSurface = Color(0xFFFFF1F2),
                    surfaceVariant = Color(0xFF3B121D),
                    onSurfaceVariant = Color(0xFFFDA4AF),
                    outline = Color(0xFF5A1E2D)
                )
            } else {
                lightColorScheme(
                    primary = BurgundyPrimary,
                    onPrimary = Color.White,
                    primaryContainer = BurgundyContainerLight,
                    onPrimaryContainer = Color(0xFF4C0519),
                    secondary = CashInGreen,
                    onSecondary = Color.White,
                    error = CashOutRed,
                    background = BurgundyBackgroundLight,
                    onBackground = Color(0xFF26050C),
                    surface = PaperWhite,
                    onSurface = Color(0xFF26050C),
                    surfaceVariant = Color(0xFFFFE4E6),
                    onSurfaceVariant = Color(0xFF881337),
                    outline = Color(0xFFFECDD3)
                )
            }
        }

        AppThemeMode.STEEL_SLATE -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF94A3B8),
                    onPrimary = Color(0xFF0F172A),
                    primaryContainer = SlateContainerDark,
                    onPrimaryContainer = Color(0xFFF1F5F9),
                    secondary = CashInGreenDark,
                    onSecondary = Color.White,
                    error = CashOutRedDark,
                    background = SlateBackgroundDark,
                    onBackground = Color(0xFFF8FAFC),
                    surface = SlateSurfaceDark,
                    onSurface = Color(0xFFF8FAFC),
                    surfaceVariant = Color(0xFF243042),
                    onSurfaceVariant = Color(0xFFCBD5E1),
                    outline = Color(0xFF3E4C5E)
                )
            } else {
                lightColorScheme(
                    primary = SlatePrimary,
                    onPrimary = Color.White,
                    primaryContainer = SlateContainerLight,
                    onPrimaryContainer = Color(0xFF0F172A),
                    secondary = CashInGreen,
                    onSecondary = Color.White,
                    error = CashOutRed,
                    background = SlateBackgroundLight,
                    onBackground = Color(0xFF0F172A),
                    surface = PaperWhite,
                    onSurface = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFFF1F5F9),
                    onSurfaceVariant = Color(0xFF334155),
                    outline = Color(0xFFCBD5E1)
                )
            }
        }

        // 5. BLACK & WHITE MONOCHROME MODE (High-Contrast Pure Ink & Paper)
        AppThemeMode.BLACK_AND_WHITE -> {
            if (isDark) {
                darkColorScheme(
                    primary = MonoPureWhite,
                    onPrimary = MonoPureBlack,
                    primaryContainer = MonoDarkGray,
                    onPrimaryContainer = MonoPureWhite,
                    secondary = MonoLightGray,
                    onSecondary = MonoPureBlack,
                    secondaryContainer = MonoDarkGray,
                    onSecondaryContainer = MonoPureWhite,
                    error = MonoLightGray,
                    onError = MonoPureBlack,
                    background = MonoPureBlack,
                    onBackground = MonoPureWhite,
                    surface = Color(0xFF121212),
                    onSurface = MonoPureWhite,
                    surfaceVariant = Color(0xFF1E1E1E),
                    onSurfaceVariant = MonoLightGray,
                    outline = MonoBorderDark,
                    outlineVariant = MonoMidGray
                )
            } else {
                lightColorScheme(
                    primary = MonoPureBlack,
                    onPrimary = MonoPureWhite,
                    primaryContainer = MonoLightGray,
                    onPrimaryContainer = MonoPureBlack,
                    secondary = MonoDarkGray,
                    onSecondary = MonoPureWhite,
                    secondaryContainer = Color(0xFFF5F5F5),
                    onSecondaryContainer = MonoPureBlack,
                    error = MonoPureBlack,
                    onError = MonoPureWhite,
                    background = MonoPaperWhite,
                    onBackground = MonoPureBlack,
                    surface = MonoPureWhite,
                    onSurface = MonoPureBlack,
                    surfaceVariant = Color(0xFFEBEBEB),
                    onSurfaceVariant = MonoDarkGray,
                    outline = MonoBorder,
                    outlineVariant = MonoMidGray
                )
            }
        }
    }
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 240)
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "onSecondary")
    val background by animateColorAsState(target.background, spec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant")
    val outline by animateColorAsState(target.outline, spec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "outlineVariant")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant
    )
}

// Crisp, utilitarian commercial accounting shapes (4dp-6dp corners, never bubbly)
val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

/**
 * Centralized Theme Provider that manages persistent state for:
 * - Curated Accounting Theme Presets & Monochrome B&W Mode
 * - Dedicated Dark Mode Toggle
 * - Material 3 Dynamic Color (`dynamicDarkColorScheme` / `dynamicLightColorScheme`)
 */
@Composable
fun ProvideShivayTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val prefs = remember(context) {
        context.getSharedPreferences("shivay_cashbook_prefs", Context.MODE_PRIVATE)
    }

    var currentTheme by rememberSaveable {
        val savedName = prefs.getString("theme_mode", AppThemeMode.CLASSIC_NAVY.name)
        val initialMode = AppThemeMode.entries.firstOrNull { it.name == savedName } ?: AppThemeMode.CLASSIC_NAVY
        mutableStateOf(initialMode)
    }
    var appearanceMode by rememberSaveable {
        val savedAppearance = prefs.getString("appearance_mode", null)
        val initialAppearance = if (savedAppearance != null) {
            ThemeAppearanceMode.entries.firstOrNull { it.name == savedAppearance } ?: ThemeAppearanceMode.SYSTEM
        } else if (prefs.contains("is_dark_theme")) {
            if (prefs.getBoolean("is_dark_theme", false)) ThemeAppearanceMode.DARK else ThemeAppearanceMode.LIGHT
        } else {
            ThemeAppearanceMode.SYSTEM
        }
        mutableStateOf(initialAppearance)
    }
    var useDynamicColor by rememberSaveable {
        mutableStateOf(prefs.getBoolean("use_dynamic_color", false))
    }

    val effectiveDarkMode = when (appearanceMode) {
        ThemeAppearanceMode.LIGHT -> false
        ThemeAppearanceMode.DARK -> true
        ThemeAppearanceMode.SYSTEM -> systemDark
    }

    val controller = remember(currentTheme, appearanceMode, effectiveDarkMode, useDynamicColor) {
        ThemeController(
            themeMode = currentTheme,
            appearanceMode = appearanceMode,
            isDarkMode = effectiveDarkMode,
            useDynamicColor = useDynamicColor,
            isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onSelectThemeMode = { mode ->
                currentTheme = mode
                val dynamicFlag = (mode == AppThemeMode.DYNAMIC_WALLPAPER)
                useDynamicColor = dynamicFlag
                prefs.edit()
                    .putString("theme_mode", mode.name)
                    .putBoolean("use_dynamic_color", dynamicFlag)
                    .apply()
            },
            onSelectAppearanceMode = { newAppearance ->
                appearanceMode = newAppearance
                val explicitDark = when (newAppearance) {
                    ThemeAppearanceMode.LIGHT -> false
                    ThemeAppearanceMode.DARK -> true
                    ThemeAppearanceMode.SYSTEM -> systemDark
                }
                prefs.edit()
                    .putString("appearance_mode", newAppearance.name)
                    .putBoolean("is_dark_theme", explicitDark)
                    .apply()
            },
            onToggleDarkMode = { dark ->
                val newAppearance = if (dark) ThemeAppearanceMode.DARK else ThemeAppearanceMode.LIGHT
                appearanceMode = newAppearance
                prefs.edit()
                    .putString("appearance_mode", newAppearance.name)
                    .putBoolean("is_dark_theme", dark)
                    .apply()
            },
            onToggleDynamicColor = { enabled ->
                useDynamicColor = enabled
                if (enabled) {
                    currentTheme = AppThemeMode.DYNAMIC_WALLPAPER
                    prefs.edit()
                        .putBoolean("use_dynamic_color", true)
                        .putString("theme_mode", AppThemeMode.DYNAMIC_WALLPAPER.name)
                        .apply()
                } else {
                    if (currentTheme == AppThemeMode.DYNAMIC_WALLPAPER) {
                        currentTheme = AppThemeMode.CLASSIC_NAVY
                    }
                    prefs.edit()
                        .putBoolean("use_dynamic_color", false)
                        .putString("theme_mode", currentTheme.name)
                        .apply()
                }
            }
        )
    }

    CompositionLocalProvider(LocalThemeController provides controller) {
        MyApplicationTheme(
            themeMode = currentTheme,
            darkTheme = effectiveDarkMode,
            dynamicColor = useDynamicColor,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.CLASSIC_NAVY,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val targetScheme = createLedgerColorScheme(
        context = context,
        themeMode = themeMode,
        isDark = darkTheme,
        useDynamicColor = dynamicColor
    )
    val animatedScheme = animateColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography,
        shapes = LedgerShapes,
        content = content
    )
}
