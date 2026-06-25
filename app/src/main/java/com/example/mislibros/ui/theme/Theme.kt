package com.example.mislibros.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = YaleBlue,
    secondary = AirForceBlue,
    tertiary = Mindaro,
    background = LightBackground,
    surface = SurfaceColor,
    onBackground = OnBackgroundText,
    onSurface = OnSurfaceText
)

@Composable
fun MisLibrosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}