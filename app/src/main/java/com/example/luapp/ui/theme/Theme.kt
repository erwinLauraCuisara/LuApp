package com.example.luapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Rose40,
    onPrimary = Color.White,
    primaryContainer = Rose90,
    onPrimaryContainer = Rose10,
    secondary = Mauve40,
    onSecondary = Color.White,
    secondaryContainer = Mauve90,
    onSecondaryContainer = Mauve10,
    tertiary = Gold40,
    onTertiary = Color.White,
    tertiaryContainer = Gold90,
    onTertiaryContainer = Gold10,
    background = NeutralLight,
    onBackground = NeutralDark,
    surface = NeutralLight,
    onSurface = NeutralDark,
    surfaceVariant = NeutralVariantLight,
    onSurfaceVariant = NeutralVariantDark,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Rose80,
    onPrimary = Color(0xFF5E1131),
    primaryContainer = Color(0xFF7B2942),
    onPrimaryContainer = Rose90,
    secondary = Mauve80,
    onSecondary = Color(0xFF43292D),
    secondaryContainer = Color(0xFF5C3F43),
    onSecondaryContainer = Mauve90,
    tertiary = Gold80,
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5D4200),
    onTertiaryContainer = Gold90,
    background = NeutralDark,
    onBackground = Color(0xFFEBE0E1),
    surface = NeutralDark,
    onSurface = Color(0xFFEBE0E1),
    surfaceVariant = NeutralVariantDark,
    onSurfaceVariant = Color(0xFFD5C2C6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun LuAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
