package io.jasperapps.appusagernd.utils

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.health.connect.client.HealthConnectClient
import io.jasperapps.appusagernd.R

// 에러 메시지 출력
class MessageDialogShower(private val context: Context) {
    fun showAlertDialogError(message: String) {
        val dialog = AlertDialog.Builder(context).setMessage(message)
        dialog?.show()
    }
}

// 로딩 메시지 출력
class ProgressDialogShower(private val context: Context) {
    fun showLoadingProgressDialog(): ProgressDialog {
        val loading = ProgressDialog(context)
        loading.setMessage(context.getString(R.string.progress_message))
        loading.create()
        return loading
    }
}

// 권한 부여가 되지 않았다는 메시지 출력
class PermissionNotGrantedDialogShower(private val context: Context) {
    fun showAlertDialogPermissionNotGranted(reason: String) {
        val message = AlertDialog.Builder(context)
            .setMessage(reason)
            .setPositiveButton(context.getString(android.R.string.ok)) { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            }
            .setNegativeButton(context.getString(android.R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .create()
        message.show()
    }
}

// Health Client가 이용 가능하지 않다는 메시지 출력
class ShowHealthClientNotAvailable(private val context: Context) {
    fun showAlertDialog() {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.health_service_not_available))
            .setMessage(context.getString(R.string.health_date_not_available))
            .setPositiveButton(context.getString(android.R.string.ok)) { _, _ ->
                val availabilityStatus = HealthConnectClient.getSdkStatus(
                    context,
                    Constants.HEALTH_APP_PACKAGE_NAME
                )
                if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                    val uriString =
                        "market://details?id=${Constants.HEALTH_APP_PACKAGE_NAME}&url=healthconnect%3A%2F%2Fonboarding"
                    if (isPackageInstalled(context)) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW).apply {
                                setPackage("com.android.vending")
                                data = Uri.parse(uriString)
                                putExtra("overlay", true)
                                putExtra("callerId", context.packageName)
                            }
                        )
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun isPackageInstalled(context: Context): Boolean {
        val pm = context.packageManager
        val appInstalled = try {
            val info = pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES)
            val label = info.applicationInfo.loadLabel(pm) as String
            label.startsWith("Google Play") && label.isNotEmpty()
        } catch (e: NameNotFoundException) {
            false
        }
        return appInstalled
    }
}

// 건강 데이터 권한이 허용되지 않을 때 메시지 출력
class HealthPermissionNotGranted(private val context: Context) {
    fun showPermissionDialog() {
        AlertDialog.Builder(context)
            .setMessage(context.getString(R.string.health_service_info))
            .setPositiveButton(context.getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}
