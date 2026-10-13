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
