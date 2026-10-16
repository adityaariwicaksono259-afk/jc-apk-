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
import androidx.compose.material.icons.filled.PlayArrow
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
import app.javin.codex.data.repository.AnimeRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DramaDetailScreen(
    title: String,
    poster: String?,
    url: String,
    provider: String,
    onBack: () -> Unit = {},
    onPlayEpisode: (url: String, title: String, index: Int) -> Unit = { _, _, _ -> },
    onEpisodesLoaded: (List<String>, List<String>) -> Unit = { _, _ -> }
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { AnimeRepository(ApiClient.get()) }

    var episodes by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var synopsis by remember { mutableStateOf<String?>(null) }

    // Load detail drama
    LaunchedEffect(url) {
        loading = true
        errorMsg = null
        try {
            val epId = when (provider) {
                "reelshort" -> "ep073"
                else -> "ep061"
            }
            val r = repo.callRawEndpoint(epId, mapOf("url" to url))
            val parsed = parseDramaDetail(r)
            episodes = parsed.first
            synopsis = parsed.second
            onEpisodesLoaded(
                parsed.first.map { it["url"] ?: "" },
                parsed.first.map { it["title"] ?: "" }
            )
        } catch (e: Exception) {
            errorMsg = e.message ?: "Gagal load"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
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
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Detail Drama",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = t.textPrimary
            )
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
            }
            errorMsg != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Mohon maaf, drama ini belum tersedia\n${errorMsg}",
                    color = t.danger,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // HERO poster
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        if (!poster.isNullOrBlank()) {
                            AsyncImage(
                                model = poster,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            t.bg.copy(alpha = 0.7f),
                                            t.bg
                                        )
                                    )
                                )
                        )
                    }
                }

                // Judul + tombol tonton
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-50).dp)
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = t.textPrimary,
                            lineHeight = 26.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        if (episodes.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(t.accent)
                                    .clickable {
                                        val ep = episodes.first()
                                        onPlayEpisode(
                                            ep["url"] ?: "",
                                            ep["title"] ?: "Episode 1",
                                            0
                                        )
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
                                        text = "Tonton Episode 1",
                                        color = t.accentOn,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Sinopsis
                if (!synopsis.isNullOrBlank()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = "Sinopsis",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = t.textPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = synopsis!!,
                                fontSize = 13.sp,
                                color = t.textSecondary,
                                lineHeight = 19.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // List episode
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Episode (${episodes.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                itemsIndexed(episodes, key = { idx, ep -> ep["url"] ?: "ep-$idx" }) { idx, ep ->
                    DramaEpisodeRow(
                        number = idx + 1,
                        title = ep["title"] ?: "Episode ${idx + 1}",
                        onClick = {
                            onPlayEpisode(
                                ep["url"] ?: "",
                                ep["title"] ?: "Episode ${idx + 1}",
                                idx
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DramaEpisodeRow(
    number: Int,
    title: String,
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(t.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = t.textPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = t.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = t.accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Parse detail drama -> (list episode, sinopsis)
 */
private fun parseDramaDetail(body: String): Pair<List<Map<String, String>>, String?> {
    val out = mutableListOf<Map<String, String>>()
    var synopsis: String? = null
    try {
        val gson = com.google.gson.Gson()
        val root = gson.fromJson(body, Any::class.java)

        fun walk(obj: Any?, depth: Int = 0) {
            if (depth > 7) return
            when (obj) {
                is Map<*, *> -> {
                    // Cek episode
                    val title = (obj["title"] ?: obj["name"] ?: obj["episodeName"]) as? String
                    val url = (obj["url"] ?: obj["link"] ?: obj["episodeUrl"] ?: obj["videoUrl"]) as? String

                    // Cek sinopsis (biasanya di parent)
                    val desc = (obj["synopsis"] ?: obj["description"] ?: obj["desc"] ?: obj["summary"]) as? String
                    if (synopsis == null && desc != null && desc.length > 30) synopsis = desc

                    if (title != null && url != null && url.startsWith("http")) {
                        out.add(mapOf("title" to title, "url" to url))
                    }
                    obj.values.forEach { walk(it, depth + 1) }
                }
                is List<*> -> obj.forEach { walk(it, depth + 1) }
            }
        }
        walk(root)
    } catch (e: Exception) { /* ignore */ }
    return Pair(out.distinctBy { it["url"] }, synopsis)
}
