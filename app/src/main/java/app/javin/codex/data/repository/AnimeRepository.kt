package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiService
import app.javin.codex.data.model.AnimeDetail
import app.javin.codex.data.model.AnimeEpisode
import app.javin.codex.data.model.AnimeItem
import app.javin.codex.data.model.AnimeStreamResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

/**
 * AnimeRepository — fetch anime dari 3 source: Animasu, Samehadaku, Otakudesu.
 * Auto-fallback kalau source pertama gagal/kosong.
 */
class AnimeRepository(private val api: ApiService) {

    private val gson = Gson()

    // ============================================
    // ENDPOINT IDs (sesuai endpoints.json)
    // ============================================
    private val epAnimasuSearch = "ep052"
    private val epAnimasuOngoing = "ep049"
    private val epAnimasuCompleted = "ep047"
    private val epAnimasuPopular = "ep050"
    private val epAnimasuDetails = "ep048"
    private val epAnimasuStream = "ep053"

    private val epSamehadakuSearch = "ep136"
    private val epSamehadakuLatest = "ep134"
    private val epSamehadakuDetail = "ep132"
    private val epSamehadakuDownload = "ep133"

    private val epOtakudesuSearch = "ep131"
    private val epOtakudesuOngoing = "ep130"
    private val epOtakudesuDetail = "ep128"
    private val epOtakudesuDownload = "ep129"

    // ============================================
    // SEARCH — multi-source fallback
    // ============================================

    /**
     * Search anime by title, per specific source.
     */
    suspend fun searchByTitle(query: String, source: String): List<AnimeItem> = withContext(Dispatchers.IO) {
        try {
            val (epId, paramKey) = when (source) {
                "samehadaku" -> epSamehadakuSearch to "query"
                "otakudesu" -> epOtakudesuSearch to "s"
                else -> epAnimasuSearch to "search"
            }
            android.util.Log.d("AnimeRepo", "searchByTitle($query, $source) -> $epId")
            val r = api.callToolDynamic(epId, mapOf(paramKey to query))
            if (!r.isSuccessful) {
                android.util.Log.w("AnimeRepo", "searchByTitle HTTP ${r.code()}")
                return@withContext emptyList()
            }
            val body = r.body()?.string() ?: return@withContext emptyList()
            // Skip kalau HTML error (bukan JSON)
            if (body.trimStart().startsWith("<")) {
                android.util.Log.w("AnimeRepo", "searchByTitle HTML error dari upstream")
                return@withContext emptyList()
            }
            parseAnimeList(body, source)
        } catch (e: Exception) {
            android.util.Log.e("AnimeRepo", "searchByTitle error: ${e.message}")
            emptyList()
        }
    }


    // ============================================
    // DOWNLOAD LINKS — Samehadaku/Otakudesu mp4
    // ============================================
    suspend fun getDownloadLinks(episodeUrl: String, source: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val epId = when (source) {
                "samehadaku" -> epSamehadakuDownload
                "otakudesu" -> epOtakudesuDownload
                else -> return@withContext emptyMap()
            }
            val paramKey = "url"
            val r = api.callToolDynamic(epId, mapOf(paramKey to episodeUrl))
            val body = r.body()?.string() ?: return@withContext emptyMap()
            android.util.Log.d("AnimeRepo", "download body=${body.take(500)}")

            // Parse: cari key "360p","480p","720p","1080p" -> URL mp4
            val gson = com.google.gson.Gson()
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val results = mutableMapOf<String, String>()

            fun walk(obj: Any?, depth: Int = 0) {
                if (depth > 5) return
                when (obj) {
                    is Map<*, *> -> {
                        obj.forEach { (k, v) ->
                            val key = k?.toString()?.lowercase() ?: ""
                            if (v is String && v.startsWith("http") &&
                                (key.contains("360") || key.contains("480") || key.contains("720") ||
                                 key.contains("1080") || key.contains("mp4") || key.contains("sd") ||
                                 key.contains("hd"))) {
                                val quality = when {
                                    key.contains("1080") -> "1080p"
                                    key.contains("720") -> "720p"
                                    key.contains("480") -> "480p"
                                    key.contains("360") -> "360p"
                                    else -> k.toString()
                                }
                                results[quality] = v
                            }
                            walk(v, depth + 1)
                        }
                    }
                    is List<*> -> obj.forEach { walk(it, depth + 1) }
                }
            }
            walk(root)
            results
        } catch (e: Exception) {
            android.util.Log.e("AnimeRepo", "getDownloadLinks error: ${e.message}")
            emptyMap()
        }
    }

    suspend fun search(query: String): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())

        // Coba Animasu dulu
        val animasu = searchAnimasu(query)
        if (animasu.isNotEmpty()) return@withContext Result.success(animasu)

        // Fallback Samehadaku
        val samehadaku = searchSamehadaku(query)
        if (samehadaku.isNotEmpty()) return@withContext Result.success(samehadaku)

        // Fallback Otakudesu
        val otakudesu = searchOtakudesu(query)
        if (otakudesu.isNotEmpty()) return@withContext Result.success(otakudesu)

        Result.success(emptyList())
    }

    private suspend fun searchAnimasu(query: String): List<AnimeItem> {
        return try {
            val r = api.callToolDynamic(epAnimasuSearch, mapOf("search" to query))
            val body = r.body()?.string() ?: return emptyList()
            parseAnimeList(body, "animasu")
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun searchSamehadaku(query: String): List<AnimeItem> {
        return try {
            val r = api.callToolDynamic(epSamehadakuSearch, mapOf("query" to query))
            val body = r.body()?.string() ?: return emptyList()
            parseAnimeList(body, "samehadaku")
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun searchOtakudesu(query: String): List<AnimeItem> {
        return try {
            val r = api.callToolDynamic(epOtakudesuSearch, mapOf("s" to query))
            val body = r.body()?.string() ?: return emptyList()
            parseAnimeList(body, "otakudesu")
        } catch (e: Exception) { emptyList() }
    }

    // ============================================
    // LIST — ongoing / completed / popular
    // ============================================
    suspend fun listOngoing(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        // Paralel: coba Animasu + Otakudesu + Samehadaku bareng
        val jobs = listOf(
            async { fetchList(epAnimasuOngoing, mapOf("ongoing" to "1", "page" to "1"), "animasu") },
            async { fetchList(epOtakudesuOngoing, emptyMap(), "otakudesu") },
            async { fetchList(epSamehadakuLatest, emptyMap(), "samehadaku") }
        )
        // Ambil yang pertama selesai + ada isinya
        var result: List<AnimeItem> = emptyList()
        for (job in jobs) {
            try {
                val r = job.await()
                if (r.isNotEmpty()) { result = r; break }
            } catch (e: Exception) {}
        }
        // Cancel sisanya
        jobs.forEach { it.cancel() }
        Result.success(result)
    }

    suspend fun listCompleted(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        val jobs = listOf(
            async { fetchList(epAnimasuCompleted, mapOf("completed" to "1", "page" to "1"), "animasu") },
            async { fetchList(epSamehadakuLatest, emptyMap(), "samehadaku") },
            async { fetchList(epOtakudesuOngoing, emptyMap(), "otakudesu") }
        )
        var result: List<AnimeItem> = emptyList()
        for (job in jobs) {
            try {
                val r = job.await()
                if (r.isNotEmpty()) { result = r; break }
            } catch (e: Exception) {}
        }
        jobs.forEach { it.cancel() }
        Result.success(result)
    }

    suspend fun listPopular(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        val jobs = listOf(
            async { fetchList(epAnimasuPopular, mapOf("popular" to "1", "page" to "1"), "animasu") },
            async { fetchList(epSamehadakuLatest, emptyMap(), "samehadaku") }
        )
        var result: List<AnimeItem> = emptyList()
        for (job in jobs) {
            try {
                val r = job.await()
                if (r.isNotEmpty()) { result = r; break }
            } catch (e: Exception) {}
        }
        jobs.forEach { it.cancel() }
        Result.success(result)
    }

    private suspend fun fetchList(epId: String, params: Map<String, String>, source: String): List<AnimeItem> {
        return try {
            val r = api.callToolDynamic(epId, params)
            val body = r.body()?.string() ?: return emptyList()
            parseAnimeList(body, source)
        } catch (e: Exception) { emptyList() }
    }

    // ============================================
    // DETAIL
    // ============================================
    suspend fun detail(url: String, source: String): Result<AnimeDetail> = withContext(Dispatchers.IO) {
        try {
            val epId = when (source) {
                "samehadaku" -> epSamehadakuDetail
                "otakudesu" -> epOtakudesuDetail
                else -> epAnimasuDetails
            }
            val paramKey = when (source) {
                "samehadaku" -> "link"
                else -> "details"
            }
            dbgLog("detail() START epId=$epId paramKey=$paramKey url=$url")
            val r = api.callToolDynamic(epId, mapOf(paramKey to url))
            dbgLog("detail() HTTP=${r.code()} success=${r.isSuccessful}")
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Response kosong"))
            dbgLog("detail() body[0..500]=${body.take(500)}")
            val detail = parseDetail(body, url, source)
            dbgLog("detail() PARSED title=${detail.title} eps=${detail.episodes.size}")
            Result.success(detail)
        } catch (e: Exception) {
            dbgLog("detail() ERROR: ${e.message}")
            Result.failure(e)
        }
    }

    // ============================================
    // STREAM (khusus Animasu)
    // ============================================
    suspend fun stream(episodeUrl: String): Result<AnimeStreamResult> = withContext(Dispatchers.IO) {
        try {
            val r = api.callToolDynamic(epAnimasuStream, mapOf("stream" to episodeUrl))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Response kosong"))
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val data = root["data"] as? Map<*, *>
            val streamUrl = data?.get("streamUrl") as? String
            val info = data?.get("episodeInfo") as? Map<*, *>
            val title = (info?.get("heading") ?: info?.get("title") ?: "Streaming") as? String ?: "Streaming"

            if (streamUrl.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Stream URL gak ditemukan"))
            }
            Result.success(AnimeStreamResult(streamUrl, title, "animasu"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // PARSER — list anime dari response JSON apapun
    // ============================================
    private fun parseAnimeList(body: String, source: String): List<AnimeItem> {
        return try {
            val root = gson.fromJson(body, Any::class.java)
            val items = mutableListOf<AnimeItem>()
            walkList(root, items, source, 0)
            items.distinctBy { it.url }.take(60)
        } catch (e: Exception) { emptyList() }
    }

    private fun walkList(obj: Any?, out: MutableList<AnimeItem>, source: String, depth: Int) {
        if (depth > 6) return
        when (obj) {
            is Map<*, *> -> {
                // Cek apakah map ini kelihatan seperti anime item
                val title = (obj["title"] ?: obj["judul"] ?: obj["name"]) as? String
                val url = (obj["url"] ?: obj["link"] ?: obj["href"] ?: obj["episodeUrl"]) as? String
                val poster = (obj["poster"] ?: obj["thumbnail"] ?: obj["image"] ?: obj["cover"]) as? String
                val episode = (obj["episode"] ?: obj["eps"] ?: obj["current_episode"]) as? String

                if (title != null && url != null && (url.startsWith("http") || url.startsWith("/"))) {
                    out.add(AnimeItem(
                        title = title,
                        url = url,
                        poster = poster,
                        source = source,
                        episode = episode
                    ))
                }

                // Recurse
                obj.values.forEach { walkList(it, out, source, depth + 1) }
            }
            is List<*> -> obj.forEach { walkList(it, out, source, depth + 1) }
            else -> {}
        }
    }

    // ============================================
    // PARSER — detail (info + list episode)
    // ============================================
    private fun parseDetail(body: String, url: String, source: String): AnimeDetail {
        val root = gson.fromJson(body, Map::class.java) as Map<*, *>

        // Handle nested: root -> data -> data (backend proxy wrap sekali)
        var data = (root["data"] ?: root) as? Map<*, *> ?: return AnimeDetail(
            title = "Unknown", url = url, source = source
        )

        // Kalau data.data masih ada dan lebih dalam, pakai yang lebih dalam
        val inner = data["data"]
        if (inner is Map<*, *> && (inner["title"] != null || inner["episodes"] != null)) {
            data = inner
        }

        val title = (data["title"] ?: data["judul"] ?: data["name"]) as? String ?: "Unknown"
        val poster = (data["image"] ?: data["poster"] ?: data["thumbnail"]) as? String
        val synopsis = (data["sinopsis"] ?: data["synopsis"] ?: data["description"] ?: data["desc"]) as? String
        // status dari info.Status
        val info = data["info"] as? Map<*, *>
        val status = (info?.get("Status") ?: data["status"] ?: data["state"]) as? String
        // totalEpisodes (bukan totalEpisode)
        val totalEpNum = (data["totalEpisodes"] ?: data["totalEpisode"]) as? Number
        val totalEpStr = (data["totalEpisodes"] ?: data["totalEpisode"]) as? String
        val totalEp = totalEpStr ?: totalEpNum?.toString()
        // Rating
        val rating = (data["rating"] as? Number)?.toDouble()

        // Genres
        val genres = mutableListOf<String>()
        (data["genres"] ?: data["genre"])?.let { g ->
            when (g) {
                is List<*> -> g.forEach { if (it is String) genres.add(it) }
                is String -> genres.addAll(g.split(",").map { it.trim() })
                else -> {}
            }
        }

        // Episodes
        val episodes = mutableListOf<AnimeEpisode>()
        val epsRaw = data["episodes"] ?: data["episode_list"] ?: data["episodeList"]
        if (epsRaw is List<*>) {
            epsRaw.forEachIndexed { idx, e ->
                if (e is Map<*, *>) {
                    val epTitle = (e["title"] ?: e["judul"] ?: e["name"]) as? String ?: "Episode ${idx + 1}"
                    val epUrl = (e["episodeUrl"] ?: e["url"] ?: e["link"]) as? String ?: ""
                    val epNum = (e["episode"] ?: e["number"] ?: e["eps"]) as? String
                    val epDate = (e["date"] ?: e["released"]) as? String
                    if (epUrl.isNotBlank()) {
                        // Extract nomor dari title (contoh "Episode 24" -> 24)
                        val numFromTitle = Regex("Episode\\s+(\\d+)").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                        episodes.add(AnimeEpisode(
                            title = epTitle,
                            url = epUrl,
                            number = numFromTitle ?: epNum?.toIntOrNull() ?: (idx + 1),
                            date = epDate
                        ))
                    }
                }
            }
        }

        return AnimeDetail(
            title = title,
            url = url,
            poster = poster,
            synopsis = synopsis,
            genres = genres,
            status = status,
            totalEpisode = totalEp,
            episodes = episodes.reversed(),
            source = source
        )
    }
}

/**
 * Tulis log debug ke /sdcard/Download/anime_debug.log.
 * Baca via file manager atau Termux: cat /sdcard/Download/anime_debug.log
 */
private fun dbgLog(msg: String) {
    android.util.Log.d("AnimeRepo", msg)
    try {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val dir = java.io.File("/sdcard/Download")
        if (!dir.exists()) dir.mkdirs()
        val file = java.io.File(dir, "anime_debug.log")
        file.appendText("[$ts] $msg\n")
    } catch (e: Exception) {
        android.util.Log.e("AnimeRepo", "dbgLog write error: ${e.message}")
    }
}
