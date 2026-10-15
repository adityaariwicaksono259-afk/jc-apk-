package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.AnimeItem
import app.javin.codex.data.repository.AniListRepository
import app.javin.codex.data.repository.AnimeRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

enum class AnimeListTab(val label: String) {
    ONGOING("Ongoing"),
    COMPLETED("Completed"),
    POPULAR("Popular")
}

@Composable
fun AnimeHomeScreen(
    onAnimeClick: (AnimeItem) -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { AnimeRepository(ApiClient.get()) }
    app.javin.codex.JavinDebug.log("AnimeHomeScreen COMPOSE")

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(AnimeListTab.ONGOING) }
    var items by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Load data saat tab ganti
    LaunchedEffect(selectedTab) {
        loading = true
        errorMsg = null
        app.javin.codex.JavinDebug.log("AniList load: tab=$selectedTab")
        val list = when (selectedTab) {
            AnimeListTab.ONGOING -> AniListRepository.get().trending()
            AnimeListTab.COMPLETED -> AniListRepository.get().popular(2)
            AnimeListTab.POPULAR -> AniListRepository.get().popular(1)
        }
        app.javin.codex.JavinDebug.log("AniList loaded: ${list.size} items")
        // Convert AnimeMeta -> AnimeItem
        items = list.map { meta ->
            AnimeItem(
                title = meta.title,
                url = "",  // belum ada slug Animasu
                poster = meta.cover,
                source = "anilist",
                episode = "${meta.episodes ?: "?"} eps · ${meta.status ?: ""}"
            )
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ====== TOP BAR + SEARCH ======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Streaming",
                fontSize = 26.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Cari anime...", fontSize = 14.sp, color = t.searchPlaceholder)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = t.searchPlaceholder)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = t.searchBg,
                    unfocusedContainerColor = t.searchBg,
                    focusedBorderColor = t.searchBorder,
                    unfocusedBorderColor = t.searchBorder,
                    focusedTextColor = t.searchText,
                    unfocusedTextColor = t.searchText,
                    cursorColor = t.accent
                )
            )

            // Trigger search saat submit
            LaunchedEffect(searchQuery) {
                if (searchQuery.length >= 3) {
                    loading = true
                    errorMsg = null
                    val list = AniListRepository.get().search(searchQuery)
                    items = list.map { meta ->
                        AnimeItem(
                            title = meta.title,
                            url = "",
                            poster = meta.cover,
                            source = "anilist",
                            episode = "${meta.episodes ?: "?"} eps · ${meta.status ?: ""}"
                        )
                    }
                    loading = false
                } else if (searchQuery.isBlank()) {
                    selectedTab = selectedTab
                }
            }
        }

        // ====== TAB CHIPS ======
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimeListTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) t.accent else t.cardBg)
                            .then(
                                if (t.isNeo) Modifier
                                    .border(1.5.dp, t.cardBorder, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) t.accentOn else t.textPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ====== GRID ======
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
                }
            }
            errorMsg != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = t.danger, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { selectedTab = selectedTab },
                            colors = ButtonDefaults.buttonColors(containerColor = t.accentSoft)
                        ) { Text("Coba Lagi", color = t.textPrimary) }
                    }
                }
            }
            items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "Belum ada data" else "Gak ketemu: \"$searchQuery\"",
                        color = t.textMuted,
                        fontSize = 13.sp
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.url }) { item ->
                        AnimePosterCard(item = item, onClick = { onAnimeClick(item) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AnimePosterCard(
    item: AnimeItem,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(if (t.isNeo) 12.dp else 14.dp))
                .background(t.iconBg)
                .then(
                    if (t.isNeo) Modifier
                        .border(2.dp, t.cardBorder, RoundedCornerShape(12.dp))
                    else Modifier
                )
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = t.iconTint,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            fontSize = 12.sp,
            fontWeight = if (t.isNeo) FontWeight.Bold else FontWeight.SemiBold,
            color = t.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
        if (!item.episode.isNullOrBlank()) {
            Text(
                text = item.episode,
                fontSize = 10.sp,
                color = t.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
