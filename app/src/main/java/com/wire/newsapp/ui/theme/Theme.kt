package com.wire.newsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = IOSBlueDark,
    onPrimary = Color.White,
    secondary = IOSOrangeDark,
    onSecondary = Color.Black,
    tertiary = IOSGreenDark,
    onTertiary = Color.Black,
    background = IOSGroupedBackgroundDark,
    onBackground = Color.White,
    surface = IOSSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = IOSFillDark,
    onSurfaceVariant = Color(0xFF98989F),
    surfaceContainer = IOSSurfaceDark,
    surfaceContainerHigh = IOSElevatedDark,
    surfaceContainerHighest = IOSElevatedDark,
    outline = Color(0xFF38383A),
    outlineVariant = Color(0x1F98989F),
    error = IOSRedDark,
    onError = Color.Black
)

private val LightColors = lightColorScheme(
    primary = IOSBlueLight,
    onPrimary = Color.White,
    secondary = IOSOrangeLight,
    onSecondary = Color.White,
    tertiary = IOSGreenLight,
    onTertiary = Color.White,
    background = IOSGroupedBackgroundLight,
    onBackground = Color(0xFF1C1C1E),
    surface = IOSSurfaceLight,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = IOSFillLight,
    onSurfaceVariant = Color(0x6B3C3C43),
    surfaceContainer = IOSSurfaceLight,
    surfaceContainerHigh = IOSFillLightSecondary,
    surfaceContainerHighest = IOSFillLight,
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0x143C3C43),
    error = IOSRedLight,
    onError = Color.White
)

/**
 * @param themeMode one of "system", "light", "dark" (persisted in DataStore).
 */
@Composable
fun WireTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WireTypography,
        shapes = WireShapes,
        content = content
    )
}
