package com.busfacultativo.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VerdePrimario,
    secondary = AzulRecarga,
    tertiary = VerdeSuave,
    background = GrisOscuro,
    surface = GrisOscuro,
    onPrimary = BlancoFondo,
    onSecondary = BlancoFondo,
    error = RojoError
)

private val LightColorScheme = lightColorScheme(
    primary = VerdePrimario,
    secondary = AzulRecarga,
    tertiary = VerdeSuave,
    background = BlancoFondo,
    surface = BlancoFondo,
    onPrimary = BlancoFondo,
    onSecondary = BlancoFondo,
    onBackground = GrisOscuro,
    onSurface = GrisOscuro,
    error = RojoError
)

@Composable
fun BusFacultativoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.primary.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
