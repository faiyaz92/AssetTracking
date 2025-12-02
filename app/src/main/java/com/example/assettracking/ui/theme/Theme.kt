package com.example.assettracking.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = SurfaceCard,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = SurfaceCard,
    secondary = AccentTeal,
    onSecondary = SurfaceDark,
    secondaryContainer = AccentTealLight,
    onSecondaryContainer = SurfaceDark,
    tertiary = SuccessGreen,
    onTertiary = SurfaceCard,
    error = ErrorRed,
    onError = SurfaceCard,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    background = SurfaceDark,
    onBackground = SurfaceCard,
    surface = SurfaceDarkCard,
    onSurface = SurfaceCard,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextLight,
    outline = BorderDark
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceCard,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = SurfaceCard,
    secondary = AccentTeal,
    onSecondary = SurfaceCard,
    secondaryContainer = AccentTealLight,
    onSecondaryContainer = SurfaceDark,
    tertiary = SuccessGreen,
    onTertiary = SurfaceCard,
    error = ErrorRed,
    onError = SurfaceCard,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight
)

@Composable
fun AssetTrackingTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
