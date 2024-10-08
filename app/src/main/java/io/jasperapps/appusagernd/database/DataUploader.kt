package io.jasperapps.appusagernd.database

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.jasperapps.appusagernd.model.AppUsage
import io.jasperapps.appusagernd.model.NotificationInfo
import io.jasperapps.appusagernd.model.UserInfo
import io.jasperapps.appusagernd.utils.Constants
import io.jasperapps.appusagernd.utils.DateFormatter
import io.jasperapps.appusagernd.utils.NotificationTimeFormatter
import io.jasperapps.appusagernd.utils.TimeFormatter
import io.jasperapps.appusagernd.utils.WhitelistApps
import java.util.Calendar

class DataUploader(private val context: Context) {
    private val database = Firebase.database // Firebase DB 인스턴스
    private val timeFormatter = TimeFormatter() // 시간 포맷팅
    private val dateFormatter = DateFormatter() // 날짜 포맷팅
    private val notificationFormatter = NotificationTimeFormatter() // 알림 시간? 포맷팅

    // 데이터베이스 업데이트 (특정 유저 정보를 찾아 업데이트)
    fun saveToDatabase(userId: String, userInfo: UserInfo) {
        val reference = database.getReference(userId)
        reference.setValue(userInfo)
    }

    // 발걸음 데이터 업데이트
    fun saveSteps(userId: String, steps: Long) {
        val reference = database.getReference(userId)
        val stepsReference = reference.child(Constants.STEPS_USAGE_KEY)
        val dateKey =
            dateFormatter.getDateFromMilliSeconds(System.currentTimeMillis() - 3600 * 1000)
        stepsReference.child(dateKey).setValue(steps)
    }

    // 심장 박동수 데이터 업데이트
    fun saveHeartRate(userId: String, heartRate: Map<String, Long?>) {
        val reference = database.getReference(userId)
        val heartReference = reference.child(Constants.HEART_RATE_USAGE_KEY)
        val dateKey =
            dateFormatter.getDateFromMilliSeconds(System.currentTimeMillis() - 3600 * 1000)
        heartReference.child(dateKey).child(Constants.MAX_HEART_RATE)
            .setValue(heartRate.getValue(Constants.MAX_HEART_RATE))
        heartReference.child(dateKey).child(Constants.MIN_HEART_RATE)
            .setValue(heartRate.getValue(Constants.MIN_HEART_RATE))
    }

    // 거리 데이터 업데이트
    fun saveDistance(userId: String, distance: Long) {
        val reference = database.getReference(userId)
        val distanceReference = reference.child(Constants.DISTANCE)
        val dateKey =
            dateFormatter.getDateFromMilliSeconds(System.currentTimeMillis() - 3600 * 1000)
        distanceReference.child(dateKey).setValue(distance)
    }

    // 데이터 업로드 성공 여부 반환
    fun upload(userId: String): Boolean {
        val reference = database.getReference(userId)
        val appUsageRef = reference.child(Constants.APP_USAGE_KEY)
        val map = convertData()
        var result = false
        for ((key, value) in map) {
            result = appUsageRef.child(key).setValue(value).isSuccessful
        }
        return result
    }

    // 앱 사용 시간 데이터 GET
    // 핵심 함수
    private fun getAppUsage(): UsageEvents {
        // val start = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        // val end = System.currentTimeMillis()

        // 어제의 0시 (시작 시간)
        val calendarStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)  // 하루 전 날짜로 설정
            set(Calendar.HOUR_OF_DAY, 0)   // 시간을 0시로 설정
            set(Calendar.MINUTE, 0)        // 분을 0분으로 설정
            set(Calendar.SECOND, 0)        // 초를 0초로 설정
            set(Calendar.MILLISECOND, 0)   // 밀리초를 0으로 설정
        }
        val start = calendarStart.timeInMillis

        // 어제의 23시 59분 59초 (종료 시간)
        val calendarEnd = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)  // 하루 전 날짜로 설정
            set(Calendar.HOUR_OF_DAY, 23)  // 시간을 23시로 설정
            set(Calendar.MINUTE, 59)       // 분을 59분으로 설정
            set(Calendar.SECOND, 59)       // 초를 59초로 설정
            set(Calendar.MILLISECOND, 999) // 밀리초를 999로 설정
        }
        val end = calendarEnd.timeInMillis

        val userStatsManager =
            context.getSystemService(AppCompatActivity.USAGE_STATS_SERVICE) as UsageStatsManager
        println("getAppUsage 실행됨 ${userStatsManager.queryEvents(start, end)}")
        return userStatsManager.queryEvents(start, end)
    }

    private fun getListOfUsage(): List<AppUsage> {
        val usageEvents = getAppUsage()
        val events = mutableListOf<UsageEvents.Event>()
        val appUsage = mutableListOf<AppUsage>()

        // 이벤트 필터링 및 수집
        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (!isAppSystem(event.packageName) || event.packageName in getWhitelistApps()) {
                events.add(event)
            }
        }

        // 이벤트를 시간순으로 정렬
        events.sortBy { it.timeStamp }

        val foregroundEvents = mutableMapOf<String, UsageEvents.Event>()

        // 시간 순서대로 이벤트 매칭
        events.forEach { event ->
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // foreground 이벤트는 packageName을 키로 저장
                    foregroundEvents[event.packageName] = event
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    // background 이벤트가 발생하면 해당 패키지의 foreground와 매칭
                    val foreground = foregroundEvents[event.packageName]
                    if (foreground != null) {
                        appUsage.add(
                            AppUsage(
                                event.packageName,
                                timeFormatter.getTimeFromMilliSeconds(foreground.timeStamp),
                                timeFormatter.getTimeFromMilliSeconds(event.timeStamp)
                            )
                        )
                        // 매칭된 foreground 이벤트는 제거
                        foregroundEvents.remove(event.packageName)
                    }
                }
            }
        }

        return appUsage
    }


    // 알림정보(데이터) 업데이트
    fun saveNotification(userId: String, notificationInfo: NotificationInfo) {
        val reference = database.getReference(userId)
        val notificationRef = reference.child(Constants.NOTIFICATION_DB_KEY)
        notificationRef.child(notificationFormatter.getDateFromMilliSeconds(System.currentTimeMillis()))
            .setValue(notificationInfo)
    }

    // 데이터 형식 변경
    private fun convertData(): HashMap<String, List<AppUsage>> {
        val appUsageList = getListOfUsage()
        val filteredList = unionAppUsageRecord(appUsageList)
        val map = HashMap<String, List<AppUsage>>()

        // 어제의 0시 (시작 시간)
        val calendarStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)  // 하루 전 날짜로 설정
            set(Calendar.HOUR_OF_DAY, 0)   // 시간을 0시로 설정
            set(Calendar.MINUTE, 0)        // 분을 0분으로 설정
            set(Calendar.SECOND, 0)        // 초를 0초로 설정
            set(Calendar.MILLISECOND, 0)   // 밀리초를 0으로 설정
        }
        val start = calendarStart.timeInMillis
        val date =
            dateFormatter.getDateFromMilliSeconds(start)
        map[date] = filteredList
        return map
    }

    // WhiteList에 등록된 앱들의 정보 GET
    private fun getWhitelistApps(): List<String> {
        val whitelistApps = arrayListOf<String>()
        enumValues<WhitelistApps>().forEach {
            whitelistApps.add(it.packageName)
        }
        return whitelistApps
    }

    // 사용한 앱 기록 통합
    private fun unionAppUsageRecord(listOfUsage: List<AppUsage>): List<AppUsage> {
        val filteredAppList = mutableListOf<AppUsage>()
        var index = 0
        while (index < listOfUsage.size) {
            val currentApp = listOfUsage[index]
            if (index != listOfUsage.size - 1) {
                val nextApp = listOfUsage[index + 1]
                // 만약 겹치는 이벤트가 있다면
                if (currentApp.end_time == nextApp.start_time && currentApp.app_name == nextApp.app_name) {
                    val unionAppUsage = AppUsage(
                        currentApp.app_name,
                        currentApp.start_time,
                        nextApp.end_time
                    )
                    filteredAppList.add(unionAppUsage)
                    index += 2
                }
                // 총 사용 시간이 0초인 경우
                else if (currentApp.start_time == currentApp.end_time) {
                    index++
                    continue
                }
                // 정상인 경우
                else {
                    filteredAppList.add(currentApp)
                    index++
                }
            } else {
                filteredAppList.add(currentApp)
                index++
            }
        }
        return filteredAppList
    }

    // App System의 정상 여부
    private fun isAppSystem(packageName: String): Boolean {
        val manager: PackageManager = context.packageManager
        return try {
            val info: ApplicationInfo = manager.getApplicationInfo(packageName, 0)
            ((info.flags and ApplicationInfo.FLAG_SYSTEM != 0))
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
