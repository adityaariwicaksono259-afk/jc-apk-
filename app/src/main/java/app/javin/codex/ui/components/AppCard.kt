package app.javin.codex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.javin.codex.ui.theme.LocalThemeTokens

/**
 * Card universal — auto switch Glass <-> NeoBrutalism via LocalThemeTokens.
 * Neo: hard shadow (kotak hitam offset) + border tebal hitam.
 * Glass: translucent + border tipis putih.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(t.cornerRadius)

    if (t.isNeo && t.hasShadow) {
        // Wrapper: shadow di belakang, card di depan
        Box(
            modifier = modifier
                .fillMaxWidth()
                .offset(x = t.shadowOffset, y = t.shadowOffset)
                .clip(shape)
                .background(t.shadowColor)
        ) {}
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(t.cardBg)
                .border(t.cardBorderWidth, t.cardBorder, shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(contentPadding)
        ) { content() }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(t.cardBg)
                .border(t.cardBorderWidth, t.cardBorder, shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(contentPadding)
        ) { content() }
    }
}
