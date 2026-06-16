package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
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
        private const val SERVICE_NAME = "ImHere"
        private const val MAX_MESSAGE_LENGTH = 45
        private const val MSG_FORMAT = "[%s]\n%s 도착\n발신자: %s"
    }

    init {
        validateRequiredFields()
        validateMessageText()
    }

    /** SMS 발송용 본문을 생성합니다. */
    fun buildMessageText(): String = renderMessageText()

    private fun renderMessageText(): String = String.format(
        Locale.KOREAN,
        MSG_FORMAT,
        SERVICE_NAME,
        location,
        senderNickname
    )

    private fun validateMessageText() {
        val messageText = renderMessageText()
        if (messageText.length > MAX_MESSAGE_LENGTH) {
            NotificationException.SMS_BODY_TOO_LONG.throwIt(
                contextData = mapOf(
                    "length" to messageText.length,
                    "maxLength" to MAX_MESSAGE_LENGTH,
                    "senderNickname" to senderNickname,
                    "location" to location
                )
            )
        }
    }

    private fun validateRequiredFields() {
        if (senderNickname.isBlank() || location.isBlank()) {
            NotificationException.SMS_NOT_ALLOW_EMPTY.throwIt(
                contextData = mapOf(
                    "senderNickname" to senderNickname,
                    "location" to location
                )
            )
        }
    }
}
