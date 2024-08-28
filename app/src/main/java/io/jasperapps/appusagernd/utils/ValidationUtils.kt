package io.jasperapps.appusagernd.utils

import android.util.Patterns

// 이메일 주소 검증
class ValidationEmail {
    operator fun invoke(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }
}

// 패스워드 검증 (6자 이상)
class ValidationPassword {
    operator fun invoke(password: String): Boolean {
        return password.length >= 6
    }
}

// 나이 검증 (1 ~ 99)
class ValidationAge {
    operator fun invoke(age: Int): Boolean {
        return age in 1..99
    }
}
