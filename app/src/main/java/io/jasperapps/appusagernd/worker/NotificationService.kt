package io.jasperapps.appusagernd.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.jasperapps.appusagernd.R
import io.jasperapps.appusagernd.database.DataUploader
import io.jasperapps.appusagernd.helper.NotificationHelper
import io.jasperapps.appusagernd.model.NotificationInfo
import io.jasperapps.appusagernd.utils.Constants
import io.jasperapps.appusagernd.utils.GetLauncher
import io.jasperapps.appusagernd.utils.NotificationData
import io.jasperapps.appusagernd.utils.PreferenceProvider
import io.jasperapps.appusagernd.utils.TimedNotificationThreshold
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationService : Service() {
    private val timedNotificationThreshold = TimedNotificationThreshold()
    private val userUuid: String = Firebase.auth.currentUser?.uid ?: ""
    private lateinit var dataUploader: DataUploader
    private lateinit var preferenceProvider: PreferenceProvider
    private var timeInAppMin = 0L
    private var appName: String = ""
    private lateinit var getLauncher: GetLauncher
    private val foregroundEvents = mutableListOf<UsageEvents.Event>()
    private lateinit var notificationHelper: NotificationHelper
    private val notificationData = NotificationData()

    private var scope = CoroutineScope(Dispatchers.IO + Job())

    private val screenOfReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> {
                    Log.i("Screen info", "Timer started")

                    scope = CoroutineScope(Dispatchers.IO + Job())
                    getAppDelayed()
                }

                Intent.ACTION_SCREEN_ON -> {
                    Log.i("Screen info", "Screen UNLOCKED")
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Log.i("Screen info", "Screen LOCKED")
                    timeInAppMin = 0
                    scope.cancel()
                }

                else -> Log.i("Screen info", "${intent.action}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun getForegroundApp() {
        val userStatsManager =
            getSystemService(AppCompatActivity.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvent = userStatsManager.queryEvents(
            System.currentTimeMillis() - 1000 * 60,
            System.currentTimeMillis()
        )
        while (usageEvent.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvent.getNextEvent(event)
            println("hasNextEvent")
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundEvents.add(event)
            }
        }
        println("break")
        if (foregroundEvents.isNotEmpty()) {
            if (foregroundEvents.last().packageName != getLauncher()) {
                timeInAppMin += DELAY_MIN
            }

            if (appName != foregroundEvents.last().packageName) {
                println("timeInAppmin 초기화! appName: $appName, packageName: ${foregroundEvents.last().packageName}")
                timeInAppMin = 0
            }
            Log.i("appInfo", foregroundEvents.last().packageName)
            Log.i("timeInfo", timeInAppMin.toString())
            println("timeInAppMin: $timeInAppMin")
            val notificationTimeInMin = timedNotificationThreshold.getNotificationTime()

            if (timeInAppMin >= notificationTimeInMin) {
                if (notificationData.getEnabledNotification()) {
                    Log.i("Screen info", "sending notification")
                    notificationHelper.createNotification(
                        notificationTimeInMin.toString(),
                        foregroundEvents.last().packageName
                    )
                    dataUploader.saveNotification(
                        userUuid,
                        NotificationInfo(
                            foregroundEvents.last().packageName,
                            timedNotificationThreshold.getNotificationTime().toString(),
                            System.currentTimeMillis() / 1000
                        )
                    )
                }
                timeInAppMin = 0
            }
            appName = foregroundEvents.last().packageName
        } else {
            timeInAppMin = 0
        }
        delay(TimeUnit.MINUTES.toMillis(DELAY_MIN))
        getAppDelayed()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        dataUploader = DataUploader(this)
        getLauncher = GetLauncher(this)
        notificationHelper = NotificationHelper(this)
        preferenceProvider = PreferenceProvider(this)

        val channelName = getString(R.string.foreground_service_chanel_name)
        notificationHelper.createNotificationChannel(
            Constants.FOREGROUND_SERVICE_CHANNEL_ID,
            channelName
        )

        val notification: Notification =
            Notification.Builder(this, Constants.FOREGROUND_SERVICE_CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_foreground_service))
                .setContentText(getText(R.string.notification_message))
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.ic_baseline_add_alert_24)
                .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        }
        getAppDelayed()

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenOfReceiver, intentFilter)
    }

    private fun setLocale() {
        val language = preferenceProvider.getApplicationLanguage()
        val locale = Locale(language ?: "en")
        Locale.setDefault(locale)
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun getAppDelayed() {
        setLocale()
        scope.launch {
            getForegroundApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        unregisterReceiver(screenOfReceiver)
        Log.i("NotificationService", "Service done")
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val DELAY_MIN = 1L
    }
}
