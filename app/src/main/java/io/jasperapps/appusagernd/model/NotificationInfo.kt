package io.jasperapps.appusagernd.model

// 알림 정보 객체
data class NotificationInfo(
    val app_name: String,
    val duration: String,
    val timestamp: Long
)
