package io.jasperapps.appusagernd.receiver

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.jasperapps.appusagernd.worker.NotificationService

class ServiceReceiver : BroadcastReceiver() {

    // 알림 권한 허용 확인
    // 브로드캐스트 메시지를 수신했을 때 실행
    override fun onReceive(context: Context, intent: Intent?) {
        if (context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Intent(context, NotificationService::class.java).also { i ->
                context.startForegroundService(i)
            }
        }
    }
}
