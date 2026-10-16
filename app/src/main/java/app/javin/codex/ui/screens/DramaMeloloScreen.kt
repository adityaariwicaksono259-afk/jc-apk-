package app.javin.codex.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.AnimeRepository
import app.javin.codex.ui.theme.LocalThemeTokens

/**
 * Drama Melolo-style: scroll vertikal antar episode (kayak TikTok).
 * Setiap episode = 1 halaman penuh dengan player embed.
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val repo = remember { AnimeRepository(ApiClient.get()) }
    var currentIndex by remember { mutableStateOf(0) }
    var localEpisodes by remember { mutableStateOf(episodes) }
    var loadingDetail by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onBack() }

    // Auto-fetch episode kalau kosong + ada detailUrl
    LaunchedEffect(detailUrl, episodes) {
        if (localEpisodes.isEmpty() && !detailUrl.isNullOrBlank()) {
            loadingDetail = true
            try {
                val epId = when (provider) {
                    "reelshort" -> "ep076"
                    else -> "ep064"
                }
                val r = repo.callRawEndpoint(epId, mapOf("url" to detailUrl))
                // Parse episode
                val parsed = parseDramaEpisodesV2(r)
                localEpisodes = parsed
                onEpisodesLoaded(
                    parsed.map { it["url"] ?: "" },
                    parsed.map { it["title"] ?: "" }
                )
            } catch (e: Exception) {
                // ignore
            }
            loadingDetail = false
        } else if (localEpisodes.isNotEmpty()) {
            onEpisodesLoaded(
                localEpisodes.map { it["url"] ?: "" },
                localEpisodes.map { it["title"] ?: "" }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loadingDetail || localEpisodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(localEpisodes, key = { _, ep -> ep["url"] ?: "ep" }) { idx, ep ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()  // full screen per episode
                    ) {
                        MeloloEpisodeView(
                            episodeUrl = ep["url"] ?: "",
                            title = ep["title"] ?: "Episode ${idx + 1}",
                            index = idx,
                            onLongPress = onBack
                        )
                    }
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        if (listState.firstVisibleItemIndex == idx) {
                            currentIndex = idx
                        }
                    }
                }
            }
        }

        // ===== TOP BAR OVERLAY =====
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

        // ===== SWIPE INDICATOR =====
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Swipe",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Satu episode = WebView fullscreen dengan video embed.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MeloloEpisodeView(
    episodeUrl: String,
    title: String,
    index: Int,
    onLongPress: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(episodeUrl) {
        onDispose {
            webView?.let {
                it.stopLoading()
                it.loadUrl("about:blank")
                it.destroy()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            // Hide iklan drama
                            view?.evaluateJavascript("""
                                (function() {
                                    document.body.style.background = 'black';
                                    document.body.style.overflow = 'hidden';
                                    ['header','nav','footer','aside'].forEach(t => {
                                        document.querySelectorAll(t).forEach(e => e.style.display='none');
                                    });
                                    document.querySelectorAll('iframe, video').forEach(v => {
                                        var r = v.getBoundingClientRect();
                                        if (r.width > 200 && r.height > 100) {
                                            v.style.position = 'fixed';
                                            v.style.top = '0';
                                            v.style.left = '0';
                                            v.style.width = '100vw';
                                            v.style.height = '100vh';
                                            v.style.zIndex = '9999';
                                        }
                                    });
                                })();
                            """.trimIndent(), null)
                        }
                    }
                    loadUrl(episodeUrl)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            }
        }

        // Info overlay
        Text(
            text = "Ep ${index + 1}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

/**
 * Parse episode dari response detail drama.
 */
/**
 * Parse response ep064 (Dramabox Video).
 * Format: { data: { episodes: [ { mp4, name, index, unlock } ] } }
 */
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
    } catch (e: Exception) {
        app.javin.codex.JavinDebug.log("parseDramaV2 error: ${e.message}")
    }
    return out
}
