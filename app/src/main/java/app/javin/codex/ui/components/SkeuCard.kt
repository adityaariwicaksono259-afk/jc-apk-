package app.javin.codex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.ui.theme.LocalThemeTokens

/**
 * Skeuomorphism card — shadow ganda (highlight atas + shadow bawah).
 */
@Composable
fun SkeuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color(0x80000000),
                spotColor = Color(0x80000000)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        t.cardBg,
                        androidx.compose.ui.graphics.lerp(t.cardBg, Color.Black, 0.15f)
                    )
                )
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * Skeuomorphism button — raised, gradient, shadow.
 */
@Composable
fun SkeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color(0x50566A8E),
                spotColor = Color(0x50566A8E)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (enabled) listOf(
                        Color(0xFF6C8EBF), Color(0xFF4A6FA5)
                    ) else listOf(
                        Color(0xFFB8C9E0), Color(0xFF98AFC9)
                    )
                )
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
