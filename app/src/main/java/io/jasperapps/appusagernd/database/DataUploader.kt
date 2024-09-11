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
        println("userId: $userId")
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
        val start = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val end = System.currentTimeMillis()
        val userStatsManager =
            context.getSystemService(AppCompatActivity.USAGE_STATS_SERVICE) as UsageStatsManager
        println("getAppUsage 실행됨 ${userStatsManager.queryEvents(start, end)}")
        return userStatsManager.queryEvents(start, end)
    }

    // 사용한 앱 목록 리스트 GET
    private fun getListOfUsage(): List<AppUsage> {
        val usageEvents = getAppUsage()
        val events = mutableListOf<UsageEvents.Event>()
        val foregroundEvents = mutableListOf<UsageEvents.Event>()
        val backgroundEvents = mutableListOf<UsageEvents.Event>()
        val appUsage = mutableListOf<AppUsage>()

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (!isAppSystem(event.packageName) || event.packageName in getWhitelistApps()) {
                events.add(event)
            }
        }

        // events.forEach {
        //     if (it.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
        //         foregroundEvents.add(it)
        //     }
        //     if (it.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
        //         backgroundEvents.add(it)
        //     }
        // }
        events.forEach {
            if (it.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundEvents.add(it)
            }
            if (it.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                backgroundEvents.add(it)
            }
        }
        val foregroundIterator = foregroundEvents.iterator()
        while (foregroundIterator.hasNext()) {
            val backgroundIterator = backgroundEvents.iterator()
            val foreground = foregroundIterator.next()
            while (backgroundIterator.hasNext()) {
                val background = backgroundIterator.next()
                if (foreground.packageName == background.packageName) {
                    appUsage.add(
                        AppUsage(
                            foreground.packageName,
                            timeFormatter.getTimeFromMilliSeconds(foreground.timeStamp),
                            timeFormatter.getTimeFromMilliSeconds(background.timeStamp)
                        )
                    )
                    backgroundIterator.remove()
                    break
                }
            }
            foregroundIterator.remove()
        }

        foregroundEvents.clear()
        backgroundEvents.clear()
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
        val date =
            dateFormatter.getDateFromMilliSeconds(Calendar.getInstance().timeInMillis - 3600 * 1000)
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
                if (currentApp.end_time == nextApp.start_time && currentApp.app_name == nextApp.app_name) {
                    val unionAppUsage = AppUsage(
                        currentApp.app_name,
                        currentApp.start_time,
                        nextApp.end_time
                    )
                    filteredAppList.add(unionAppUsage)
                    index += 2
                } else if (currentApp.start_time == currentApp.end_time) {
                    index++
                    continue
                } else {
                    filteredAppList.add(currentApp)
                    index++
                }
            } else {
                filteredAppList.add(currentApp)
                index++
            }
        }
        println("unionAppUsageRocord 함수 실행됨 \n filteredAppList: $filteredAppList")
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
