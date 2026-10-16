package app.javin.codex.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.themeDataStore by preferencesDataStore(name = "javin_theme")

enum class ThemeMode { NEOBRUTALISM }

class ThemeManager(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    @Volatile
    private var cachedMode: ThemeMode? = null

    suspend fun getTheme(): ThemeMode {
        cachedMode?.let { return it }
        return try {
            val prefs = context.themeDataStore.data.first()
            val raw = prefs[KEY_THEME] ?: "NEOBRUTALISM"
            val mode = try { ThemeMode.valueOf(raw) } catch (e: Exception) { ThemeMode.NEOBRUTALISM }
            cachedMode = mode
            mode
        } catch (e: Exception) { ThemeMode.NEOBRUTALISM }
    }

    fun getThemeSync(): ThemeMode {
        cachedMode?.let { return it }
        return runBlocking { getTheme() }
    }

    suspend fun setTheme(mode: ThemeMode) {
        cachedMode = mode
        context.themeDataStore.edit { prefs -> prefs[KEY_THEME] = mode.name }
    }

    val themeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: "NEOBRUTALISM"
        try { ThemeMode.valueOf(raw) } catch (e: Exception) { ThemeMode.NEOBRUTALISM }
    }
}
