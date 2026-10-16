package app.javin.codex.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.javin.codex.ui.theme.LocalThemeTokens

/**
 * Liquid nav — pill besar lonjong yang naik waktu dipilih (kayak TikTok).
 */
@Composable
fun LiquidNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    val t = LocalThemeTokens.current
    val tabs = NavTab.entries

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(t.cardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    NavTabItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavTabItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val interaction = remember { MutableInteractionSource() }

    // Animasi: pill naik ke atas kalau selected
    val offsetY by animateDpAsState(
        targetValue = if (selected) (-22).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "offsetY"
    )

    val pillSize by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillSize"
    )

    val pillBg by animateColorAsState(
        targetValue = if (selected) t.accent else Color.Transparent,
        animationSpec = tween(250),
        label = "pillBg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (selected) t.accentOn else t.textMuted,
        animationSpec = tween(200),
        label = "iconTint"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.offset(y = offsetY),
            contentAlignment = Alignment.Center
        ) {
            // Pill besar di belakang icon (muncul kalau selected)
            if (pillSize > 0.dp) {
                Box(
                    modifier = Modifier
                        .size(pillSize)
                        .shadow(
                            elevation = if (selected) 10.dp else 0.dp,
                            shape = CircleShape,
                            ambientColor = t.accent.copy(alpha = 0.5f),
                            spotColor = t.accent.copy(alpha = 0.6f)
                        )
                        .clip(CircleShape)
                        .background(pillBg)
                )
            }
            // Icon
            Icon(
                imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
