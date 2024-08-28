package io.jasperapps.appusagernd.model

// 유저 정보 객체
data class UserInfo(
    val email: String,
    val age: Int,
    val gender: String,
    val app_usage: HashMap<String, List<AppUsage>>
)

// 앱 사용 시간 객체
data class AppUsage(
    val app_name: String,
    val start_time: String,
    val end_time: String
)
