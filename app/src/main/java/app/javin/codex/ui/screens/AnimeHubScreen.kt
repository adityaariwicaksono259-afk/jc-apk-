package app.javin.codex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.javin.codex.data.model.AnimeItem
import app.javin.codex.ui.theme.LocalThemeTokens

enum class AnimeTab(
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PLAYLIST("Playlist", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    MY_ANIME("My Anime", Icons.Filled.Bookmark, Icons.Outlined.Bookmark)
}

@Composable
fun AnimeHubScreen(
    onBack: () -> Unit = {},
    onAnimeClick: (AnimeItem) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(AnimeTab.HOME) }
    var selectedAnime by remember { mutableStateOf<AnimeItem?>(null) }
    var playerUrl by remember { mutableStateOf<String?>(null) }
    var playerTitle by remember { mutableStateOf("Streaming") }
    val t = LocalThemeTokens.current

    BackHandler(enabled = true) {
        when {
            playerUrl != null -> playerUrl = null
            selectedAnime != null -> selectedAnime = null
            selectedTab != AnimeTab.HOME -> selectedTab = AnimeTab.HOME
            else -> onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = if (t.isNeo) t.cardBg else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (t.isNeo) 0.dp else 3.dp
                ) {
                    AnimeTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.iconFilled else tab.iconOutlined,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(tab.label, style = MaterialTheme.typography.labelSmall)
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
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    AnimeTab.HOME -> AnimeHomeScreen(onAnimeClick = { selectedAnime = it })
                    AnimeTab.PLAYLIST -> AnimePlaylistScreen(onAnimeClick = { selectedAnime = it })
                    AnimeTab.MY_ANIME -> AnimeMyListScreen(onAnimeClick = { selectedAnime = it })
                }
            }
        }

        // ====== OVERLAY: Detail Anime ======
        selectedAnime?.let { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(t.bg)
            ) {
                AnimeDetailScreen(
                    item = item,
                    onBack = { selectedAnime = null },
                    onPlayEpisode = { url, title ->
                        playerUrl = url
                        playerTitle = title
                    }
                )
            }
        }

        // ====== OVERLAY: Player ======
        playerUrl?.let { url ->
            AnimePlayerScreen(
                videoUrl = url,
                title = playerTitle,
                onBack = { playerUrl = null }
            )
        }
    }
}
