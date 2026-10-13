package app.javin.codex.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// DataStore extension
private val Context.dataStore by preferencesDataStore(name = "javin_session")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("session_token")
        private val KEY_USER_CODE = stringPreferencesKey("user_code")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
        private val KEY_SAVED_AT = longPreferencesKey("saved_at")

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // In-memory cache buat interceptor (biar cepet, nggak baca disk)
    @Volatile
    private var cachedToken: String? = null

    // ============================================
    // SAVE SESSION (setelah verify OTP)
    // ============================================
    suspend fun saveSession(
        token: String,
        userCode: String,
        userName: String?,
        userEmail: String?,
        userAvatar: String?
    ) {
        cachedToken = token
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_CODE] = userCode
            prefs[KEY_USER_NAME] = userName ?: ""
            prefs[KEY_USER_EMAIL] = userEmail ?: ""
            prefs[KEY_USER_AVATAR] = userAvatar ?: ""
            prefs[KEY_SAVED_AT] = System.currentTimeMillis()
        }
    }

    // ============================================
    // GET TOKEN (sync, dari cache)
    // ============================================
    fun getTokenSync(): String? {
        if (cachedToken != null) return cachedToken
        // Cache kosong → baca dari DataStore (blocking, cuma sekali)
        return runBlocking {
            try {
                val prefs = context.dataStore.data.first()
                val token = prefs[KEY_TOKEN]
                cachedToken = token
                token
            } catch (e: Exception) {
                null
            }
        }
    }

    // ============================================
    // GET TOKEN (suspend)
    // ============================================
    suspend fun getToken(): String? {
        cachedToken?.let { return it }
        return try {
            val prefs = context.dataStore.data.first()
            val token = prefs[KEY_TOKEN]
            cachedToken = token
            token
        } catch (e: Exception) {
            null
        }
    }

    // ============================================
    // GET USER CODE
    // ============================================
    suspend fun getUserCode(): String? {
        return try {
            val prefs = context.dataStore.data.first()
            prefs[KEY_USER_CODE]
        } catch (e: Exception) {
            null
        }
    }

    // ============================================
    // FLOW — reactive observation
    // ============================================
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userCodeFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_CODE] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }

    // ============================================
    // IS LOGGED IN
    // ============================================
    suspend fun isLoggedIn(): Boolean = getToken() != null

    // ============================================
    // CLEAR SESSION (logout)
    // ============================================
    suspend fun clearSession() {
        cachedToken = null
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
