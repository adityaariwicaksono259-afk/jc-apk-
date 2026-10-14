package app.javin.codex.data.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import app.javin.codex.BuildConfig
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.AppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val version: AppVersion) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object UpdateManager {

    private const val APK_FILENAME = "javin-codex-update.apk"

    suspend fun checkForUpdate(): AppVersion? = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.get().getAppVersion()
            if (resp.isSuccessful && resp.body() != null) {
                val server = resp.body()!!
                if (server.versionCode > BuildConfig.VERSION_CODE) server else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun downloadApk(context: Context, url: String): Long {
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
        if (oldFile.exists()) oldFile.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Update Javin Codex")
            .setDescription("Mengunduh versi terbaru...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    fun installApk(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
            if (!file.exists()) return

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
