package io.jasperapps.appusagernd.utils

import android.app.Activity
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import io.jasperapps.appusagernd.R
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONException
import org.json.JSONObject
import com.google.gson.annotations.SerializedName

// 알림 활성화 여부
// class NotificationsEnabled(private val activity: Activity) {
//     operator fun invoke(): Boolean {
//         val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
//         val configSettings = remoteConfigSettings {
//             // minimumFetchIntervalInSeconds = Constants.MINIMUM_FETCH_INTERVAL
//             minimumFetchIntervalInSeconds = 10L
//         }
//         var isNotificationsEnabled = false
//         remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
//         remoteConfig.setConfigSettingsAsync(configSettings)
//         remoteConfig.fetchAndActivate()
//             .addOnCompleteListener(activity) { task ->
//                 if (task.isSuccessful) {
//                     val updated = task.result
//                     if (updated) {
//                         isNotificationsEnabled = updated
//                     }
//                     Log.d("Info remote", "Config params updated: $updated")
//                 }
//             }
//         return isNotificationsEnabled
//     }
// }
class NotificationsEnabled(private val activity: Activity) {
    // private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    operator fun invoke() {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = Constants.MINIMUM_FETCH_INTERVAL
            // minimumFetchIntervalInSeconds = 10L
        }
        // var isNotificationsEnabled = false
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    if (updated) {
                        // isNotificationsEnabled = updated
                        println("isNotificationEnabled Update: $updated")
                    }
                    Log.d("Info remote", "Config params: $updated")
                }
            }

        // remoteConfig의 값들 중 하나가 업데이트되면 실행됨
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate : ConfigUpdate) {
                Log.d("onUpdate", "Updated keys: " + configUpdate.updatedKeys)
                // println("currentUserEmail: ${auth.currentUser?.email}")

                if (configUpdate.updatedKeys.contains("NotificationsEnabled")) {
                    remoteConfig.activate().addOnCompleteListener {
                        println("${configUpdate.updatedKeys} Update: ${Constants.NOTIFICATIONS_ENABLED_LEY}, Value: ${remoteConfig.getBoolean(Constants.NOTIFICATIONS_ENABLED_LEY)}")
                    }
                }
            }

            override fun onError(error : FirebaseRemoteConfigException) {
                Log.w("error", "Config update error with code: " + error.code, error)
            }
        })
    }
}

// 알림 시간 설정
class TimedNotificationThreshold {
    fun getNotificationTime(): Long {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = Constants.MINIMUM_FETCH_INTERVAL
        }
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
        return remoteConfig.getLong(Constants.TIME_NOTIFICATION_KEY)
    }
}

// 알림 데이터
class NotificationData {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    fun getEnabledNotification(): Boolean {
        // Remote Config에서 JSON 형식의 NotificationEnabledList 값을 String으로 가져옴
        val notificationEnabledJsonString = remoteConfig.getValue("NotificationEnabledList").asString()

        // JSON 문자열을 JSONObject로 파싱
        val notificationEnabledList = mutableListOf<String>()
        try {
            val jsonObject = JSONObject(notificationEnabledJsonString)
            for (i in 0 until jsonObject.length()) {
                notificationEnabledList.add(jsonObject.getString(i.toString()))
            }
        } catch (e: JSONException) {
            println("JSON 파싱 중 오류가 발생했습니다: ${e.message}")
            return false
        }

        val currentUserEmail = auth.currentUser?.email
        if (notificationEnabledList.contains(currentUserEmail)) {
            // Whitelist에 포함되어 있는지 검증
            println("알림 활성화 리스트에 포함된 이메일입니다.")
            return remoteConfig.getBoolean(Constants.NOTIFICATIONS_ENABLED_LEY)
        } else {
            // Whitelist에 포함되어 있지 않았을 때
            println("알림 활성화 리스트에 포함되지 않은 이메일입니다.")
            println("$notificationEnabledList 에 $currentUserEmail 이 포함되어 있지 않습니다.")
            return false
        }
    }
}



