package com.kdongsu5509.support.logger

object SensitiveMasker {
    fun email(value: String?): String? {
        if (value == null) return null
        val at = value.indexOf('@')
        if (at <= 1 || at == value.lastIndex) return "*****"
        return "${value.first()}***${value.substring(at)}"
    }

    fun token(value: String?): String? {
        if (value == null) return null
        if (value.length <= 10) return "*****"
        return "${value.take(6)}…${value.takeLast(4)} (len=${value.length})"
    }

    fun phone(value: String?): String? {
        if (value == null) return null
        val digits = value.filter(Char::isDigit)
        if (digits.length <= 7) return "*****"
        return "${digits.take(3)}****${digits.takeLast(4)}"
    }
}
