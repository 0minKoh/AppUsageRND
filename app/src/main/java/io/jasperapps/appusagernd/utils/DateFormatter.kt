package io.jasperapps.appusagernd.utils

import java.text.DateFormat
import java.util.Date
import java.util.Locale

// Date (날짜) 포맷
class DateFormatter {
    // ms(밀리초) 단위로 날짜 데이터를 가져옴
    fun getDateFromMilliSeconds(milliSeconds: Long): String {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.ENGLISH)
            .format(Date(milliSeconds))
    }
}

// 알림 시간 포맷
class NotificationTimeFormatter {
    // ms(밀리초) 단위로 날짜 데이터를 가져옴
    fun getDateFromMilliSeconds(milliSeconds: Long): String {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.ENGLISH)
            .format(Date(milliSeconds)) + " " + DateFormat.getTimeInstance()
            .format(Date(milliSeconds))
    }
}

// 시간 포맷
class TimeFormatter {
    // ms(밀리초) 단위로 날짜 데이터를 가져옴
    fun getTimeFromMilliSeconds(milliSeconds: Long): String {
        return DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.ENGLISH).format(Date(milliSeconds))
    }
}
