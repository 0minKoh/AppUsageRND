package io.jasperapps.appusagernd.utils

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord

// 상수들을 정의
object Constants {
    const val APP_USAGE_KEY = "app_usage"
    const val NOTIFICATIONS_ENABLED_LEY = "NotificationsEnabled"
    const val TIME_NOTIFICATION_KEY = "TimeNotification"
    const val NOTIFICATION_DB_KEY = "notifications"
    const val MINIMUM_FETCH_INTERVAL = 3600L
    const val STEPS_USAGE_KEY = "steps"
    const val HEART_RATE_USAGE_KEY = "heart rate"
    const val DISTANCE = "distance"
    // const val SELF_REMINDER_HOUR = 0
    const val SELF_REMINDER_HOUR = 24
    const val SEND_REMIND_TAG = "send_reminder_periodic"
    const val MAX_HEART_RATE = "max_heart_rate"
    const val MIN_HEART_RATE = "min_heart_rate"
    const val IS_USER_FIRST_TIME_IN_APP = "is_user_first_time_in_app"
    const val APP_LANGUAGE = "application_language"

    const val FOREGROUND_SERVICE_CHANNEL_ID = "ForegroundServiceChannelId"
    const val TIMED_NOTIFICATION_CHANNEL_ID = "TimedNotificationsChannelId"
    const val HEALTH_APP_PACKAGE_NAME = "com.google.android.apps.healthdata"

    val HEALTH_PERMISSION =
        setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class)
        )

    val NOTIFICATION_ENABLED_EMAILS_LIST = listOf(
        "usertestnotificationtrue1@gmail.com",
    )
}
