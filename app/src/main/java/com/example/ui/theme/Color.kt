package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette — Vivid Fintech Canvas (Default)
val LightBackground = Color(0xFFF4F7FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFEDF2FA)
val LightSurfaceHigh = Color(0xFFE2E8F0)
val LightPrimary = Color(0xFF2563EB)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDBEAFE)
val LightOnPrimaryContainer = Color(0xFF1E3A8A)
val LightSecondary = Color(0xFF059669)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD1FAE5)
val LightOnSecondaryContainer = Color(0xFF065F46)
val LightTertiary = Color(0xFF7C3AED)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFEDE9FE)
val LightOnTertiaryContainer = Color(0xFF4C1D95)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFF94A3B8)
val LightOutlineVariant = Color(0xFFE2E8F0)
val LightError = Color(0xFFE11D48)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFE4E6)
val LightOnErrorContainer = Color(0xFF881337)

// Dark Palette — Optional User Toggle
val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF131C31)
val DarkSurfaceLow = Color(0xFF131C31)
val DarkSurfaceContainer = Color(0xFF1A2540)
val DarkSurfaceHigh = Color(0xFF233152)
val DarkPrimary = Color(0xFF60A5FA)
val DarkOnPrimary = Color(0xFF0B1F4B)
val DarkPrimaryContainer = Color(0xFF1E3A8A)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)
val DarkSecondary = Color(0xFF34D399)
val DarkOnSecondary = Color(0xFF022C22)
val DarkSecondaryContainer = Color(0xFF065F46)
val DarkOnSecondaryContainer = Color(0xFFD1FAE5)
val DarkTertiary = Color(0xFFA78BFA)
val DarkOnTertiary = Color(0xFF2E1065)
val DarkTertiaryContainer = Color(0xFF4C1D95)
val DarkOnTertiaryContainer = Color(0xFFEDE9FE)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF64748B)
val DarkOutlineVariant = Color(0xFF293548)
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
    val onHeroCard: Color,
    val onHeroCardMuted: Color,
)

val LightSemanticColors = SpendlySemanticColors(
    income = Color(0xFF059669),
    incomeContainer = Color(0xFFD1FAE5),
    expense = Color(0xFFE11D48),
    expenseContainer = Color(0xFFFFE4E6),
    transfer = Color(0xFF0284C7),
    transferContainer = Color(0xFFE0F2FE),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    violet = Color(0xFF7C3AED),
    violetContainer = Color(0xFFEDE9FE),
    heroCardStart = Color(0xFF1D4ED8),
    heroCardMid = Color(0xFF4F46E5),
    heroCardEnd = Color(0xFF0D9488),
    onHeroCard = Color(0xFFFFFFFF),
    onHeroCardMuted = Color(0xFFDBEAFE),
)

val DarkSemanticColors = SpendlySemanticColors(
    income = Color(0xFF34D399),
    incomeContainer = Color(0xFF064E3B),
    expense = Color(0xFFFB7185),
    expenseContainer = Color(0xFF4C0519),
    transfer = Color(0xFF38BDF8),
    transferContainer = Color(0xFF0C4A6E),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF451A03),
    violet = Color(0xFFA78BFA),
    violetContainer = Color(0xFF2E1065),
    heroCardStart = Color(0xFF1E3A8A),
    heroCardMid = Color(0xFF3730A3),
    heroCardEnd = Color(0xFF115E59),
    onHeroCard = Color(0xFFF8FAFC),
    onHeroCardMuted = Color(0xFFBFDBFE),
)

val LocalSpendlyColors = staticCompositionLocalOf { LightSemanticColors }

// Vibrant Category & Account Accent Helpers
fun categoryColorPair(iconKey: String?): Pair<Color, Color> = when (iconKey) {
    "groceries" -> Color(0xFF059669) to Color(0xFFD1FAE5)
    "dining" -> Color(0xFFEA580C) to Color(0xFFFFEDD5)
    "housing" -> Color(0xFF4F46E5) to Color(0xFFE0E7FF)
    "transport" -> Color(0xFF0284C7) to Color(0xFFE0F2FE)
    "utilities" -> Color(0xFFD97706) to Color(0xFFFEF3C7)
    "shopping" -> Color(0xFFDB2777) to Color(0xFFFCE7F3)
    "health" -> Color(0xFF0D9488) to Color(0xFFCCFBF1)
    "travel" -> Color(0xFF7C3AED) to Color(0xFFEDE9FE)
    "salary" -> Color(0xFF059669) to Color(0xFFD1FAE5)
    "freelance" -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
    "interest" -> Color(0xFF0D9488) to Color(0xFFCCFBF1)
    else -> Color(0xFF4F46E5) to Color(0xFFE0E7FF)
}
