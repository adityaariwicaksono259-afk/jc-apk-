package app.javin.codex

import android.app.Application
import app.javin.codex.data.api.ApiClient

class JavinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Init Retrofit + interceptor (butuh context)
        ApiClient.init(this)
    }
}


/**
 * Global debug logger — panggil dari mana aja, langsung tulis ke file.
 * Contoh: JavinDebug.log("STEP 1: app start")
 */
object JavinDebug {
    private val file = java.io.File("/sdcard/Download/javin_debug.log")
    private val fmt = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)

    @Synchronized
    fun log(msg: String) {
        val line = "[${fmt.format(java.util.Date())}] $msg\n"
        try { file.appendText(line) } catch (_: Throwable) {}
        try { android.util.Log.d("JavinDebug", msg) } catch (_: Throwable) {}
    }

    fun clear() {
        try { file.writeText("") } catch (_: Throwable) {}
    }
}
