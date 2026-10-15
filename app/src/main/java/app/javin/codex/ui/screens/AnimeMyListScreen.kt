package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.model.AnimeItem
import app.javin.codex.data.session.AnimeHistoryManager
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun AnimeMyListScreen(
    onAnimeClick: (AnimeItem) -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history = remember { AnimeHistoryManager.getInstance(context) }
    val entries by history.historyFlow.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            Text(
                text = "My Anime",
                fontSize = 26.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
            Text(
                text = "${entries.size} anime pernah ditonton",
                fontSize = 12.sp,
                color = t.textMuted
            )
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = t.textMuted,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Belum ada riwayat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Anime yang kamu tonton bakal muncul di sini",
                        fontSize = 12.sp,
                        color = t.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.url }) { entry ->
                    MyListItem(
                        entry = entry,
                        onClick = {
                            onAnimeClick(
                                AnimeItem(
                                    title = entry.title,
                                    url = entry.url,
                                    poster = entry.poster,
                                    source = entry.source
                                )
                            )
                        },
                        onDelete = {
                            scope.launch { history.removeFromHistory(entry.url) }
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MyListItem(
    entry: app.javin.codex.data.session.AnimeHistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(if (t.isNeo) 12.dp else 14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(t.cardBg)
            .then(
                if (t.isNeo) Modifier.border(2.dp, t.cardBorder, shape)
                else Modifier.border(1.dp, t.cardBorder, shape)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        Box(
            modifier = Modifier
                .size(80.dp, 110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(t.iconBg)
        ) {
            if (!entry.poster.isNullOrBlank()) {
                AsyncImage(
                    model = entry.poster,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 14.sp,
                fontWeight = if (t.isNeo) FontWeight.Bold else FontWeight.SemiBold,
                color = t.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            if (!entry.lastEpisodeTitle.isNullOrBlank()) {
                Text(
                    text = "Eps ${entry.lastEpisodeNumber}: ${entry.lastEpisodeTitle}",
                    fontSize = 11.sp,
                    color = t.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { entry.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = t.accent,
                trackColor = t.divider
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✓ ${entry.totalEpisodesWatched} eps",
                    fontSize = 10.sp,
                    color = t.textMuted
                )
                Text(
                    text = "⏱ ${entry.watchedFormatted}",
                    fontSize = 10.sp,
                    color = t.textMuted
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hapus",
                tint = t.danger,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
