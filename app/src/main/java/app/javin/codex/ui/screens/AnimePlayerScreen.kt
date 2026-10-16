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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Auto-detect: kalau URL video langsung (.mp4/.m3u8) -> ExoPlayer.
 * Kalau Blogger/iframe/embed -> WebView dengan iklan di-hide.
 */
private fun isDirectVideo(url: String): Boolean {
    val lower = url.lowercase()
    return lower.endsWith(".mp4") ||
           lower.endsWith(".m3u8") ||
           lower.endsWith(".webm") ||
           lower.contains(".mp4?") ||
           lower.contains(".m3u8?")
}

@Composable
fun AnimePlayerScreen(
    videoUrl: String,
    title: String = "Streaming",
    onBack: () -> Unit = {},
    onNextEpisode: (() -> Unit)? = null
) {
    val directVideo = remember(videoUrl) { isDirectVideo(videoUrl) }
    var showTopBar by remember { mutableStateOf(true) }

    BackHandler(enabled = true) { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (directVideo) {
            ExoPlayerView(videoUrl, onNextEpisode)
        } else {
            WebPlayerView(videoUrl)
        }

        // ====== TOP BAR CUSTOM ======
        if (showTopBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(top = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExoPlayerView(videoUrl: String, onNextEpisode: (() -> Unit)?) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    onNextEpisode?.invoke()
                }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPlayerView(url: String) {
    var loading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    // JS buat sembunyiin elemen iklan umum
    val hideAdsJs = """
        (function() {
            var selectors = [
                'iframe[src*="ads"]', 'iframe[src*="doubleclick"]',
                'div[class*="ad-"]', 'div[id*="ad-"]',
                'div[class*="banner"]', 'div[id*="banner"]',
                '.adsbygoogle', '[class*="popup"]', '[class*="overlay"]',
                'div[class*="iklan"]', 'a[href*="slot"]', 'a[href*="judi"]'
            ];
            selectors.forEach(function(sel) {
                try { document.querySelectorAll(sel).forEach(function(el) { el.style.display='none'; }); } catch(e) {}
            });
            // Auto-hide sticky footer ads
            try {
                var all = document.querySelectorAll('div, iframe, ins');
                all.forEach(function(el) {
                    var r = el.getBoundingClientRect();
                    if (r.height > 50 && r.height < 200 && r.width > 300 && el.tagName !== 'VIDEO') {
                        var txt = (el.innerText || '').toLowerCase();
                        if (txt.includes('iklan') || txt.includes('close') || txt.includes('tutup')) {
                            el.style.display = 'none';
                        }
                    }
                });
            } catch(e) {}
        })();
    """.trimIndent()

    DisposableEffect(Unit) {
        onDispose {
            webView?.let {
                it.stopLoading()
                it.loadUrl("about:blank")
                it.destroy()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            super.onPageStarted(view, url, favicon)
                            loading = true
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            loading = false
                            // Inject JS buat hide iklan
                            view?.evaluateJavascript(hideAdsJs, null)
                        }
                    }
                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            }
        }
    }
}
