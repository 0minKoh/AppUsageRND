package io.jasperapps.appusagernd.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.jasperapps.appusagernd.R
import io.jasperapps.appusagernd.utils.Constants
import kotlin.random.Random

class NotificationHelper(
    private val context: Context,
) {

    // 알림 ID
    private val notificationId: Int
        get() = Random.nextInt()

    // 알림 생성
    fun createNotification(time: String, packageName: String) {
        val builder = NotificationCompat.Builder(context, Constants.TIMED_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setSmallIcon(R.drawable.ic_baseline_access_alarm_24)
            .setShowWhen(true)
            .setContentIntent(null)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentText(context.getString(R.string.notification_description, packageName, time))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    // 알림 채널 생성
    fun createNotificationChannel(channelId: String, channelName: String) {
        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH,
        )
        chan.enableVibration(true)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        notificationManager.createNotificationChannel(chan)
    }
}
