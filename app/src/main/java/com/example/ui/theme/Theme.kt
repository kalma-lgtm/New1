package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IspDarkPrimary,
    onPrimary = IspDarkOnPrimary,
    primaryContainer = IspDarkPrimaryContainer,
    secondary = IspCyanSecondary,
    onSecondary = IspCyanOnSecondary,
    background = IspDarkBackground,
    surface = IspDarkSurface,
    surfaceVariant = IspDarkSurfaceVariant,
    onBackground = IspDarkOnSurface,
    onSurface = IspDarkOnSurface,
    onSurfaceVariant = IspDarkOnSurfaceVariant,
    outline = IspDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = IspBluePrimary,
    onPrimary = IspBlueOnPrimary,
    primaryContainer = IspBluePrimaryContainer,
    onPrimaryContainer = IspBlueOnPrimaryContainer,
    secondary = IspCyanSecondary,
    onSecondary = IspCyanOnSecondary,
    secondaryContainer = IspCyanContainer,
    onSecondaryContainer = IspCyanOnContainer,
    tertiary = IspTealTertiary,
    onTertiary = IspTealOnTertiary,
    background = IspLightBackground,
    surface = IspLightSurface,
    surfaceVariant = IspLightSurfaceVariant,
    onBackground = IspLightOnSurface,
    onSurface = IspLightOnSurface,
    onSurfaceVariant = IspLightOnSurfaceVariant,
    outline = IspLightOutline
)

@Composable
fun NetPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
