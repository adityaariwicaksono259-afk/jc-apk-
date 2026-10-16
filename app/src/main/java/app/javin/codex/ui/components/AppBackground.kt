package app.javin.codex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.javin.codex.ui.theme.LocalThemeTokens

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val t = LocalThemeTokens.current

    if (t.isNeo) {
        // ===== NEOBRUTALISM: blok warna gerak =====
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(t.bg)
        ) {
            // Blok warna animated (rotasi + translate)
            val transition = rememberInfiniteTransition(label = "neoBg")

            // Blok 1 — cyan kiri atas
            val offset1 by transition.animateFloat(
                initialValue = 0f, targetValue = 40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "off1"
            )
            val rot1 by transition.animateFloat(
                initialValue = -10f, targetValue = 15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "rot1"
            )

            Box(
                modifier = Modifier
                    .offset(x = (-30).dp + offset1.dp, y = 80.dp)
                    .size(180.dp)
                    .rotate(rot1)
                    .background(
                        t.neoPalette.getOrNull(0) ?: t.accent,
                        RoundedCornerShape(20.dp)
                    )
            )

            // Blok 2 — kuning kanan atas
            val offset2 by transition.animateFloat(
                initialValue = 0f, targetValue = -50f,
                animationSpec = infiniteRepeatable(
                    animation = tween(7000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "off2"
            )
            val rot2 by transition.animateFloat(
                initialValue = 20f, targetValue = -15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(9000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "rot2"
            )

            Box(
                modifier = Modifier
                    .offset(x = 240.dp + offset2.dp, y = 120.dp)
                    .size(140.dp)
                    .rotate(rot2)
                    .background(
                        t.neoPalette.getOrNull(2) ?: t.iconBg,
                        RoundedCornerShape(16.dp)
                    )
            )

            // Blok 3 — pink tengah
            val offset3 by transition.animateFloat(
                initialValue = 0f, targetValue = 60f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "off3"
            )

            Box(
                modifier = Modifier
                    .offset(x = 100.dp + offset3.dp, y = 380.dp)
                    .size(120.dp)
                    .rotate(25f)
                    .background(
                        t.neoPalette.getOrNull(4) ?: t.danger,
                        RoundedCornerShape(18.dp)
                    )
            )

            // Blok 4 — hijau kiri bawah
            val offset4 by transition.animateFloat(
                initialValue = 0f, targetValue = -40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "off4"
            )

            Box(
                modifier = Modifier
                    .offset(x = (-20).dp + offset4.dp, y = 620.dp)
                    .size(130.dp)
                    .rotate(-15f)
                    .background(
                        t.neoPalette.getOrNull(3) ?: t.success,
                        RoundedCornerShape(20.dp)
                    )
            )

            content()
        }
    } else {
        // Non-Neo: fallback gradient
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = t.bgGradient ?: listOf(t.bg, t.bg),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            content()
        }
    }
}
