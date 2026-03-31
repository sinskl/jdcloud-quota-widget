package com.yi.jdcloud.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JdRed = Color(0xFFE2231A)
private val JdRedDark = Color(0xFFB91C1A)

private val LightColorScheme = lightColorScheme(
    primary = JdRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775653),
    onSecondary = Color.White,
    tertiary = Color(0xFF715B2E),
    onTertiary = Color.White,
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = JdRedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442926),
    tertiary = Color(0xFFE0C38E),
    onTertiary = Color(0xFF3F2E04),
    background = Color(0xFF201A1A),
    onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF201A1A),
    onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun JdCloudTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
