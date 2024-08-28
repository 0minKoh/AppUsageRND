package io.jasperapps.appusagernd.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.jasperapps.appusagernd.utils.PreferenceProvider
import java.util.Locale

class LocaleReceiver : BroadcastReceiver() {

    // 언어 초기화
    // 브로드캐스트 메시지를 수신했을 때 실행
    override fun onReceive(context: Context, intent: Intent?) {
        val preferenceProvider = PreferenceProvider(context)
        preferenceProvider.saveApplicationLanguage(Locale.getDefault().language)
    }
}
