package app.javin.codex.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.javin.codex.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "javin_music_channel"
        const val NOTIF_ID = 1001
        const val ACTION_PLAY = "javin.music.PLAY"
        const val ACTION_RESUME = "javin.music.RESUME"
        const val ACTION_PAUSE = "javin.music.PAUSE"
        const val ACTION_NEXT = "javin.music.NEXT"
        const val ACTION_PREV = "javin.music.PREV"
        const val ACTION_STOP = "javin.music.STOP"

        var currentTitle: String = ""
        var currentArtist: String = ""
        var currentCoverUrl: String? = null
        @Volatile var isPlaying: Boolean = false

        @Volatile
        var instance: MusicService? = null
    }

    private lateinit var player: ExoPlayer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()

        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        sendBroadcast(Intent("javin.music.TRACK_ENDED"))
                    }
                    updateNotification()
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    setPlayingState(playing)
                    updateNotification()
                }
            })
            // Audio focus + wake lock
            setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true  // handleAudioFocus
            )
            setHandleAudioBecomingNoisy(true)  // pause pas headset dicabut
        }

        // Update notifikasi tiap 1 detik (progress bar)
        positionUpdateJob = scope.launch {
            while (true) {
                if (isPlaying) updateNotification()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra("url")
                val title = intent.getStringExtra("title") ?: ""
                val artist = intent.getStringExtra("artist") ?: ""
                val cover = intent.getStringExtra("cover")
                if (!url.isNullOrBlank()) {
                    // Play track baru
                    currentTitle = title
                    currentArtist = artist
                    currentCoverUrl = cover
                    player.setMediaItem(MediaItem.fromUri(url))
                    player.prepare()
                    player.play()
                } else {
                    // Resume (kalau lagi pause)
                    player.play()
                }
                startForeground(NOTIF_ID, buildNotification())
            }
            ACTION_RESUME -> {
                player.play()
                updateNotification()
            }
            ACTION_PAUSE -> player.pause()
            ACTION_NEXT -> sendBroadcast(Intent("javin.music.NEXT"))
            ACTION_PREV -> sendBroadcast(Intent("javin.music.PREV"))
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        updateNotification()
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback control"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(chan)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PREV),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(if (isPlaying) ACTION_PAUSE else ACTION_RESUME),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Progress bar (kalau sedang play)
        val pos = if (::player.isInitialized) player.currentPosition else 0L
        val dur = if (::player.isInitialized) player.duration.coerceAtLeast(0) else 0L
        val posStr = formatMs(pos)
        val durStr = formatMs(dur)

        val contentText = if (dur > 0) "$currentArtist · $posStr / $durStr" else currentArtist

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle.ifBlank { "Javin Codex Music" })
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
            )

        val url = currentCoverUrl
        if (!url.isNullOrBlank()) {
            scope.launch {
                try {
                    val bmp = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeStream(URL(url).openStream())
                    }
                    if (bmp != null) {
                        builder.setLargeIcon(bmp)
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIF_ID, builder.build())
                    }
                } catch (_: Exception) {}
            }
        }

        return builder.build()
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification())
        } catch (_: Exception) {}
    }

    fun pause() { player.pause() }
    fun resume() { player.play() }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setPlayingState(playing: Boolean) {
        isPlaying = playing
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        positionUpdateJob?.cancel()
        player.release()
        scope.cancel()
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "$m:${sec.toString().padStart(2, '0')}"
}