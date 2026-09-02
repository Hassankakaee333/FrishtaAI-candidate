package ai.hassan.app.radar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object RadarNotifier {
    private const val CHANNEL_ID = "hassan_radar_daily"
    private const val NOTIFICATION_ID = 4102

    fun notifyDailySummary(context: Context, verified: Int, total: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle("اقتراحات الرادار اليومية")
            .setContentText("اكتُشف $verified مصدرًا موثقًا من $total. افتح Hassan → الرادار للتفاصيل.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "رادار Hassan",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "ملخص يومي لاكتشافات الرادار والمزوّدين المجانية"
        }
        manager.createNotificationChannel(channel)
    }
}
