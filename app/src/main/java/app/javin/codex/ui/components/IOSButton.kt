package app.javin.codex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS-style 3D button — gradient + soft shadow + bounce.
 * Cocok untuk CTA utama (Tonton, Lanjut, Jalankan).
 */
@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4A9EFF),
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = color.copy(alpha = 0.4f),
                spotColor = color.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = if (enabled) 1f else 0.5f),
                        color.copy(alpha = if (enabled) 0.75f else 0.3f)
                    )
                )
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * iOS-style icon button bulat kecil (back, bookmark, dll).
 */
@Composable
fun IOSIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    bg: Color = Color.White.copy(alpha = 0.15f)
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(size / 2),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(size / 2))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
