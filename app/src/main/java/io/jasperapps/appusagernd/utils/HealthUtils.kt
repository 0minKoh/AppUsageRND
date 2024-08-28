package io.jasperapps.appusagernd.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import io.jasperapps.appusagernd.utils.Constants.HEALTH_APP_PACKAGE_NAME

// Health Client 연결 상태 점검
class HealthUtils(private val context: Context) {
    fun checkIfHealthConnectAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(
            context,
            HEALTH_APP_PACKAGE_NAME
        ) != HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }
}
