package com.unbound.messageme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = WaterBlue,
    onPrimary = Foam,
    secondary = AccentOrange,
    onSecondary = Ink,
    tertiary = PastelYellow,
    onTertiary = Ink,
    background = SoftSky,
    onBackground = Ink,
    surface = Foam,
    onSurface = Ink,
    error = ReminderRed,
    onError = Foam
)

private val DarkColors = darkColorScheme(
    primary = WaterBlue,
    onPrimary = Foam,
    secondary = AccentOrange,
    onSecondary = Ink,
    tertiary = PastelYellow,
    onTertiary = Ink,
    background = Color(0xFF0B1F4A),
    onBackground = Foam,
    surface = Color(0xFF1C2C37),
    onSurface = Foam,
    error = ReminderRed,
    onError = Foam
)

@Composable
fun MessageMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MessageMeTypography,
        content = content
    )
}
