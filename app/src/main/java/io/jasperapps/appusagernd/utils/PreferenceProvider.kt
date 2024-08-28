package io.jasperapps.appusagernd.utils

import android.content.Context
import android.content.SharedPreferences
import io.jasperapps.appusagernd.utils.Constants.APP_LANGUAGE

// Pereference (언어 등) 관리
class PreferenceProvider(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    fun getApplicationLanguage(): String? = sharedPreferences.getString(APP_LANGUAGE, "en")

    fun saveApplicationLanguage(lang: String) {
        sharedPreferences.edit().putString(APP_LANGUAGE, lang).apply()
    }

    companion object {
        const val APP_PREFERENCES = "app_preferences"
    }
}
