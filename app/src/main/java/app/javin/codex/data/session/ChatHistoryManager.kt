package app.javin.codex.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.chatDataStore by preferencesDataStore(name = "javin_chat_history")

data class ChatHistoryItem(
    val text: String,
    val fromUser: Boolean,
    val timestamp: Long
)

class ChatHistoryManager(private val context: Context) {

    companion object {
        private const val MAX_MESSAGES = 100

        @Volatile
        private var instance: ChatHistoryManager? = null

        fun getInstance(context: Context): ChatHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: ChatHistoryManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val gson = Gson()

    private fun keyFor(catalogId: String) =
        stringPreferencesKey("chat_" + catalogId)

    suspend fun load(catalogId: String): List<ChatHistoryItem> {
        if (catalogId.isBlank()) return emptyList()
        return try {
            val prefs = context.chatDataStore.data.first()
            val raw = prefs[keyFor(catalogId)] ?: return emptyList()
            val type = object : TypeToken<List<ChatHistoryItem>>() {}.type
            gson.fromJson<List<ChatHistoryItem>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(catalogId: String, messages: List<ChatHistoryItem>) {
        if (catalogId.isBlank()) return
        try {
            val trimmed = if (messages.size > MAX_MESSAGES) {
                messages.takeLast(MAX_MESSAGES)
            } else messages
            val raw = gson.toJson(trimmed)
            context.chatDataStore.edit { prefs ->
                prefs[keyFor(catalogId)] = raw
            }
        } catch (e: Exception) {}
    }

    suspend fun clear(catalogId: String) {
        if (catalogId.isBlank()) return
        try {
            context.chatDataStore.edit { prefs ->
                prefs.remove(keyFor(catalogId))
            }
        } catch (e: Exception) {}
    }
}
