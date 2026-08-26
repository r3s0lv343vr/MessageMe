package com.unbound.messageme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
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
    background = Color(0xFF15222B),
    onBackground = Foam,
    surface = Color(0xFF1C2C37),
    onSurface = Foam,
    error = ReminderRed,
    onError = Foam
)

/** Dark-on-foam fields so typed text stays readable on light composer/settings surfaces. */
@Composable
fun readableOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    disabledTextColor = Ink.copy(alpha = 0.55f),
    cursorColor = WaterBlue,
    focusedPlaceholderColor = Ink.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = Ink.copy(alpha = 0.45f),
    focusedLabelColor = Ink,
    unfocusedLabelColor = Ink.copy(alpha = 0.7f),
    focusedBorderColor = WaterBlue,
    unfocusedBorderColor = Ink.copy(alpha = 0.35f),
    focusedContainerColor = Foam,
    unfocusedContainerColor = Foam,
    disabledContainerColor = Foam
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
