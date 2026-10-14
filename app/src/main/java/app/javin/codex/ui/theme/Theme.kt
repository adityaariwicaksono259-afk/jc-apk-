package app.javin.codex.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.javin.codex.data.session.ThemeMode

data class ThemeTokens(
    val mode: ThemeMode,
    val isNeo: Boolean,
    val bg: Color,
    val bgGradient: List<Color>?,
    val bokeh: Boolean,
    val cardBg: Color,
    val cardBorder: Color,
    val cardBorderWidth: Dp,
    val cornerRadius: Dp,
    val hasShadow: Boolean,
    val shadowColor: Color,
    val shadowOffset: Dp,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentOn: Color,
    val accentSoft: Color,
    val iconBg: Color,
    val iconTint: Color,
    val divider: Color,
    val searchBg: Color,
    val searchBorder: Color,
    val searchText: Color,
    val searchPlaceholder: Color,
    val success: Color,
    val danger: Color,
    val scheme: androidx.compose.material3.ColorScheme
)

private val GlassScheme = darkColorScheme(
    primary = Color(0xFF4A9EFF),
    onPrimary = Color.White,
    secondary = Color(0xFF7F5AF0),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF7FE5),
    onTertiary = Color.White,
    background = Color(0xFF0F0F1E),
    onBackground = Color(0xFFF0F0F5),
    surface = Color(0x14FFFFFF),
    onSurface = Color(0xFFF0F0F5),
    outline = Color(0x26FFFFFF)
)

private val NeoScheme = lightColorScheme(
    primary = Color(0xFFFFD166),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFA0D8B3),
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Color(0xFFE76F51),
    onTertiary = Color.White,
    background = Color(0xFFF5F0E8),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    outline = Color(0xFF1A1A1A)
)

val GlassTokens = ThemeTokens(
    mode = ThemeMode.GLASS, isNeo = false,
    bg = Color(0xFF0F0F1E),
    bgGradient = listOf(Color(0xFF0F0F1E), Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F0F1E)),
    bokeh = true,
    cardBg = Color(0x1FFFFFFF), cardBorder = Color(0x26FFFFFF), cardBorderWidth = 1.dp,
    cornerRadius = 20.dp, hasShadow = false, shadowColor = Color.Transparent, shadowOffset = 0.dp,
    textPrimary = Color(0xFFF0F0F5), textSecondary = Color(0xFFB8B8C8), textMuted = Color(0x80FFFFFF),
    accent = Color(0xFF4A9EFF), accentOn = Color.White, accentSoft = Color(0x334A9EFF),
    iconBg = Color(0x1AFFFFFF), iconTint = Color(0xFF4A9EFF),
    divider = Color(0x0FFFFFFF),
    searchBg = Color(0x0FFFFFFF), searchBorder = Color(0x1AFFFFFF),
    searchText = Color.White, searchPlaceholder = Color(0x66FFFFFF),
    success = Color(0xFF7FFFA0), danger = Color(0xFFFF6B6B),
    scheme = GlassScheme
)

val NeoTokens = ThemeTokens(
    mode = ThemeMode.NEOBRUTALISM, isNeo = true,
    bg = Color(0xFFF5F0E8),
    bgGradient = null,
    bokeh = false,
    cardBg = Color.White, cardBorder = Color(0xFF1A1A1A), cardBorderWidth = 2.5.dp,
    cornerRadius = 14.dp, hasShadow = true, shadowColor = Color(0xFF1A1A1A), shadowOffset = 4.dp,
    textPrimary = Color(0xFF1A1A1A), textSecondary = Color(0xFF3A3A3A), textMuted = Color(0xFF6A6A6A),
    accent = Color(0xFFFFD166), accentOn = Color(0xFF1A1A1A), accentSoft = Color(0xFFFFE9A8),
    iconBg = Color(0xFFA0D8B3), iconTint = Color(0xFF1A1A1A),
    divider = Color(0xFFE0D8C8),
    searchBg = Color.White, searchBorder = Color(0xFF1A1A1A),
    searchText = Color(0xFF1A1A1A), searchPlaceholder = Color(0xFF8A8A8A),
    success = Color(0xFF4CAF50), danger = Color(0xFFE76F51),
    scheme = NeoScheme
)

val LocalThemeTokens = staticCompositionLocalOf { GlassTokens }

@Composable
fun JavinCodexTheme(
    mode: ThemeMode = ThemeMode.GLASS,
    content: @Composable () -> Unit
) {
    val tokens = if (mode == ThemeMode.NEOBRUTALISM) NeoTokens else GlassTokens
    val isDark = !tokens.isNeo
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }
    CompositionLocalProvider(LocalThemeTokens provides tokens) {
        MaterialTheme(colorScheme = tokens.scheme, content = content)
    }
}
