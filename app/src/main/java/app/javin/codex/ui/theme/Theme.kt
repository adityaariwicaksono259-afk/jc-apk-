package app.javin.codex.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================
// JAVIN CODEX THEME — Glassmorphism
// ============================================

// Brand colors
val JavinBlue = Color(0xFF4A9EFF)
val JavinPurple = Color(0xFF7F5AF0)
val JavinPink = Color(0xFFFF7FE5)

// Glass surface colors
val GlassLight = Color(0x1AFFFFFF)       // 10% white
val GlassBorder = Color(0x26FFFFFF)      // 15% white
val GlassDarkBg = Color(0xFF0F0F1E)      // almost black with purple tint
val GlassDarkBg2 = Color(0xFF1A1A2E)

// Dark scheme — glassmorphism base
private val DarkColors = darkColorScheme(
    primary = JavinBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0x334A9EFF),
    onPrimaryContainer = Color.White,

    secondary = JavinPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0x337F5AF0),
    onSecondaryContainer = Color.White,

    tertiary = JavinPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0x33FF7FE5),
    onTertiaryContainer = Color.White,

    background = GlassDarkBg,
    onBackground = Color(0xFFF0F0F5),
    surface = Color(0x14FFFFFF),           // translucent
    onSurface = Color(0xFFF0F0F5),
    surfaceVariant = Color(0x1AFFFFFF),
    onSurfaceVariant = Color(0xFFB8B8C8),

    outline = Color(0x26FFFFFF),
    outlineVariant = Color(0x1AFFFFFF)
)

// Light scheme (opsional)
private val LightColors = lightColorScheme(
    primary = JavinBlue,
    onPrimary = Color.White,
    secondary = JavinPurple,
    onSecondary = Color.White,
    tertiary = JavinPink,
    onTertiary = Color.White,
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xCCFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0x99FFFFFF),
    onSurfaceVariant = Color(0xFF4A4A5A),
    outline = Color(0x22000000),
    outlineVariant = Color(0x11000000)
)

@Composable
fun JavinCodexTheme(
    darkTheme: Boolean = true,  // Default dark untuk glassmorphism
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
