package app.javin.codex.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.javin.codex.data.model.AnimeItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.animeDataStore by preferencesDataStore(name = "javin_anime")

/**
 * Simpan:
 *  - Playlist (bookmark manual user)
 *  - My Anime (history auto-track + progress)
 */
class AnimeHistoryManager private constructor(private val context: Context) {

    companion object {
        private val KEY_PLAYLIST = stringPreferencesKey("playlist")
        private val KEY_HISTORY = stringPreferencesKey("history")

        @Volatile
        private var instance: AnimeHistoryManager? = null

        fun getInstance(context: Context): AnimeHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: AnimeHistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson = Gson()

    // ============================================
    // PLAYLIST (bookmark)
    // ============================================
    val playlistFlow: Flow<List<AnimeItem>> = context.animeDataStore.data.map { prefs ->
        val json = prefs[KEY_PLAYLIST] ?: "[]"
        try {
            val type = object : TypeToken<List<AnimeItem>>() {}.type
            gson.fromJson<List<AnimeItem>>(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getPlaylist(): List<AnimeItem> = playlistFlow.first()

    suspend fun isInPlaylist(url: String): Boolean =
        getPlaylist().any { it.url == url }

    suspend fun addToPlaylist(item: AnimeItem) {
        val current = getPlaylist().toMutableList()
        if (current.none { it.url == item.url }) {
            current.add(0, item)
            savePlaylist(current)
        }
    }

    suspend fun removeFromPlaylist(url: String) {
        val current = getPlaylist().filterNot { it.url == url }
        savePlaylist(current)
    }

    suspend fun togglePlaylist(item: AnimeItem) {
        if (isInPlaylist(item.url)) removeFromPlaylist(item.url)
        else addToPlaylist(item)
    }

    private suspend fun savePlaylist(list: List<AnimeItem>) {
        context.animeDataStore.edit { prefs ->
            prefs[KEY_PLAYLIST] = gson.toJson(list)
        }
    }

    // ============================================
    // MY ANIME (history + progress)
    // ============================================
    val historyFlow: Flow<List<AnimeHistoryEntry>> = context.animeDataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY] ?: "[]"
        try {
            val type = object : TypeToken<List<AnimeHistoryEntry>>() {}.type
            gson.fromJson<List<AnimeHistoryEntry>>(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getHistory(): List<AnimeHistoryEntry> = historyFlow.first()

    /**
     * Update history saat user buka/tonton anime.
     * Progress pakai 0.0 - 1.0 (0 = baru buka, 1 = selesai nonton full).
     */
    suspend fun updateHistory(
        item: AnimeItem,
        episodeTitle: String? = null,
        episodeNumber: Int = 0,
        watchedSeconds: Long = 0,
        totalSeconds: Long = 0
    ) {
        val current = getHistory().toMutableList()
        val existing = current.firstOrNull { it.url == item.url }
        val progress = if (totalSeconds > 0) {
            (watchedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
        } else existing?.progress ?: 0f

        val entry = AnimeHistoryEntry(
            url = item.url,
            title = item.title,
            poster = item.poster,
            source = item.source,
            lastEpisodeTitle = episodeTitle ?: existing?.lastEpisodeTitle,
            lastEpisodeNumber = if (episodeNumber > 0) episodeNumber else existing?.lastEpisodeNumber ?: 0,
            watchedSeconds = watchedSeconds,
            totalSeconds = totalSeconds,
            progress = progress,
            lastWatchedAt = System.currentTimeMillis(),
            totalEpisodesWatched = existing?.totalEpisodesWatched ?: 0
        )

        current.removeAll { it.url == item.url }
        current.add(0, entry)

        // Batasi 100 item
        if (current.size > 100) current.subList(100, current.size).clear()

        saveHistory(current)
    }

    suspend fun markEpisodeWatched(item: AnimeItem, episodeNumber: Int) {
        val current = getHistory().toMutableList()
        val existing = current.firstOrNull { it.url == item.url }
        val count = (existing?.totalEpisodesWatched ?: 0).coerceAtLeast(episodeNumber)
        updateHistory(item, episodeNumber = episodeNumber).let { }
        val updated = getHistory().map {
            if (it.url == item.url) it.copy(totalEpisodesWatched = count) else it
        }
        saveHistory(updated)
    }

    suspend fun removeFromHistory(url: String) {
        val current = getHistory().filterNot { it.url == url }
        saveHistory(current)
    }

    suspend fun clearHistory() {
        saveHistory(emptyList())
    }

    private suspend fun saveHistory(list: List<AnimeHistoryEntry>) {
        context.animeDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = gson.toJson(list)
        }
    }
}

data class AnimeHistoryEntry(
    val url: String,
    val title: String,
    val poster: String? = null,
    val source: String = "animasu",
    val lastEpisodeTitle: String? = null,
    val lastEpisodeNumber: Int = 0,
    val watchedSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val progress: Float = 0f,
    val lastWatchedAt: Long = 0,
    val totalEpisodesWatched: Int = 0
) {
    /** Format "12 menit 34 detik" */
    val watchedFormatted: String
        get() {
            if (watchedSeconds <= 0) return "Belum ditonton"
            val m = watchedSeconds / 60
            val s = watchedSeconds % 60
            return when {
                m >= 60 -> "${m / 60} jam ${m % 60} menit"
                m > 0 -> "$m menit $s detik"
                else -> "$s detik"
            }
        }
}
