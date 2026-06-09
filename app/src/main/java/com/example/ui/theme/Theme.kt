package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Since the brand targets "Minimalist Luxury" centered on Off-White #F9F9F9,
// we will maintain this premium and clean look.
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLime,
    onPrimary = Color.White,
    secondary = SecondaryCharcoal,
    onSecondary = Color.White,
    tertiary = AccentPillLight,
    onTertiary = SecondaryCharcoal,
    background = Background,
    onBackground = SecondaryCharcoal,
    surface = CardWhite,
    onSurface = SecondaryCharcoal,
    surfaceVariant = LightGray,
    onSurfaceVariant = SecondaryCharcoal
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLime,
    onPrimary = Color.White,
    secondary = CardWhite,
    onSecondary = SecondaryCharcoal,
    tertiary = AccentPillLight,
    onTertiary = SecondaryCharcoal,
    background = SecondaryCharcoal,
    onBackground = Background,
    surface = Color(0xFF222222),
    onSurface = Background,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Background
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Keep a premium, highly-branded minimalist look
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
