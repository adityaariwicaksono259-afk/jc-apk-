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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.ui.theme.LocalThemeTokens

/**
 * Nav — pill besar lonjong naik (kayak TikTok/liquid navigation).
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
        // Container navbar (rounded pill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(t.cardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
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

    // Pill naik + membesar kalau selected
    val pillHeight by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillHeight"
    )

    val iconOffsetY by animateDpAsState(
        targetValue = if (selected) (-10).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconOffsetY"
    )

    val pillBg by animateColorAsState(
        targetValue = if (selected) t.accent else Color.Transparent,
        animationSpec = tween(280),
        label = "pillBg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (selected) t.accentOn else t.textMuted,
        animationSpec = tween(220),
        label = "iconTint"
    )

    val labelTint by animateColorAsState(
        targetValue = if (selected) t.accent else t.textMuted,
        animationSpec = tween(220),
        label = "labelTint"
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pill lonjong dengan icon
            Box(
                modifier = Modifier
                    .offset(y = iconOffsetY)
                    .height(if (pillHeight > 0.dp) pillHeight else 40.dp)
                    .width(if (selected) 64.dp else 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                    contentDescription = tab.label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Label di bawah icon (muncul kalau selected)
            if (selected) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelTint
                )
            }
        }
    }
}
