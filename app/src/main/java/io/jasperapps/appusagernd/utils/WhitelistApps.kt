package io.jasperapps.appusagernd.utils

// 아래 정의된 화이트리스트 앱들의 사용시간 추적
enum class WhitelistApps(val packageName: String) {
    YOUTUBE("com.google.android.youtube"),
    GOOGLE("com.android.chrome"),
    GOOGLE_PHOTOS("com.google.android.apps.photos"),
    GOOGLE_DOCS("com.google.android.apps.docs"),
    YOUTUBE_MUSIC("om.google.android.apps.youtube.music"),
    GOOGLE_PLAY("com.android.vending"),
    GMAIL("com.google.android.gm")
}
