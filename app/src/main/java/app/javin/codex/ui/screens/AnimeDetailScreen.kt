package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.AnimeDetail
import app.javin.codex.data.model.AnimeEpisode
import app.javin.codex.data.model.AnimeItem
import app.javin.codex.data.repository.AniListRepository
import app.javin.codex.data.repository.AnimeRepository
import app.javin.codex.data.session.AnimeHistoryManager
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun AnimeDetailScreen(
    item: AnimeItem,
    onBack: () -> Unit = {},
    onPlayEpisode: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val t = LocalThemeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AnimeRepository(ApiClient.get()) }
    val history = remember { AnimeHistoryManager.getInstance(context) }

    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var inPlaylist by remember { mutableStateOf(false) }

    // Cek playlist
    LaunchedEffect(item.url) {
        inPlaylist = history.isInPlaylist(item.url)
    }

    // Load detail
    LaunchedEffect(item.url, item.title) {
        loading = true
        errorMsg = null

        try {
            if (item.source == "anilist" || item.url.isBlank()) {
                // Source AniList: cari di Animasu, terus Samehadaku, terus Otakudesu
                var found = false
                for (source in listOf("animasu", "samehadaku", "otakudesu")) {
                    val searchResult = repo.searchByTitle(item.title, source)
                    if (searchResult.isNotEmpty()) {
                        val url = searchResult.first().url
                        val r = repo.detail(url, source)
                        r.onSuccess {
                            detail = it
                            found = true
                        }
                        if (found) break
                    }
                }
                if (!found) {
                    errorMsg = "Hasil \"${item.title}\" tidak ditemukan, tunggu next update"
                }
            } else {
                // Source langsung (animasu/samehadaku/otakudesu)
                val r = repo.detail(item.url, item.source)
                r.onSuccess { detail = it }
                    .onFailure { errorMsg = it.message ?: "Gagal load detail" }
            }
        } catch (e: Exception) {
            errorMsg = e.message ?: "Error"
        }
        loading = false
        history.updateHistory(item)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ====== TOP BAR ======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.iconBg)
                    .then(
                        if (t.isNeo) Modifier.border(2.dp, t.cardBorder, CircleShape)
                        else Modifier
                    )
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Detail",
                fontSize = 18.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
            Spacer(Modifier.weight(1f))
            // Tombol Bookmark
            Icon(
                imageVector = if (inPlaylist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (inPlaylist) t.accent else t.textMuted,
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        scope.launch {
                            history.togglePlaylist(item)
                            inPlaylist = history.isInPlaylist(item.url)
                        }
                    }
            )
        }

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
                }
            }
            errorMsg != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = t.danger, fontSize = 13.sp)
                }
            }
            detail != null -> {
                val d = detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    // ====== POSTER + INFO ======
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Poster
                            Box(
                                modifier = Modifier
                                    .size(120.dp, 170.dp)
                                    .clip(RoundedCornerShape(if (t.isNeo) 12.dp else 14.dp))
                                    .background(t.iconBg)
                                    .then(
                                        if (t.isNeo) Modifier.border(2.dp, t.cardBorder, RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                            ) {
                                val poster = d.poster ?: item.poster
                                if (!poster.isNullOrBlank()) {
                                    AsyncImage(
                                        model = poster,
                                        contentDescription = d.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = d.title,
                                    fontSize = 16.sp,
                                    fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                                    color = t.textPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                if (!d.status.isNullOrBlank()) {
                                    Text(
                                        text = "Status: ${d.status}",
                                        fontSize = 11.sp,
                                        color = t.textMuted
                                    )
                                }
                                if (!d.totalEpisode.isNullOrBlank()) {
                                    Text(
                                        text = "Total: ${d.totalEpisode} eps",
                                        fontSize = 11.sp,
                                        color = t.textMuted
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${d.episodes.size} episode tersedia",
                                    fontSize = 11.sp,
                                    color = t.accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ====== GENRES ======
                    if (d.genres.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                d.genres.take(4).forEach { g ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(t.accentSoft)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = g,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = t.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ====== SINOPSIS ======
                    if (!d.synopsis.isNullOrBlank()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Sinopsis",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.textPrimary,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = d.synopsis,
                                fontSize = 12.sp,
                                color = t.textSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 20.dp),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // ====== LIST EPISODE ======
                    item {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Episode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = t.textPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(d.episodes, key = { it.url }) { ep ->
                        EpisodeRow(
                            ep = ep,
                            onClick = {
                                onPlayEpisode(ep.url, ep.title)
                                scope.launch {
                                    history.updateHistory(
                                        item = item,
                                        episodeTitle = ep.title,
                                        episodeNumber = ep.number
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    ep: AnimeEpisode,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(shape)
            .background(t.cardBg)
            .then(
                if (t.isNeo) Modifier.border(1.5.dp, t.cardBorder, shape)
                else Modifier.border(1.dp, t.cardBorder, shape)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = t.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = ep.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = t.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!ep.date.isNullOrBlank()) {
            Text(
                text = ep.date,
                fontSize = 10.sp,
                color = t.textMuted
            )
        }
    }
}
