package app.javin.codex.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Timelapse
import app.javin.codex.ui.theme.LocalThemeTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class NavTab(
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    CHAT("chat", "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    PLAYLIST("playlist", "Playlist", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    STATUS("status", "Status", Icons.Filled.Timelapse, Icons.Outlined.Timelapse),
    PROFIL("profil", "Profil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@Composable
fun JavinBottomNav(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    val t = LocalThemeTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (t.isNeo) Modifier
                    .background(t.cardBg)
                    .border(width = 2.dp, color = t.cardBorder)
                else Modifier
            )
    ) {
    NavigationBar(
        containerColor = if (t.isNeo) t.cardBg else MaterialTheme.colorScheme.surface,
        tonalElevation = if (t.isNeo) 0.dp else 3.dp
    ) {
        NavTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (t.isNeo) t.accentOn else MaterialTheme.colorScheme.primary,
                    selectedTextColor = if (t.isNeo) t.accentOn else MaterialTheme.colorScheme.primary,
                    indicatorColor = if (t.isNeo) t.accent else MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = if (t.isNeo) t.textMuted else MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = if (t.isNeo) t.textMuted else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
    }
}
