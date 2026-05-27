package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkSlatePrimary,
    secondary = DarkSlateSecondary,
    tertiary = GoldenPeachTertiary,
    background = DarkBrownBackground,
    surface = DarkBrownSurface,
    onPrimary = Color(0xFF4A1C00),
    onSecondary = Color(0xFF4A1C00),
    onBackground = Color(0xFFFBEFE3),
    onSurface = Color(0xFFFBEFE3),
)

private val LightColorScheme = lightColorScheme(
    primary = WarmAmberPrimary,
    secondary = ApricotOrangeSecondary,
    tertiary = GoldenPeachTertiary,
    background = CozyWhiteBackground,
    surface = WarmSurfaceCream,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF2E1905),
    onSurface = Color(0xFF4E2C0C),
    surfaceVariant = SoftRoseAccent,
    outline = BorderCream
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure our gorgeous custom warm/cozy colors always take precedence!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
