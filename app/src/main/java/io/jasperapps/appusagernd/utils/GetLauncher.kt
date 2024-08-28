package io.jasperapps.appusagernd.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

// Launcher 데이터 GET
class GetLauncher(private val context: Context) {
    operator fun invoke(): String {
        val localPackageManager = context.packageManager
        val intent = Intent("android.intent.action.MAIN")
        intent.addCategory("android.intent.category.HOME")
        return localPackageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)!!.activityInfo.packageName
    }
}
