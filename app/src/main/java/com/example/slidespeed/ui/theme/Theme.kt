package com.example.slidespeed.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SpeedBlue,
    secondary = SpeedOrange,
    background = SpeedCream,
    surface = SpeedCream,
    onPrimary = SpeedCream,
    onSecondary = SpeedInk,
    onBackground = SpeedInk,
    onSurface = SpeedInk,
)

private val DarkColors = darkColorScheme(
    primary = SpeedOrange,
    secondary = SpeedBlue,
    background = SpeedInk,
    surface = SpeedInk,
    onPrimary = SpeedInk,
    onSecondary = SpeedCream,
    onBackground = SpeedCream,
    onSurface = SpeedCream,
)

@Composable
fun SlideSpeedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}

