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
import kotlin.math.absoluteValue

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
    val neoPalette: List<Color> = emptyList(),
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

val LegacyGlassTokens = ThemeTokens(
    mode = ThemeMode.NEOBRUTALISM, isNeo = true,
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
    bg = Color(0xFFFDF6E3),                       // cream lembut
    bgGradient = null,
    bokeh = false,
    cardBg = Color.White, cardBorder = Color(0xFF0A0A0A), cardBorderWidth = 3.dp,
    cornerRadius = 12.dp, hasShadow = true, shadowColor = Color(0xFF0A0A0A), shadowOffset = 5.dp,
    textPrimary = Color(0xFF0A0A0A), textSecondary = Color(0xFF2A2A2A), textMuted = Color(0xFF5A5A5A),
    accent = Color(0xFF00D9D9),                   // cyan utama
    accentOn = Color(0xFF0A0A0A),
    accentSoft = Color(0xFFB3F5F5),
    iconBg = Color(0xFFFFD166),                   // kuning cerah
    iconTint = Color(0xFF0A0A0A),
    divider = Color(0xFF0A0A0A).copy(alpha = 0.15f),
    searchBg = Color.White, searchBorder = Color(0xFF0A0A0A),
    searchText = Color(0xFF0A0A0A), searchPlaceholder = Color(0xFF6A6A6A),
    success = Color(0xFF22C55E), danger = Color(0xFFEF4444),
    neoPalette = listOf(
        Color(0xFF00D9D9),   // cyan
        Color(0xFFFF5252),   // merah cerah
        Color(0xFFFFD166),   // kuning
        Color(0xFF22C55E),   // hijau
        Color(0xFFFF8FB1),   // pink
        Color(0xFF9D7BFF),   // ungu
        Color(0xFFFF9F43),   // orange
        Color(0xFF5B9BFF)    // biru
    ),
    scheme = NeoScheme
)

val NeoTokens = ThemeTokens(
    mode = ThemeMode.NEOBRUTALISM, isNeo = true,
    bg = Color(0xFF1E2230),
    bgGradient = listOf(Color(0xFF232838), Color(0xFF1A1F2E), Color(0xFF141825)),
    bokeh = false,
    cardBg = Color(0xFF2A2F40), cardBorder = Color(0x1AFFFFFF), cardBorderWidth = 1.dp,
    cornerRadius = 18.dp,
    hasShadow = true, shadowColor = Color(0xFF000000), shadowOffset = 6.dp,
    textPrimary = Color(0xFFE8EAF0), textSecondary = Color(0xFFB8BDCC), textMuted = Color(0xFF8A90A0),
    accent = Color(0xFF5B8DEF), accentOn = Color.White, accentSoft = Color(0xFF3A4A6A),
    iconBg = Color(0xFF3A4154), iconTint = Color(0xFF8DB4F0),
    divider = Color(0x14FFFFFF),
    searchBg = Color(0xFF232838), searchBorder = Color(0xFF3A4154),
    searchText = Color(0xFFE8EAF0), searchPlaceholder = Color(0xFF8A90A0),
    success = Color(0xFF5DC787), danger = Color(0xFFE57B7B),
    scheme = SkelScheme
)

val LocalThemeTokens = staticCompositionLocalOf { NeoTokens }

@Composable
fun JavinCodexTheme(
    mode: ThemeMode = ThemeMode.NEOBRUTALISM,
    content: @Composable () -> Unit
) {
    val tokens = when (mode) {
        ThemeMode.NEOBRUTALISM -> NeoTokens
        else -> NeoTokens
    }
    val isDark = mode != ThemeMode.NEOBRUTALISM
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


/**
 * Pilih warna accent dari palet Neo secara deterministik berdasarkan nama.
 * Kalau palet kosong (Glass), balikin fallback.
 */
fun pickAccent(name: String, palette: List<Color>, fallback: Color): Color {
    if (palette.isEmpty()) return fallback
    var hash = 0
    for (c in name) hash = hash * 31 + c.code
    val idx = (hash.absoluteValue) % palette.size
    return palette[idx]
}
