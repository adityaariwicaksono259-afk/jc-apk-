package app.javin.codex.data.api

import android.content.Context
import app.javin.codex.data.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://jvin.pages.dev/"

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun init(context: Context) {
        if (retrofit != null) return
        val session = SessionManager.getInstance(context)

        // ==== Logging (debug) ====
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // ==== OkHttp with Bearer interceptor ====
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "JavinCodex/1.0.0 (Android Native)")
                    .header("Origin", "https://jvin.pages.dev")
                    .header("Referer", "https://jvin.pages.dev/")
                    .header("Sec-Fetch-Site", "same-site")

                // Inject Bearer token kalau ada
                val token = session.getTokenSync()
                if (!token.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $token")
                }

                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit!!.create(ApiService::class.java)
    }

    fun get(): ApiService {
        return apiService ?: throw IllegalStateException("ApiClient belum di-init. Panggil ApiClient.init(context) di Application/MainActivity.")
    }
}
