package src.mobile.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spendly Brand Identity — Material 3 Theme Specification
 * Default Appearance: Bright Light Mode ("Vivid Fintech Canvas")
 */
object SpendlyPalette {
    // Primary Brand Colors (Royal Cobalt & Electric Indigo)
    val CobaltPrimary = Color(0xFF2563EB)
    val CobaltDark = Color(0xFF1D4ED8)
    val CobaltLightContainer = Color(0xFFDBEAFE)
    val OnCobaltContainer = Color(0xFF1E3A8A)

    // Secondary Brand Colors (Vivid Emerald Mint)
    val MintSecondary = Color(0xFF059669)
    val MintContainer = Color(0xFFD1FAE5)
    val OnMintContainer = Color(0xFF065F46)

    // Tertiary Brand Colors (Berry Violet & Warm Amber)
    val VioletTertiary = Color(0xFF7C3AED)
    val VioletContainer = Color(0xFFEDE9FE)
    val OnVioletContainer = Color(0xFF4C1D95)

    // Light Surfaces (Default)
    val LightCanvas = Color(0xFFF4F7FC)
    val LightCard = Color(0xFFFFFFFF)
    val LightSurfaceLow = Color(0xFFF8FAFE)
    val LightSurfaceContainer = Color(0xFFEDF2FA)
    val LightSurfaceHigh = Color(0xFFE2E8F0)
    val LightOnSurface = Color(0xFF0F172A)
    val LightOnSurfaceVariant = Color(0xFF475569)
    val LightOutline = Color(0xFF94A3B8)
    val LightOutlineVariant = Color(0xFFE2E8F0)

    // Dark Surfaces (Optional user toggle)
    val DarkCanvas = Color(0xFF0B1120)
    val DarkCard = Color(0xFF131C31)
    val DarkSurfaceLow = Color(0xFF11192C)
    val DarkSurfaceContainer = Color(0xFF1A2540)
    val DarkSurfaceHigh = Color(0xFF233152)
    val DarkOnSurface = Color(0xFFF1F5F9)
    val DarkOnSurfaceVariant = Color(0xFF94A3B8)
    val DarkOutlineVariant = Color(0xFF293548)

    // Semantic Financial Colors
    val IncomeGreen = Color(0xFF10B981)
    val ExpenseCoral = Color(0xFFEF4444)
    val WarningAmber = Color(0xFFF59E0B)
    val TransferSky = Color(0xFF0EA5E9)
}

@Immutable
data class SpendlySemanticPalette(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val accentViolet: Color,
    val accentVioletContainer: Color,
    val heroGradientStart: Color,
    val heroGradientMid: Color,
    val heroGradientEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
)

private val LightSemanticPalette = SpendlySemanticPalette(
    income = Color(0xFF059669),
    incomeContainer = Color(0xFFD1FAE5),
    expense = Color(0xFFE11D48),
    expenseContainer = Color(0xFFFFE4E6),
    transfer = Color(0xFF0284C7),
    transferContainer = Color(0xFFE0F2FE),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFEF3C7),
    accentViolet = Color(0xFF7C3AED),
    accentVioletContainer = Color(0xFFEDE9FE),
    heroGradientStart = Color(0xFF1D4ED8),
    heroGradientMid = Color(0xFF4F46E5),
    heroGradientEnd = Color(0xFF0D9488),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFDBEAFE),
)

private val DarkSemanticPalette = SpendlySemanticPalette(
    income = Color(0xFF34D399),
    incomeContainer = Color(0xFF064E3B),
    expense = Color(0xFFFB7185),
    expenseContainer = Color(0xFF4C0519),
    transfer = Color(0xFF38BDF8),
    transferContainer = Color(0xFF0C4A6E),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF451A03),
    accentViolet = Color(0xFFA78BFA),
    accentVioletContainer = Color(0xFF2E1065),
    heroGradientStart = Color(0xFF1E3A8A),
    heroGradientMid = Color(0xFF3730A3),
    heroGradientEnd = Color(0xFF115E59),
    onHero = Color(0xFFF8FAFC),
    onHeroMuted = Color(0xFFBFDBFE),
)

val LocalSpendlySemanticPalette = staticCompositionLocalOf { LightSemanticPalette }

val SpendlyLightColorScheme = lightColorScheme(
    primary = SpendlyPalette.CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = SpendlyPalette.CobaltLightContainer,
    onPrimaryContainer = SpendlyPalette.OnCobaltContainer,
    secondary = SpendlyPalette.MintSecondary,
    onSecondary = Color.White,
    secondaryContainer = SpendlyPalette.MintContainer,
    onSecondaryContainer = SpendlyPalette.OnMintContainer,
    tertiary = SpendlyPalette.VioletTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SpendlyPalette.VioletContainer,
    onTertiaryContainer = SpendlyPalette.OnVioletContainer,
    background = SpendlyPalette.LightCanvas,
    onBackground = SpendlyPalette.LightOnSurface,
    surface = SpendlyPalette.LightCard,
    onSurface = SpendlyPalette.LightOnSurface,
    surfaceVariant = SpendlyPalette.LightSurfaceContainer,
    onSurfaceVariant = SpendlyPalette.LightOnSurfaceVariant,
    surfaceContainerLowest = SpendlyPalette.LightCard,
    surfaceContainerLow = SpendlyPalette.LightCard,
    surfaceContainer = SpendlyPalette.LightSurfaceContainer,
    surfaceContainerHigh = SpendlyPalette.LightSurfaceHigh,
    outline = SpendlyPalette.LightOutline,
    outlineVariant = SpendlyPalette.LightOutlineVariant,
    error = SpendlyPalette.ExpenseCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
)

val SpendlyDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0B1F4B),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = SpendlyPalette.DarkCanvas,
    onBackground = SpendlyPalette.DarkOnSurface,
    surface = SpendlyPalette.DarkCard,
    onSurface = SpendlyPalette.DarkOnSurface,
    surfaceVariant = SpendlyPalette.DarkSurfaceContainer,
    onSurfaceVariant = SpendlyPalette.DarkOnSurfaceVariant,
    surfaceContainerLowest = SpendlyPalette.DarkCanvas,
    surfaceContainerLow = SpendlyPalette.DarkCard,
    surfaceContainer = SpendlyPalette.DarkSurfaceContainer,
    surfaceContainerHigh = SpendlyPalette.DarkSurfaceHigh,
    outlineVariant = SpendlyPalette.DarkOutlineVariant,
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519),
)

val SpendlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val SpendlyBrandTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontFeatureSettings = "tnum",
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
)

object SpendlyBrandTheme {
    val semantic: SpendlySemanticPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSpendlySemanticPalette.current
}

/**
 * Defaults strictly to Light Mode (`darkTheme = false`) for crisp daytime financial clarity.
 */
@Composable
fun SpendlyMaterialTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SpendlyDarkColorScheme else SpendlyLightColorScheme
    val semantic = if (darkTheme) DarkSemanticPalette else LightSemanticPalette

    CompositionLocalProvider(LocalSpendlySemanticPalette provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpendlyBrandTypography,
            shapes = SpendlyShapes,
            content = content,
        )
    }
}
