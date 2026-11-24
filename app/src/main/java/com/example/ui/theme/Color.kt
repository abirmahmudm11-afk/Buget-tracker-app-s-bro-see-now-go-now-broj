package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette — Nordic Paper & Deep Spruce (Architectural Financial Precision)
val LightBackground = Color(0xFFF4F4F0)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFECECE7)
val LightSurfaceContainer = Color(0xFFE4E4DE)
val LightSurfaceHigh = Color(0xFFDBDBD4)

val LightPrimary = Color(0xFF0F5C4D)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDCEEE9)
val LightOnPrimaryContainer = Color(0xFF0A3A30)

val LightSecondary = Color(0xFF284B63)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDCE6EE)
val LightOnSecondaryContainer = Color(0xFF162C3C)

val LightTertiary = Color(0xFF1F637A)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFD9EDF4)
val LightOnTertiaryContainer = Color(0xFF0D3A49)

val LightOnSurface = Color(0xFF141716)
val LightOnSurfaceVariant = Color(0xFF535B58)
val LightOutline = Color(0xFF838C88)
val LightOutlineVariant = Color(0xFFE0E0DA)

val LightError = Color(0xFFB42334)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFCE6E9)
val LightOnErrorContainer = Color(0xFF6B101B)

// Dark Palette — Composed Nordic Slate & Sage Mint
val DarkBackground = Color(0xFF0E1110)
val DarkSurface = Color(0xFF161A19)
val DarkSurfaceLow = Color(0xFF1D2220)
val DarkSurfaceContainer = Color(0xFF242A27)
val DarkSurfaceHigh = Color(0xFF2D3431)

val DarkPrimary = Color(0xFF4CC2A5)
val DarkOnPrimary = Color(0xFF06261E)
val DarkPrimaryContainer = Color(0xFF173D34)
val DarkOnPrimaryContainer = Color(0xFFBCECE0)

val DarkSecondary = Color(0xFF8FB4D4)
val DarkOnSecondary = Color(0xFF0C2336)
val DarkSecondaryContainer = Color(0xFF1D3448)
val DarkOnSecondaryContainer = Color(0xFFD3E6F6)

val DarkTertiary = Color(0xFF72C0D8)
val DarkOnTertiary = Color(0xFF072936)
val DarkTertiaryContainer = Color(0xFF163846)
val DarkOnTertiaryContainer = Color(0xFFCBEBF5)

val DarkOnSurface = Color(0xFFECEFEB)
val DarkOnSurfaceVariant = Color(0xFFA0A9A5)
val DarkOutline = Color(0xFF56605C)
val DarkOutlineVariant = Color(0xFF262C2A)

val DarkError = Color(0xFFF27C89)
val DarkOnError = Color(0xFF470C13)
val DarkErrorContainer = Color(0xFF471B21)
val DarkOnErrorContainer = Color(0xFFFFDADE)

@Immutable
data class SpendlySemanticColors(
    val isDark: Boolean,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val slateAccent: Color,
    val slateContainer: Color,
    val heroSurface: Color,
    val heroBorder: Color,
    val heroAccent: Color,
    val heroWarn: Color,
    val onHeroSurface: Color,
    val onHeroSurfaceMuted: Color,
    val onHeroWell: Color,
    val surfaceBorder: Color,
)

val LightSemanticColors = SpendlySemanticColors(
    isDark = false,
    income = Color(0xFF0D6E4E),
    incomeContainer = Color(0xFFDCF3EA),
    expense = Color(0xFFB42334),
    expenseContainer = Color(0xFFFCE6E9),
    transfer = Color(0xFF1D5A8A),
    transferContainer = Color(0xFFE1EFFA),
    warning = Color(0xFF9A5B00),
    warningContainer = Color(0xFFFDF0D5),
    slateAccent = Color(0xFF4A4E69),
    slateContainer = Color(0xFFEAEBF2),
    heroSurface = Color(0xFF102A24),
    heroBorder = Color(0xFF1E473D),
    heroAccent = Color(0xFF66DFB9),
    heroWarn = Color(0xFFF4C05A),
    onHeroSurface = Color(0xFFF7FAF9),
    onHeroSurfaceMuted = Color(0xFFB5C9C3),
    onHeroWell = Color(0xFF183830),
    surfaceBorder = Color(0xFFE0E0DA),
)

val DarkSemanticColors = SpendlySemanticColors(
    isDark = true,
    income = Color(0xFF52D1A2),
    incomeContainer = Color(0xFF143A2C),
    expense = Color(0xFFF27C89),
    expenseContainer = Color(0xFF451C22),
    transfer = Color(0xFF6AB3E8),
    transferContainer = Color(0xFF163348),
    warning = Color(0xFFEBB04D),
    warningContainer = Color(0xFF3F2D12),
    slateAccent = Color(0xFFB4B9D6),
    slateContainer = Color(0xFF282B3A),
    heroSurface = Color(0xFF182421),
    heroBorder = Color(0xFF283A35),
    heroAccent = Color(0xFF5AD6B3),
    heroWarn = Color(0xFFEBB04D),
    onHeroSurface = Color(0xFFF2F6F4),
    onHeroSurfaceMuted = Color(0xFFA2B3AE),
    onHeroWell = Color(0xFF20302B),
    surfaceBorder = Color(0xFF262C2A),
)

val LocalSpendlyColors = staticCompositionLocalOf { LightSemanticColors }

// Restrained, high-contrast category icon tint & tonal container pairs (theme-aware)
fun categoryColorPair(iconKey: String?, isDark: Boolean = false): Pair<Color, Color> {
    if (isDark) {
        return when (iconKey) {
            "groceries" -> Color(0xFF52D1A2) to Color(0xFF143A2C)
            "dining" -> Color(0xFFF09A6A) to Color(0xFF422719)
            "housing" -> Color(0xFF7CB4E8) to Color(0xFF1A3147)
            "transport" -> Color(0xFF67C9D6) to Color(0xFF15373D)
            "utilities" -> Color(0xFFEBB04D) to Color(0xFF3E2D12)
            "shopping" -> Color(0xFFF27C89) to Color(0xFF451C22)
            "health" -> Color(0xFF59CFC2) to Color(0xFF143935)
            "travel" -> Color(0xFFB4A7EB) to Color(0xFF2B2547)
            "salary" -> Color(0xFF52D1A2) to Color(0xFF143A2C)
            "freelance" -> Color(0xFF7EAEE6) to Color(0xFF1A2F4A)
            "interest" -> Color(0xFFEBB04D) to Color(0xFF3E2D12)
            else -> Color(0xFF5AD6B3) to Color(0xFF173D34)
        }
    }
    return when (iconKey) {
        "groceries" -> Color(0xFF0D6E4E) to Color(0xFFDCF3EA)
        "dining" -> Color(0xFFB54718) to Color(0xFFFCE9DF)
        "housing" -> Color(0xFF1D4E89) to Color(0xFFE0EAF6)
        "transport" -> Color(0xFF156778) to Color(0xFFDDF0F4)
        "utilities" -> Color(0xFF9A5B00) to Color(0xFFFDF0D5)
        "shopping" -> Color(0xFFB42334) to Color(0xFFFCE6E9)
        "health" -> Color(0xFF0E6B64) to Color(0xFFDAF2EF)
        "travel" -> Color(0xFF4F4698) to Color(0xFFEAE8F8)
        "salary" -> Color(0xFF0D6E4E) to Color(0xFFDCF3EA)
        "freelance" -> Color(0xFF21528C) to Color(0xFFE2ECF8)
        "interest" -> Color(0xFF9A5B00) to Color(0xFFFDF0D5)
        else -> Color(0xFF0F5C4D) to Color(0xFFDCEEE9)
    }
}
