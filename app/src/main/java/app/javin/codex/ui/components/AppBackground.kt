package app.javin.codex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.javin.codex.ui.theme.LocalThemeTokens

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalThemeTokens.current

    if (tokens.isNeo) {
        // NeoBrutalism: flat cream + subtle dot pattern
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(tokens.bg)
        ) {
            content()
        }
    } else {
        // Glass: gradient + bokeh
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = tokens.bgGradient ?: listOf(tokens.bg, tokens.bg),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x664A9EFF), Color.Transparent),
                            center = Offset(100f, 200f),
                            radius = 600f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x447F5AF0), Color.Transparent),
                            center = Offset(800f, 1400f),
                            radius = 700f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x33FF7FE5), Color.Transparent),
                            center = Offset(500f, 800f),
                            radius = 500f
                        )
                    )
            )
            content()
        }
    }
}
