package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ArcadePrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF80F3FF),
    secondary = ArcadeSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A0027),
    onSecondaryContainer = Color(0xFFFFB2D1),
    tertiary = ArcadeTertiary,
    onTertiary = Color.Black,
    background = ArcadeBackground,
    onBackground = TextPrimary,
    surface = ArcadeSurface,
    onSurface = TextPrimary,
    surfaceVariant = ArcadeSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun BrickOutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
