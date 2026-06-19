package ee.kuller.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand palette — courier-style green with a cyan accent
val BrandGreen = Color(0xFF0B7A4B)
val BrandGreenDark = Color(0xFF075C39)
val AccentCyan = Color(0xFF00B4D8)
val Sun = Color(0xFFFFB703)
val Coral = Color(0xFFE5484D)
val Ink = Color(0xFF1A1C1A)
val Cloud = Color(0xFFF3F5F4)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFDC),
    onPrimaryContainer = BrandGreenDark,
    secondary = AccentCyan,
    onSecondary = Color.White,
    tertiary = Sun,
    error = Coral,
    background = Cloud,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD89A),
    onPrimary = Color(0xFF00351E),
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = Color(0xFFCDEFDC),
    secondary = AccentCyan,
    tertiary = Sun,
    error = Coral,
    background = Color(0xFF101411),
    onBackground = Cloud,
    surface = Color(0xFF1A201C),
    onSurface = Cloud,
)

@Composable
fun KullerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
