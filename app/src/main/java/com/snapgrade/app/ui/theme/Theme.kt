package com.snapgrade.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkNavy,
    primaryContainer = ElectricTeal,
    onPrimaryContainer = WhiteText,
    secondary = AccentPurple,
    onSecondary = WhiteText,
    background = DarkNavy,
    onBackground = WhiteText,
    surface = DeepSlate,
    onSurface = WhiteText,
    surfaceVariant = SlateBorder,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricTeal,
    onPrimary = WhiteText,
    secondary = AccentPurple,
    background = WhiteText,
    surface = SurfaceCardLight,
    onBackground = DarkNavy,
    onSurface = DarkNavy
)

@Composable
fun SnapGradeTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode for high-contrast presentation
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
