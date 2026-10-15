package app.javin.codex.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * AniList GraphQL API — metadata anime (search, detail, trending, popular).
 */
class AniListRepository {

    private val ENDPOINT = "https://graphql.anilist.co"

    companion object {
        @Volatile
        private var INSTANCE: AniListRepository? = null
        fun get(): AniListRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AniListRepository().also { INSTANCE = it }
            }
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    data class AnimeMeta(
        val id: Int,
        val title: String,
        val titleEn: String? = null,
        val episodes: Int? = null,
        val status: String? = null,
        val description: String? = null,
        val genres: List<String> = emptyList(),
        val cover: String? = null,
        val score: Int? = null,
        val format: String? = null,
        val year: Int? = null,
        val season: String? = null
    )

    /**
     * Search anime via AniList.
     */
    suspend fun search(query: String, page: Int = 1): List<AnimeMeta> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val gql = """
            query(${'$'}q:String,${'$'}p:Int){
              Page(page:${'$'}p, perPage:30){
                media(search:${'$'}q, type:ANIME, sort:POPULARITY_DESC){
                  id
                  title{romaji english}
                  episodes
                  status
                  description
                  genres
                  coverImage{large}
                  averageScore
                  format
                  seasonYear
                  season
                }
              }
            }
        """.trimIndent()
        val body = gson.toJson(mapOf("query" to gql, "variables" to mapOf("q" to query, "p" to page)))
        call(body)
    }

    /**
     * Trending anime (buat tab Ongoing).
     */
    suspend fun trending(page: Int = 1): List<AnimeMeta> = withContext(Dispatchers.IO) {
        val gql = """
            query(${'$'}p:Int){
              Page(page:${'$'}p, perPage:30){
                media(type:ANIME, sort:TRENDING_DESC){
                  id
                  title{romaji english}
                  episodes
                  status
                  description
                  genres
                  coverImage{large}
                  averageScore
                  format
                  seasonYear
                  season
                }
              }
            }
        """.trimIndent()
        val body = gson.toJson(mapOf("query" to gql, "variables" to mapOf("p" to page)))
        call(body)
    }

    /**
     * Popular anime (buat tab Popular).
     */
    suspend fun popular(page: Int = 1): List<AnimeMeta> = withContext(Dispatchers.IO) {
        val gql = """
            query(${'$'}p:Int){
              Page(page:${'$'}p, perPage:30){
                media(type:ANIME, sort:POPULARITY_DESC){
                  id
                  title{romaji english}
                  episodes
                  status
                  description
                  genres
                  coverImage{large}
                  averageScore
                  format
                  seasonYear
                  season
                }
              }
            }
        """.trimIndent()
        val body = gson.toJson(mapOf("query" to gql, "variables" to mapOf("p" to page)))
        call(body)
    }

    /**
     * Detail anime by ID.
     */
    suspend fun detail(id: Int): AnimeMeta? = withContext(Dispatchers.IO) {
        val gql = """
            query(${'$'}id:Int){
              Media(id:${'$'}id, type:ANIME){
                id
                title{romaji english}
                episodes
                status
                description
                genres
                coverImage{large}
                averageScore
                format
                seasonYear
                season
              }
            }
        """.trimIndent()
        val body = gson.toJson(mapOf("query" to gql, "variables" to mapOf("id" to id)))
        call(body).firstOrNull()
    }

    /**
     * Execute GraphQL request + parse media list.
     */
    private fun call(bodyJson: String): List<AnimeMeta> {
        try {
            val req = Request.Builder()
                .url(ENDPOINT)
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .header("Accept", "application/json")
                .build()

            val resp = client.newCall(req).execute()
            val text = resp.body?.string() ?: return emptyList()
            // Log hanya di debug — pindah ke try biar gak crash
            app.javin.codex.JavinDebug.log("AniList resp len=${text.length}")

            val root = gson.fromJson(text, Map::class.java) as Map<*, *>
            val data = root["data"] as? Map<*, *> ?: return emptyList()
            val page = data["Page"] as? Map<*, *> ?: data
            val media = page["media"] as? List<*> ?: listOf(page["Media"])

            return media.mapNotNull { m ->
                if (m !is Map<*, *>) return@mapNotNull null
                val title = m["title"] as? Map<*, *>
                val cover = m["coverImage"] as? Map<*, *>
                AnimeMeta(
                    id = (m["id"] as? Number)?.toInt() ?: 0,
                    title = (title?.get("romaji") ?: title?.get("english") ?: "Unknown") as String,
                    titleEn = title?.get("english") as? String,
                    episodes = (m["episodes"] as? Number)?.toInt(),
                    status = m["status"] as? String,
                    description = (m["description"] as? String)?.replace(Regex("<[^>]+>"), "")?.trim(),
                    genres = (m["genres"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    cover = cover?.get("large") as? String,
                    score = (m["averageScore"] as? Number)?.toInt(),
                    format = m["format"] as? String,
                    year = (m["seasonYear"] as? Number)?.toInt(),
                    season = m["season"] as? String
                )
            }
        } catch (e: Throwable) {
            app.javin.codex.JavinDebug.log("AniList ERROR: ${e.javaClass.simpleName}: ${e.message}")
            return emptyList()
        }
    }
}
