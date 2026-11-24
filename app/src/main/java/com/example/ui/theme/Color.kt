package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette — Warm Alabaster & Deep Forest Emerald (Editorial Fintech)
val LightBackground = Color(0xFFF6F4ED)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFFAF8F3)
val LightSurfaceContainer = Color(0xFFEDEAE0)
val LightSurfaceHigh = Color(0xFFE2DFD3)

val LightPrimary = Color(0xFF0B4F3E)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD6EFE3)
val LightOnPrimaryContainer = Color(0xFF063327)

val LightSecondary = Color(0xFFB86E00)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFEF1D1)
val LightOnSecondaryContainer = Color(0xFF543100)

val LightTertiary = Color(0xFF0E7490)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFD9F3FA)
val LightOnTertiaryContainer = Color(0xFF073E4E)

val LightOnSurface = Color(0xFF141D1A)
val LightOnSurfaceVariant = Color(0xFF596560)
val LightOutline = Color(0xFF9AA5A0)
val LightOutlineVariant = Color(0xFFE6E3D8)

val LightError = Color(0xFFC92A3E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFE5E8)
val LightOnErrorContainer = Color(0xFF6B0F1B)

// Dark Palette — Obsidian Pine & Luminous Mint
val DarkBackground = Color(0xFF0A0F0D)
val DarkSurface = Color(0xFF131B18)
val DarkSurfaceLow = Color(0xFF16201C)
val DarkSurfaceContainer = Color(0xFF1C2823)
val DarkSurfaceHigh = Color(0xFF24332D)

val DarkPrimary = Color(0xFF4ADEA8)
val DarkOnPrimary = Color(0xFF022B1F)
val DarkPrimaryContainer = Color(0xFF0D3D2E)
val DarkOnPrimaryContainer = Color(0xFFC9F8E5)

val DarkSecondary = Color(0xFFF5B942)
val DarkOnSecondary = Color(0xFF3A2500)
val DarkSecondaryContainer = Color(0xFF473004)
val DarkOnSecondaryContainer = Color(0xFFFDE68A)

val DarkTertiary = Color(0xFF38BDF8)
val DarkOnTertiary = Color(0xFF042A3A)
val DarkTertiaryContainer = Color(0xFF0D3C52)
val DarkOnTertiaryContainer = Color(0xFFD9F3FA)

val DarkOnSurface = Color(0xFFF2F5F3)
val DarkOnSurfaceVariant = Color(0xFF9BA8A2)
val DarkOutline = Color(0xFF5D6B65)
val DarkOutlineVariant = Color(0xFF23302A)

val DarkError = Color(0xFFFB7185)
val DarkOnError = Color(0xFF4C0519)
val DarkErrorContainer = Color(0xFF5C1024)
val DarkOnErrorContainer = Color(0xFFFFE4E6)

@Immutable
data class SpendlySemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val violet: Color,
    val violetContainer: Color,
    val heroCardStart: Color,
    val heroCardMid: Color,
    val heroCardEnd: Color,
    val heroAccent: Color,
    val heroGold: Color,
    val onHeroCard: Color,
    val onHeroCardMuted: Color,
)

val LightSemanticColors = SpendlySemanticColors(
    income = Color(0xFF0B7A53),
    incomeContainer = Color(0xFFD8F3E8),
    expense = Color(0xFFC92A3E),
    expenseContainer = Color(0xFFFFE5E8),
    transfer = Color(0xFF0E7490),
    transferContainer = Color(0xFFD9F3FA),
    warning = Color(0xFFB86E00),
    warningContainer = Color(0xFFFEF1D1),
    violet = Color(0xFF6B46C1),
    violetContainer = Color(0xFFEDE7FA),
    heroCardStart = Color(0xFF072E24),
    heroCardMid = Color(0xFF0B4F3E),
    heroCardEnd = Color(0xFF136A53),
    heroAccent = Color(0xFF6EE7B7),
    heroGold = Color(0xFFF5B942),
    onHeroCard = Color(0xFFFFFFFF),
    onHeroCardMuted = Color(0xFFBFEAD8),
)

val DarkSemanticColors = SpendlySemanticColors(
    income = Color(0xFF4ADEA8),
    incomeContainer = Color(0xFF0D3B2C),
    expense = Color(0xFFFB7185),
    expenseContainer = Color(0xFF4C0F1C),
    transfer = Color(0xFF38BDF8),
    transferContainer = Color(0xFF0D384C),
    warning = Color(0xFFF5B942),
    warningContainer = Color(0xFF452C04),
    violet = Color(0xFFA78BFA),
    violetContainer = Color(0xFF2D1E5F),
    heroCardStart = Color(0xFF0E261F),
    heroCardMid = Color(0xFF143C31),
    heroCardEnd = Color(0xFF1A4F40),
    heroAccent = Color(0xFF4ADEA8),
    heroGold = Color(0xFFF5B942),
    onHeroCard = Color(0xFFF4F8F6),
    onHeroCardMuted = Color(0xFF9AD9C0),
)

val LocalSpendlyColors = staticCompositionLocalOf { LightSemanticColors }

// Editorial Category & Account Accent Helpers (Harmonized with Forest Emerald & Warm Ivory)
fun categoryColorPair(iconKey: String?): Pair<Color, Color> = when (iconKey) {
    "groceries" -> Color(0xFF0B7A53) to Color(0xFFD8F3E8)
    "dining" -> Color(0xFFC2590A) to Color(0xFFFFEAD8)
    "housing" -> Color(0xFF0B4F3E) to Color(0xFFD6EFE3)
    "transport" -> Color(0xFF0E7490) to Color(0xFFD9F3FA)
    "utilities" -> Color(0xFFB86E00) to Color(0xFFFEF1D1)
    "shopping" -> Color(0xFFC92A54) to Color(0xFFFFE5EE)
    "health" -> Color(0xFF0F766E) to Color(0xFFD2F6F1)
    "travel" -> Color(0xFF6B46C1) to Color(0xFFEDE7FA)
    "salary" -> Color(0xFF0B7A53) to Color(0xFFD8F3E8)
    "freelance" -> Color(0xFF1D6B87) to Color(0xFFDDF1F8)
    "interest" -> Color(0xFFB86E00) to Color(0xFFFEF1D1)
    else -> Color(0xFF0B4F3E) to Color(0xFFD6EFE3)
}
