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
 * Nav — icon bulat warna naik waktu dipilih (kayak javin.pages.dev).
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
            .padding(horizontal = 12.dp)
            .padding(bottom = 30.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(44.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(44.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                tabs.forEachIndexed { idx, tab ->
                    NavTabItem(
                        tab = tab,
                        index = idx,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

// Warna bulat per-tab (kayak javin.pages.dev)
private val NAV_COLORS = listOf(
    Color(0xFFFF6BCB),   // pink — Home
    Color(0xFF9D7BFF),   // ungu — Chat
    Color(0xFFFFB84D),   // kuning — Playlist
    Color(0xFF5BDBFF),   // biru — Status
    Color(0xFFFF9F7F)    // orange — Profil
)

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavTabItem(
    tab: NavTab,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val interaction = remember { MutableInteractionSource() }
    val bgColor = NAV_COLORS.getOrElse(index) { t.accent }

    // Icon naik + membesar kalau selected
    val iconSize by animateDpAsState(
        targetValue = if (selected) 52.dp else 42.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconSize"
    )

    val offsetY by animateDpAsState(
        targetValue = if (selected) (-14).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "offsetY"
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
            modifier = Modifier.offset(y = offsetY)
        ) {
            // Icon bulat warna
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .shadow(
                        elevation = if (selected) 12.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = bgColor.copy(alpha = 0.5f),
                        spotColor = bgColor.copy(alpha = 0.6f)
                    )
                    .clip(CircleShape)
                    .background(if (selected) bgColor else Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                    contentDescription = tab.label,
                    tint = if (selected) Color.White else Color(0xFF6A6A6A),
                    modifier = Modifier.size(if (selected) 26.dp else 22.dp)
                )
            }
            // Label di bawah (cuma kalau selected)
            if (selected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            } else {
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
