package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val positive: Color,
    val positiveContainer: Color,
    val onPositiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val negative: Color,
    val negativeContainer: Color,
    val onNegativeContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val aguinaldoStart: Color,
    val aguinaldoEnd: Color,
    val cardBorder: Color
)

private val LightFinanceColors = FinanceColors(
    positive = Emerald600,
    positiveContainer = Emerald100,
    onPositiveContainer = Emerald900,
    warning = Amber600,
    warningContainer = Amber100,
    onWarningContainer = Amber800,
    negative = Rose600,
    negativeContainer = Rose100,
    onNegativeContainer = Rose700,
    info = TealInfo,
    infoContainer = TealInfoContainerLight,
    onInfoContainer = Color(0xFF075985),
    heroStart = Emerald900,
    heroEnd = Color(0xFF0F766E),
    aguinaldoStart = Color(0xFF78350F),
    aguinaldoEnd = Color(0xFFB45309),
    cardBorder = OutlineLight
)

private val DarkFinanceColors = FinanceColors(
    positive = Emerald300,
    positiveContainer = Color(0xFF0A3D2E),
    onPositiveContainer = Emerald100,
    warning = Amber200,
    warningContainer = Color(0xFF452707),
    onWarningContainer = Amber200,
    negative = Color(0xFFFB7185),
    negativeContainer = Rose900,
    onNegativeContainer = Rose100,
    info = Color(0xFF38BDF8),
    infoContainer = TealInfoContainerDark,
    onInfoContainer = Color(0xFFBAE6FD),
    heroStart = Emerald950,
    heroEnd = Emerald800,
    aguinaldoStart = Color(0xFF451A03),
    aguinaldoEnd = Color(0xFF92400E),
    cardBorder = OutlineDark
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

private val LightColorScheme = lightColorScheme(
    primary = Emerald900,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    secondary = Amber600,
    onSecondary = Color.White,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber800,
    tertiary = TealInfo,
    onTertiary = Color.White,
    tertiaryContainer = TealInfoContainerLight,
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = CanvasLight,
    onBackground = InkPrimaryLight,
    surface = SurfaceLight,
    onSurface = InkPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkSecondaryLight,
    outline = OutlineLight,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose100,
    onErrorContainer = Rose700
)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald300,
    onPrimary = Emerald950,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald50,
    secondary = Amber200,
    onSecondary = Color(0xFF3B1D00),
    secondaryContainer = Color(0xFF5C3304),
    onSecondaryContainer = Amber100,
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF062C43),
    tertiaryContainer = TealInfoContainerDark,
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = CanvasDark,
    onBackground = InkPrimaryDark,
    surface = SurfaceDark,
    onSurface = InkPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = InkSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFFB7185),
    onError = Rose900,
    errorContainer = Rose900,
    onErrorContainer = Rose100
)

@Composable
fun MiQuincenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
