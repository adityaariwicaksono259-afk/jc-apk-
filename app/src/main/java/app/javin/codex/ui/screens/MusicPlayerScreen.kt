package app.javin.codex.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Environment
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.javin.codex.data.model.MusicTrack
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MusicPlayerScreen(
    track: MusicTrack,
    playlist: List<MusicTrack> = listOf(track),
    startIndex: Int = 0,
    onBack: () -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentIndex by remember { mutableStateOf(startIndex) }
    var currentTrack by remember { mutableStateOf(track) }
    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var downloading by remember { mutableStateOf(false) }
    var downloadMsg by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) { onBack() }

    // Start background service
    LaunchedEffect(currentTrack) {
        val url = currentTrack.streamUrl
        if (url.isNullOrBlank()) {
            downloadMsg = "Stream URL kosong"
            return@LaunchedEffect
        }
        try {
            val svcIntent = Intent(context, app.javin.codex.service.MusicService::class.java).apply {
                action = app.javin.codex.service.MusicService.ACTION_PLAY
                putExtra("url", url)
                putExtra("title", currentTrack.title)
                putExtra("artist", currentTrack.artist)
                putExtra("cover", currentTrack.artwork)
            }
            ContextCompat.startForegroundService(context, svcIntent)
            isPlaying = true
        } catch (e: Exception) {
            downloadMsg = "Gagal play: ${e.message}"
        }
    }

    // Auto-next listener via BroadcastReceiver
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                when (intent?.action) {
                    "javin.music.TRACK_ENDED" -> {
                        if (currentIndex < playlist.size - 1) {
                            currentIndex++
                            currentTrack = playlist[currentIndex]
                        }
                    }
                    "javin.music.NEXT" -> {
                        if (currentIndex < playlist.size - 1) {
                            currentIndex++
                            currentTrack = playlist[currentIndex]
                        }
                    }
                    "javin.music.PREV" -> {
                        if (currentIndex > 0) {
                            currentIndex--
                            currentTrack = playlist[currentIndex]
                        }
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction("javin.music.TRACK_ENDED")
            addAction("javin.music.NEXT")
            addAction("javin.music.PREV")
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {}

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    // Sync UI state dari service
    LaunchedEffect(Unit) {
        while (true) {
            val svc = app.javin.codex.service.MusicService.instance
            if (svc != null) {
                isPlaying = app.javin.codex.service.MusicService.isPlaying
            }
            kotlinx.coroutines.delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(t.bg)
    ) {
        // ===== TOP BAR =====
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
                    .background(t.cardBg)
                    .border(2.dp, Color(0xFF0A0A0A), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = t.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Now Playing",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = t.textPrimary
            )
            Spacer(Modifier.weight(1f))
            // Download button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (downloading) t.textMuted else Color(0xFFFF3B5C))
                    .clickable(enabled = !downloading) {
                        scope.launch {
                            downloading = true
                            downloadMsg = "Mengunduh..."
                            val result = downloadTrack(context, currentTrack)
                            downloadMsg = result
                            downloading = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (downloading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(0.5f))

        // ===== ALBUM ART =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(t.cardBg)
                .border(3.dp, Color(0xFF0A0A0A), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!currentTrack.artwork.isNullOrBlank()) {
                AsyncImage(
                    model = currentTrack.artwork,
                    contentDescription = currentTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("♪", fontSize = 120.sp, color = t.textMuted)
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== JUDUL + ARTIS =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = currentTrack.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = t.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = currentTrack.artist,
                fontSize = 14.sp,
                color = t.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== SEEK BAR =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Slider(
                value = position.toFloat(),
                onValueChange = { newPos ->
                    // Seek via service (belum diimplement, placeholder)
                    position = newPos.toLong()
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF3B5C),
                    activeTrackColor = Color(0xFFFF3B5C),
                    inactiveTrackColor = t.textMuted.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), fontSize = 11.sp, color = t.textMuted)
                Text(formatTime(duration), fontSize = 11.sp, color = t.textMuted)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== CONTROLS =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prev
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(t.cardBg)
                    .border(2.dp, Color(0xFF0A0A0A), CircleShape)
                    .clickable {
                        if (currentIndex > 0) {
                            currentIndex--
                            currentTrack = playlist[currentIndex]
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SkipPrevious, "Prev", tint = t.textPrimary, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(24.dp))

            // Play/Pause (besar)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B5C))
                    .border(3.dp, Color(0xFF0A0A0A), CircleShape)
                    .clickable {
                        val svcIntent = Intent(context, app.javin.codex.service.MusicService::class.java).apply {
                            action = if (isPlaying) app.javin.codex.service.MusicService.ACTION_PAUSE
                                     else app.javin.codex.service.MusicService.ACTION_PLAY
                        }
                        context.startService(svcIntent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.width(24.dp))

            // Next
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(t.cardBg)
                    .border(2.dp, Color(0xFF0A0A0A), CircleShape)
                    .clickable {
                        if (currentIndex < playlist.size - 1) {
                            currentIndex++
                            currentTrack = playlist[currentIndex]
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SkipNext, "Next", tint = t.textPrimary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Download status
        downloadMsg?.let { msg ->
            Text(
                text = msg,
                fontSize = 12.sp,
                color = t.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // ExoPlayer dipindah ke MusicService (background)
        Spacer(Modifier.height(1.dp))
    }
}

private fun formatTime(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/**
 * Download mp3 ke folder Music.
 */
private suspend fun downloadTrack(context: android.content.Context, track: MusicTrack): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = track.downloadUrl ?: track.streamUrl ?: return@withContext "URL download gak ada"
            val fileName = "${track.title} - ${track.artist}.mp3".replace(Regex("[^a-zA-Z0-9 .-]"), "_")

            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "JavinCodex")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)

            URL(url).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            "Tersimpan: Music/JavinCodex/$fileName"
        } catch (e: Exception) {
            "Gagal download: ${e.message}"
        }
    }
}
