package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onPlayEpisode: (url: String, title: String, episodeIndex: Int) -> Unit = { _, _, _ -> },
    onEpisodesLoaded: (List<String>, List<String>) -> Unit = { _, _ -> }
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

    // AniList metadata
    var aniListScore by remember { mutableStateOf<Int?>(null) }
    var aniListGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var aniListSynopsis by remember { mutableStateOf<String?>(null) }

    // Cek playlist
    LaunchedEffect(item.url) {
        inPlaylist = history.isInPlaylist(item.url)
    }

    // Load detail dari Animasu + metadata dari AniList
    LaunchedEffect(item.url, item.title) {
        loading = true
        errorMsg = null
        try {
            if (item.url.isBlank()) {
                // Search dulu di 3 source
                var found = false
                for (source in listOf("animasu", "samehadaku", "otakudesu")) {
                    try {
                        val searchResult = repo.searchByTitle(item.title, source)
                        if (searchResult.isNotEmpty()) {
                            val url = searchResult.first().url
                            val r = repo.detail(url, source)
                            r.onSuccess { d ->
                                detail = d
                                found = true
                                onEpisodesLoaded(d.episodes.map { it.url }, d.episodes.map { it.title })
                            }
                            if (found) break
                        }
                    } catch (e: Exception) {
                        // lanjut
                    }
                }
                if (!found) {
                    errorMsg = "Mohon maaf, server belum ada \"${item.title}\"\nSilakan coba anime lain"
                }
            } else {
                val r = repo.detail(item.url, item.source)
                r.onSuccess { d ->
                    detail = d
                    onEpisodesLoaded(d.episodes.map { it.url }, d.episodes.map { it.title })
                }
                    .onFailure { errorMsg = it.message ?: "Gagal load detail" }
            }

            // Fetch AniList metadata
            try {
                val meta = AniListRepository.get().search(item.title).firstOrNull()
                if (meta != null) {
                    aniListScore = meta.score
                    aniListGenres = meta.genres
                    aniListSynopsis = meta.description
                }
            } catch (e: Exception) { /* ignore */ }

            history.updateHistory(item)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Error"
        }
        loading = false
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
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMsg!!,
                        color = t.danger,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
            detail != null -> {
                val d = detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    // ====== HERO: poster background + overlay ======
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            // Background poster besar
                            val poster = d.poster ?: item.poster
                            if (!poster.isNullOrBlank()) {
                                AsyncImage(
                                    model = poster,
                                    contentDescription = d.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Gradient overlay ke bawah
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                t.bg.copy(alpha = 0.6f),
                                                t.bg
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // ====== JUDUL + INFO (overlap poster) ======
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-60).dp)
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = d.title,
                                fontSize = 22.sp,
                                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                                color = t.textPrimary,
                                lineHeight = 28.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            // Row: skor + tahun + format
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                aniListScore?.let { score ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${score / 10.0}",
                                            fontSize = 12.sp,
                                            color = t.textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (!d.status.isNullOrBlank()) {
                                    Text(
                                        text = d.status,
                                        fontSize = 12.sp,
                                        color = t.textMuted
                                    )
                                }
                                if (d.episodes.isNotEmpty()) {
                                    Text(
                                        text = "${d.episodes.size} eps",
                                        fontSize = 12.sp,
                                        color = t.textMuted
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Tombol Tonton Episode 1
                            if (d.episodes.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(t.accent)
                                        .clickable {
                                            val ep = d.episodes.first()
                                            onPlayEpisode(ep.url, ep.title, 0)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = t.accentOn,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Tonton Episode ${d.episodes.first().number}",
                                            color = t.accentOn,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ====== GENRE CHIPS ======
                    val genres = if (aniListGenres.isNotEmpty()) aniListGenres else d.genres
                    if (genres.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                genres.take(4).forEach { g ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(t.accentSoft)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = g,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = t.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ====== SINOPSIS ======
                    val synopsis = aniListSynopsis ?: d.synopsis
                    if (!synopsis.isNullOrBlank()) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "Sinopsis",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.textPrimary,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = synopsis,
                                fontSize = 13.sp,
                                color = t.textSecondary,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    // ====== LIST EPISODE ======
                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episode",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.textPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "(${d.episodes.size})",
                                fontSize = 13.sp,
                                color = t.textMuted
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    itemsIndexed(d.episodes, key = { _, ep -> ep.url }) { idx, ep ->
                        EpisodeRow(
                            ep = ep,
                            onClick = {
                                onPlayEpisode(ep.url, ep.title, idx)
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
        // Nomor episode
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(t.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${ep.number}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = t.textPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
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
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = t.accent,
            modifier = Modifier.size(20.dp)
        )
    }
}
