package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiService
import app.javin.codex.data.model.MusicTrack
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MusicRepository(private val api: ApiService) {

    companion object {
        private const val AUDIUS_BASE = "https://discoveryprovider.audius.co"
        private const val SPOTIFY_SEARCH = "ep041"
        private const val SPOTIFY_DOWNLOAD = "ep040"
        private const val SPOTIFY_META = "ep039"
    }

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // ============================================
    // AUDIUS — streaming langsung
    // ============================================
    suspend fun searchAudius(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val url = "$AUDIUS_BASE/v1/tracks/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val req = Request.Builder().url(url).build()
            val res = httpClient.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()
            parseAudiusTracks(body)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun trendingAudius(): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$AUDIUS_BASE/v1/tracks/trending").build()
            val res = httpClient.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()
            parseAudiusTracks(body)
        } catch (e: Exception) { emptyList() }
    }

    private fun parseAudiusTracks(body: String): List<MusicTrack> {
        val out = mutableListOf<MusicTrack>()
        try {
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val data = root["data"] as? List<*> ?: return emptyList()
            data.forEach { item ->
                if (item is Map<*, *>) {
                    val id = item["id"] as? String ?: return@forEach
                    val title = item["title"] as? String ?: "Unknown"
                    val user = item["user"] as? Map<*, *>
                    val artist = (user?.get("name") ?: user?.get("handle") ?: "Unknown") as String
                    val artwork = (user?.get("profile_picture") as? Map<*, *>)?.let { pic ->
                        (pic["480x480"] ?: pic["150x150"] ?: pic["1000x1000"]) as? String
                    }
                    val duration = (item["duration"] as? Number)?.toInt() ?: 0
                    val playCount = (item["play_count"] as? Number)?.toLong() ?: 0
                    val downloadable = item["is_downloadable"] as? Boolean ?: false

                    out.add(MusicTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        artwork = artwork,
                        streamUrl = "$AUDIUS_BASE/v1/tracks/$id/stream",
                        downloadUrl = if (downloadable) "$AUDIUS_BASE/v1/tracks/$id/download" else null,
                        duration = duration,
                        source = "audius",
                        playCount = playCount,
                        isDownloadable = downloadable
                    ))
                }
            }
        } catch (e: Exception) { /* ignore */ }
        return out
    }

    // ============================================
    // SPOTIFY — via backend
    // ============================================
    suspend fun searchSpotify(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val r = api.callToolDynamic(SPOTIFY_SEARCH, mapOf("q" to query))
            val body = r.body()?.string() ?: return@withContext emptyList()
            parseSpotifyResult(body)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun spotifyMeta(spotifyUrl: String): MusicTrack? = withContext(Dispatchers.IO) {
        try {
            val r = api.callToolDynamic(SPOTIFY_META, mapOf("url" to spotifyUrl))
            val body = r.body()?.string() ?: return@withContext null
            parseSpotifyResult(body).firstOrNull()
        } catch (e: Exception) { null }
    }

    suspend fun spotifyDownload(spotifyUrl: String): MusicTrack? = withContext(Dispatchers.IO) {
        try {
            val r = api.callToolDynamic(SPOTIFY_DOWNLOAD, mapOf("url" to spotifyUrl))
            val body = r.body()?.string() ?: return@withContext null
            parseSpotifyResult(body).firstOrNull()
        } catch (e: Exception) { null }
    }

    private fun parseSpotifyResult(body: String): List<MusicTrack> {
        val out = mutableListOf<MusicTrack>()
        try {
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val data = root["data"] as? Map<*, *> ?: return emptyList()

            // Format ep041 (spotify play): data.data = { title, artist, cover, mp3_url }
            val innerData = data["data"] as? Map<*, *>
            if (innerData != null && (innerData.containsKey("mp3_url") || innerData.containsKey("title"))) {
                val track = parseSingleSpotifyTrack(innerData)
                if (track != null) return listOf(track)
            }

            // Format ep040 (spotify download): data.tracks = [...]
            val tracks = data["tracks"] as? List<*>
            if (tracks != null) {
                tracks.forEach { tt ->
                    if (tt is Map<*, *>) {
                        val track = parseSingleSpotifyTrack(tt)
                        if (track != null) out.add(track)
                    }
                }
            }

            // Fallback: coba langsung dari data
            if (out.isEmpty() && (data.containsKey("mp3_url") || data.containsKey("title"))) {
                val track = parseSingleSpotifyTrack(data)
                if (track != null) return listOf(track)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepo", "parseSpotify: ${e.message}")
        }
        return out
    }

    private fun parseSingleSpotifyTrack(obj: Map<*, *>): MusicTrack? {
        val title = (obj["title"] ?: obj["name"]) as? String ?: return null
        val artist = obj["artist"] as? String ?: "Unknown"
        val album = obj["album"] as? String
        val cover = (obj["cover"] ?: obj["thumbnail"] ?: obj["image"] ?: obj["artwork"]) as? String
        val mp3 = obj["mp3_url"] as? String
        val spUrl = obj["spotify_url"] as? String
        val durationStr = obj["duration"] as? String

        val duration = durationStr?.let {
            val p = it.split(":")
            if (p.size == 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
        } ?: 0

        return MusicTrack(
            id = spUrl ?: mp3?.takeLast(20) ?: title.hashCode().toString(),
            title = title,
            artist = artist,
            artwork = cover,
            streamUrl = mp3,
            downloadUrl = mp3,
            duration = duration,
            source = "spotify",
            album = album,
            isDownloadable = !mp3.isNullOrBlank()
        )
    }

    // ============================================
    // COMBINED
    // ============================================
    suspend fun search(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        // Coba Spotify dulu (katalog Indo lengkap)
        val spotify = searchSpotify(query)
        if (spotify.isNotEmpty()) return@withContext spotify
        // Fallback Audius
        searchAudius(query)
    }
}
