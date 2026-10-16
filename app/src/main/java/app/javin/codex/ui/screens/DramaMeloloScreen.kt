package app.javin.codex.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.AnimeRepository
import app.javin.codex.ui.theme.LocalThemeTokens

/**
 * Drama Melolo-style: 1 episode per layar, ExoPlayer (native).
 * Swipe naik/turun = ganti episode.
 */
@Composable
fun DramaMeloloScreen(
    title: String,
    episodes: List<Map<String, String>>,
    detailUrl: String? = null,
    provider: String = "dramabox",
    onBack: () -> Unit = {},
    onEpisodesLoaded: (List<String>, List<String>) -> Unit = { _, _ -> }
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { AnimeRepository(ApiClient.get()) }
    var currentIndex by remember { mutableStateOf(0) }
    var localEpisodes by remember { mutableStateOf(episodes) }
    var loadingDetail by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onBack() }

    // Fetch episode dari detailUrl kalau kosong
    LaunchedEffect(detailUrl, episodes) {
        if (localEpisodes.isEmpty() && !detailUrl.isNullOrBlank()) {
            loadingDetail = true
            try {
                val epId = when (provider) {
                    "reelshort" -> "ep076"
                    else -> "ep064"
                }
                val r = repo.callRawEndpoint(epId, mapOf("url" to detailUrl))
                val parsed = parseDramaEpisodesV2(r)
                localEpisodes = parsed
                onEpisodesLoaded(
                    parsed.map { it["url"] ?: "" },
                    parsed.map { it["title"] ?: "" }
                )
            } catch (e: Exception) { /* ignore */ }
            loadingDetail = false
        } else if (localEpisodes.isNotEmpty()) {
            onEpisodesLoaded(
                localEpisodes.map { it["url"] ?: "" },
                localEpisodes.map { it["title"] ?: "" }
            )
        }
    }

    // Swipe vertikal: naik = prev, turun = next
    // VerticalPager buat swipe TikTok-style
    val pagerState = rememberPagerState(
        pageCount = { if (localEpisodes.isEmpty()) 0 else localEpisodes.size }
    )

    // Sync currentIndex ke pagerState
    LaunchedEffect(pagerState.currentPage) {
        currentIndex = pagerState.currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (loadingDetail || localEpisodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val mp4Url = localEpisodes.getOrNull(page)?.get("url")
                if (!mp4Url.isNullOrBlank()) {
                    MeloloExoPlayer(
                        videoUrl = mp4Url,
                        onEnded = {
                            if (page < localEpisodes.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                            }
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Episode ini butuh unlock",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ===== TOP BAR =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp)
                    .padding(top = 32.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "Episode ${currentIndex + 1}/${localEpisodes.size}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // ===== SWIPE HINT =====
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Swipe",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MeloloExoPlayer(videoUrl: String, onEnded: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(videoUrl) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) onEnded()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun parseDramaEpisodesV2(body: String): List<Map<String, String>> {
    val out = mutableListOf<Map<String, String>>()
    try {
        val gson = com.google.gson.Gson()
        val root = gson.fromJson(body, Map::class.java) as Map<*, *>
        val data = root["data"] as? Map<*, *> ?: return emptyList()
        val episodes = data["episodes"] as? List<*> ?: return emptyList()
        episodes.forEach { ep ->
            if (ep is Map<*, *>) {
                val mp4 = ep["mp4"] as? String
                val name = ep["name"] as? String ?: "Episode"
                val idx = (ep["index"] as? Number)?.toInt() ?: 0
                val unlock = ep["unlock"] as? Boolean ?: false
                if (!mp4.isNullOrBlank()) {
                    out.add(mapOf(
                        "title" to name,
                        "url" to mp4,
                        "index" to idx.toString(),
                        "unlock" to unlock.toString()
                    ))
                }
            }
        }
    } catch (e: Exception) { /* ignore */ }
    return out
}
