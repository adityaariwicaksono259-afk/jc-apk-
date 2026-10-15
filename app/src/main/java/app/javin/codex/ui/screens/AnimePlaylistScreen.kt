package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun AnimePlaylistScreen(
    onAnimeClick: (AnimeItem) -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history = remember { AnimeHistoryManager.getInstance(context) }
    val playlist by history.playlistFlow.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Playlist",
                fontSize = 26.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
            Text(
                text = "${playlist.size} anime disimpan",
                fontSize = 12.sp,
                color = t.textMuted
            )
        }

        if (playlist.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = t.textMuted,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Playlist kosong",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Buka anime, tap tombol bookmark buat simpan",
                        fontSize = 12.sp,
                        color = t.textMuted
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlist, key = { it.url }) { item ->
                    Box {
                        PlaylistPosterCard(item = item, onClick = { onAnimeClick(item) })
                        // Tombol hapus di kanan atas
                        IconButton(
                            onClick = {
                                scope.launch { history.removeFromPlaylist(item.url) }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(t.danger, RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = t.accentOn,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun PlaylistPosterCard(
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
                    if (t.isNeo) Modifier.border(2.dp, t.cardBorder, RoundedCornerShape(12.dp))
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
    }
}
