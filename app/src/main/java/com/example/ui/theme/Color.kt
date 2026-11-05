package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// Shivay CashBook - Multi-Theme & Monochrome Color Definitions
// Zero AI generic gradients - authentic human-designed accounting tones
// =====================================================================

// Common Accounting Functional Tones (Cash In Credit / Cash Out Debit)
val CashInGreen = Color(0xFF15803D)
val CashInGreenDark = Color(0xFF22C55E)
val CashInContainerLight = Color(0xFFDCFCE7)
val CashInContainerDark = Color(0xFF064E3B)

val CashOutRed = Color(0xFFB91C1C)
val CashOutRedDark = Color(0xFFF87171)
val CashOutContainerLight = Color(0xFFFEE2E2)
val CashOutContainerDark = Color(0xFF7F1D1D)

val AmberWarning = Color(0xFFB45309)
val AmberContainerLight = Color(0xFFFEF3C7)

// Neutral Accounting Surfaces & Papers
val PaperWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)
val LedgerBorderLight = Color(0xFFCBD5E1)
val LedgerBorderDark = Color(0xFF334155)

// ---------------------------------------------------------------------
// 1. CLASSIC NAVY THEME (Traditional Banking & Tally Look)
// ---------------------------------------------------------------------
val NavyPrimary = Color(0xFF1E3A5F)
val NavyDark = Color(0xFF102338)
val NavyContainerLight = Color(0xFFDCE6F2)
val NavyContainerDark = Color(0xFF1E3A5F)
val NavyBackgroundLight = Color(0xFFF3F5F8)
val NavyBackgroundDark = Color(0xFF0B111B)
val NavySurfaceDark = Color(0xFF131C2B)

// ---------------------------------------------------------------------
// 2. EMERALD MERCHANT THEME (Prosperity, Indian Vyapar & Forest Green)
// ---------------------------------------------------------------------
val EmeraldPrimary = Color(0xFF047857)
val EmeraldDark = Color(0xFF064E3B)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val EmeraldContainerDark = Color(0xFF065F46)
val EmeraldBackgroundLight = Color(0xFFF0FDF4)
val EmeraldBackgroundDark = Color(0xFF061B12)
val EmeraldSurfaceDark = Color(0xFF0B291D)

// ---------------------------------------------------------------------
// 3. ROYAL BURGUNDY THEME (Heritage Bahi-Khata Deep Maroon)
// ---------------------------------------------------------------------
val BurgundyPrimary = Color(0xFF881337)
val BurgundyDark = Color(0xFF4C0519)
val BurgundyContainerLight = Color(0xFFFFE4E6)
val BurgundyContainerDark = Color(0xFF701A75)
val BurgundyBackgroundLight = Color(0xFFFFF1F2)
val BurgundyBackgroundDark = Color(0xFF1A070D)
val BurgundySurfaceDark = Color(0xFF280B14)

// ---------------------------------------------------------------------
// 4. STEEL SLATE THEME (Executive Corporate Charcoal & Cool Cyan)
// ---------------------------------------------------------------------
val SlatePrimary = Color(0xFF334155)
val SlateDark = Color(0xFF0F172A)
val SlateContainerLight = Color(0xFFE2E8F0)
val SlateContainerDark = Color(0xFF1E293B)
val SlateBackgroundLight = Color(0xFFF8FAFC)
val SlateBackgroundDark = Color(0xFF090D16)
val SlateSurfaceDark = Color(0xFF141A29)

// ---------------------------------------------------------------------
// 5. BLACK & WHITE MONOCHROME MODE (Pure High-Contrast E-Ink / Paper)
// ---------------------------------------------------------------------
val MonoPureBlack = Color(0xFF000000)
val MonoPureWhite = Color(0xFFFFFFFF)
val MonoDarkGray = Color(0xFF262626)
val MonoMidGray = Color(0xFF525252)
val MonoLightGray = Color(0xFFE5E5E5)
val MonoPaperWhite = Color(0xFFFAFAFA)
val MonoBorder = Color(0xFF171717)
val MonoBorderDark = Color(0xFFE5E5E5)

// Enum for App Theme Selection
enum class AppThemeMode(val displayName: String, val hindiName: String, val description: String) {
    CLASSIC_NAVY("Classic Navy", "क्लासिक नेवी", "Banking & Tally Blue"),
    EMERALD_GREEN("Emerald Merchant", "मर्चेंट ग्रीन", "Vyapar & Business Prosperity"),
    ROYAL_BURGUNDY("Royal Burgundy", "बही-खाता महरून", "Traditional Bahi-Khata Red"),
    STEEL_SLATE("Steel Slate", "एग्जीक्यूटिव स्लेट", "Corporate Slate Gray"),
    BLACK_AND_WHITE("Monochrome B&W", "ब्लैक एंड व्हाइट", "Pure High-Contrast Paper & E-Ink"),
    DYNAMIC_WALLPAPER("Material 3 Dynamic", "डायनामिक कलर", "Android 12+ Material You Wallpaper Palette")
}

// Enum for Display Appearance Mode (Light, Dark, System Default)
enum class ThemeAppearanceMode(val displayName: String, val hindiName: String, val description: String) {
    LIGHT("Light", "लाइट मोड", "Daylight paper register surface"),
    DARK("Dark", "डार्क मोड", "High-contrast dark ledger surface"),
    SYSTEM("System", "सिस्टम मोड", "Automatically follows device Dark/Light setting")
}

