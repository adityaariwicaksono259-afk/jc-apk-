package app.javin.codex.data.lock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.lockDataStore by preferencesDataStore(name = "javin_lock")

class LockManager(private val context: Context) {

    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_ENABLED = booleanPreferencesKey("lock_enabled")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_TIMEOUT_MS = longPreferencesKey("auto_lock_timeout_ms")
        private val KEY_LAST_ACTIVE = longPreferencesKey("last_active")

        const val TIMEOUT_IMMEDIATE = 0L
        const val TIMEOUT_1MIN = 60_000L
        const val TIMEOUT_5MIN = 5 * 60_000L
        const val TIMEOUT_30MIN = 30 * 60_000L

        @Volatile
        private var instance: LockManager? = null

        fun getInstance(context: Context): LockManager {
            return instance ?: synchronized(this) {
                instance ?: LockManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // ============================================
    // UTILS — Hash & Salt
    // ============================================
    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val hashed = md.digest(pin.toByteArray())
        return hashed.joinToString("") { "%02x".format(it) }
    }

    // ============================================
    // SETUP PIN
    // ============================================
    suspend fun setupPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        context.lockDataStore.edit { prefs ->
            prefs[KEY_PIN_SALT] = salt
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_ENABLED] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return try {
            val prefs = context.lockDataStore.data.first()
            val salt = prefs[KEY_PIN_SALT] ?: return false
            val savedHash = prefs[KEY_PIN_HASH] ?: return false
            val hash = hashPin(pin, salt)
            hash == savedHash
        } catch (e: Exception) {
            false
        }
    }

    // ============================================
    // TOGGLE / CHANGE
    // ============================================
    suspend fun disableLock() {
        context.lockDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_PIN_SALT)
            prefs[KEY_ENABLED] = false
        }
    }

    suspend fun isLockEnabled(): Boolean {
        return try {
            val prefs = context.lockDataStore.data.first()
            (prefs[KEY_ENABLED] ?: false) && prefs[KEY_PIN_HASH] != null
        } catch (e: Exception) { false }
    }

    // ============================================
    // BIOMETRIC
    // ============================================
    suspend fun setBiometric(enabled: Boolean) {
        context.lockDataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC] = enabled
        }
    }

    suspend fun isBiometricEnabled(): Boolean {
        return try {
            val prefs = context.lockDataStore.data.first()
            prefs[KEY_BIOMETRIC] ?: false
        } catch (e: Exception) { false }
    }

    // ============================================
    // TIMEOUT
    // ============================================
    suspend fun setTimeout(ms: Long) {
        context.lockDataStore.edit { prefs ->
            prefs[KEY_TIMEOUT_MS] = ms
        }
    }

    suspend fun getTimeout(): Long {
        return try {
            val prefs = context.lockDataStore.data.first()
            prefs[KEY_TIMEOUT_MS] ?: TIMEOUT_IMMEDIATE
        } catch (e: Exception) { TIMEOUT_IMMEDIATE }
    }

    // ============================================
    // ACTIVITY TRACKING — update kapan terakhir aktif
    // ============================================
    suspend fun updateLastActive() {
        context.lockDataStore.edit { prefs ->
            prefs[KEY_LAST_ACTIVE] = System.currentTimeMillis()
        }
    }

    suspend fun shouldLock(): Boolean {
        if (!isLockEnabled()) return false
        return try {
            val prefs = context.lockDataStore.data.first()
            val timeout = prefs[KEY_TIMEOUT_MS] ?: TIMEOUT_IMMEDIATE
            if (timeout <= 0L) return true  // immediate
            val lastActive = prefs[KEY_LAST_ACTIVE] ?: 0L
            val elapsed = System.currentTimeMillis() - lastActive
            elapsed >= timeout
        } catch (e: Exception) { false }
    }
}
