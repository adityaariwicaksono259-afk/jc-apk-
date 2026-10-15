package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiService
import app.javin.codex.data.model.AnimeDetail
import app.javin.codex.data.model.AnimeEpisode
import app.javin.codex.data.model.AnimeItem
import app.javin.codex.data.model.AnimeStreamResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
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
        val animasu = fetchList(epAnimasuOngoing, mapOf("ongoing" to "1", "page" to "1"), "animasu")
        if (animasu.isNotEmpty()) return@withContext Result.success(animasu)

        val otakudesu = fetchList(epOtakudesuOngoing, emptyMap(), "otakudesu")
        Result.success(otakudesu)
    }

    suspend fun listCompleted(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        val animasu = fetchList(epAnimasuCompleted, mapOf("completed" to "1", "page" to "1"), "animasu")
        if (animasu.isNotEmpty()) return@withContext Result.success(animasu)

        val samehadaku = fetchList(epSamehadakuLatest, emptyMap(), "samehadaku")
        Result.success(samehadaku)
    }

    suspend fun listPopular(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        fetchList(epAnimasuPopular, mapOf("popular" to "1", "page" to "1"), "animasu")
            .let { if (it.isNotEmpty()) Result.success(it) else Result.success(emptyList()) }
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
            val r = api.callToolDynamic(epId, mapOf(paramKey to url))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Response kosong"))
            val detail = parseDetail(body, url, source)
            Result.success(detail)
        } catch (e: Exception) {
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
        val data = (root["data"] ?: root) as? Map<*, *> ?: return AnimeDetail(
            title = "Unknown", url = url, source = source
        )

        val title = (data["title"] ?: data["judul"] ?: data["name"]) as? String ?: "Unknown"
        val poster = (data["poster"] ?: data["thumbnail"] ?: data["image"]) as? String
        val synopsis = (data["synopsis"] ?: data["description"] ?: data["desc"]) as? String
        val status = (data["status"] ?: data["state"]) as? String
        val totalEp = (data["totalEpisode"] ?: data["total_episode"] ?: data["episodes_count"]) as? String

        // Genres
        val genres = mutableListOf<String>()
        (data["genres"] ?: data["genre"])?.let { g ->
            when (g) {
                is List<*> -> g.forEach { if (it is String) genres.add(it) }
                is String -> genres.addAll(g.split(",").map { it.trim() })
            }
        }

        // Episodes
        val episodes = mutableListOf<AnimeEpisode>()
        val epsRaw = data["episodes"] ?: data["episode_list"] ?: data["episodeList"]
        if (epsRaw is List<*>) {
            epsRaw.forEachIndexed { idx, e ->
                if (e is Map<*, *>) {
                    val epTitle = (e["title"] ?: e["judul"] ?: e["name"]) as? String ?: "Episode ${idx + 1}"
                    val epUrl = (e["url"] ?: e["link"] ?: e["episodeUrl"]) as? String ?: ""
                    val epNum = (e["number"] ?: e["episode"] ?: e["eps"]) as? String
                    val epDate = (e["date"] ?: e["released"]) as? String
                    if (epUrl.isNotBlank()) {
                        episodes.add(AnimeEpisode(
                            title = epTitle,
                            url = epUrl,
                            number = epNum?.toIntOrNull() ?: (idx + 1),
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
