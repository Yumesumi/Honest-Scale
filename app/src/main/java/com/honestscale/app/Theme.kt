package com.honestscale.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Forest = Color(0xFF127E4F)
private val ForestContainer = Color(0xFFB7F1CC)
private val OnForestContainer = Color(0xFF00291A)
private val Paper = Color(0xFFF7F7F2)
private val Ink = Color(0xFF1F2A26)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFDDE5DE),
    onSurfaceVariant = Color(0xFF414943),
    outline = Color(0xFF717971),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF86D9A6),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = ForestContainer,
    background = Color(0xFF121513),
    onBackground = Color(0xFFE1E6E1),
    surface = Color(0xFF1A1D1B),
    onSurface = Color(0xFFE1E6E1),
    surfaceVariant = Color(0xFF252B27),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8B948C),
)

@Composable
fun HonestScaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
