package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.MusicTrack
import app.javin.codex.data.repository.MusicRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage

enum class MusicTab(val label: String) {
    TRENDING("Trending"),
    POP_INDO("Pop Indo"),
    LOFI("Lofi")
}

@Composable
fun MusicHubScreen(
    onTrackClick: (MusicTrack, List<MusicTrack>, Int) -> Unit = { _, _, _ -> }
) {
    val t = LocalThemeTokens.current
    val repo = remember { MusicRepository(ApiClient.get()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(MusicTab.TRENDING) }
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        loading = true
        tracks = when (selectedTab) {
            MusicTab.TRENDING -> repo.trendingAudius()
            MusicTab.POP_INDO -> repo.search("lagu indonesia viral")
            MusicTab.LOFI -> repo.search("lofi chill")
        }
        loading = false
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            loading = true
            tracks = repo.search(searchQuery)
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Music",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = t.textPrimary
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari lagu, artis...", fontSize = 14.sp, color = t.textMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = t.textPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0A0A0A),
                    unfocusedBorderColor = Color(0xFF0A0A0A),
                    focusedTextColor = t.textPrimary,
                    unfocusedTextColor = t.textPrimary,
                    cursorColor = t.accent
                )
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MusicTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (selected) t.accent else Color.White)
                            .border(2.dp, Color(0xFF0A0A0A), RoundedCornerShape(24.dp))
                            .clickable { selectedTab = tab; searchQuery = "" }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) t.accentOn else Color(0xFF0A0A0A)
                        )
                    }
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
            }
            tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada hasil", color = t.textMuted, fontSize = 14.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tracks.size) { idx ->
                    MusicTrackCard(
                        track = tracks[idx],
                        onClick = { onTrackClick(tracks[idx], tracks, idx) }
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun MusicTrackCard(
    track: MusicTrack,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(2.dp, Color(0xFF0A0A0A), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(t.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            if (!track.artwork.isNullOrBlank()) {
                AsyncImage(
                    model = track.artwork,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("♪", fontSize = 24.sp, color = t.textPrimary)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0A0A0A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${track.artist} · ${formatDuration(track.duration)}",
                fontSize = 11.sp,
                color = t.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF3B5C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
