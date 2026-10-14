package app.javin.codex.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import app.javin.codex.MainActivity
import app.javin.codex.R

object NotificationHelper {

    private const val CHANNEL_UPDATE = "javin_update"
    private const val NOTIF_ID_UPDATE = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_UPDATE,
            "Update Aplikasi",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifikasi update Javin Codex"
            enableLights(true)
            lightColor = 0xFF4A9EFF.toInt()
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun showUpdateNotification(context: Context, version: String, notes: String) {
        createChannels(context)

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update Javin Codex Tersedia")
            .setContentText("Versi $version — $notes")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Versi $version\n\n$notes"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setNumber(1)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_UPDATE, notif)
    }

    fun clearUpdateNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_UPDATE)
    }
}
