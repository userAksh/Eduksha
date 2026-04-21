package com.dobi.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DobiPrimary,
    primaryContainer = DobiPrimaryContainer,
    background = DobiBackground,
    surface = DobiSurface,
    onPrimary = DobiSurface,
    onPrimaryContainer = DobiPrimary,
    onBackground = DobiTextPrimary,
    onSurface = DobiTextPrimary,
    error = DobiError,
)

private val DarkColorScheme = darkColorScheme(
    primary = DobiPrimaryLight,
    primaryContainer = DobiPrimary,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = DobiSurface,
    onBackground = DobiSurface,
    onSurface = DobiSurface,
    error = DobiError,
)

@Composable
fun DobiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = DobiTypography,
        content = content
    )
}
