package com.kdongsu5509.notifications.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * SMS 도메인 객체.
 * 발송 대상 정보를 보유하고, SMS 본문 생성 책임도 직접 담당합니다.
 */
class SMS(
    val senderNickname: String,
    val receiverNumber: String,
    val location: String
) {
    companion object {
        private const val MSG_FORMAT =
            "%s에 안전하게 도착하였습니다.\n\n보낸 분 : %s\n시간: %s\n\nService by ImHere"
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("a h시 m분").withLocale(Locale.KOREAN)
    }

    /** SMS 발송용 본문을 생성합니다. */
    fun buildMessageText(): String = String.format(
        MSG_FORMAT,
        location,
        senderNickname,
        LocalDateTime.now().format(DATE_FORMATTER)
    )
}