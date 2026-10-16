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
 * Liquid pill bottom nav — tab aktif loncat + rounded pill.
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
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(31.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(31.dp))
                .background(t.cardBg.copy(alpha = 0.98f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    LiquidNavItem(
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
private fun androidx.compose.foundation.layout.RowScope.LiquidNavItem(
    tab: NavTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val interaction = remember { MutableInteractionSource() }

    // Animasi: icon naik + background pill
    val offsetY by animateDpAsState(
        targetValue = if (selected) (-20).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )

    val pillColor by animateColorAsState(
        targetValue = if (selected) t.accent else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "pillColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (selected) t.accentOn else t.textMuted,
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )

    val textTint by animateColorAsState(
        targetValue = if (selected) t.accent else t.textMuted,
        animationSpec = tween(durationMillis = 200),
        label = "textTint"
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = offsetY / 2)
        ) {
            Box(
                modifier = Modifier
                    .size(if (selected) 44.dp else 38.dp)
                    .clip(CircleShape)
                    .background(pillColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                    contentDescription = tab.label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (selected) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textTint
                )
            }
        }
    }
}
