package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt

/**
 * SMS 도메인 객체.
 * 발송 대상 정보를 보유하고, SMS 본문 생성 책임도 직접 담당합니다.
 */
class SMS(
    val senderNickname: String,
    val receiverNumber: String,
    val body: String
) {
    companion object {
        private const val MAX_MESSAGE_LENGTH = 45
    }

    init {
        validateRequiredFields()
        validateMessageText()
    }

    /** SMS 발송용 본문을 생성합니다. */
    fun buildMessageText(): String = body

    private fun validateMessageText() {
        if (body.length > MAX_MESSAGE_LENGTH) {
            NotificationException.SMS_BODY_TOO_LONG.throwIt(
                contextData = mapOf(
                    "length" to body.length,
                    "maxLength" to MAX_MESSAGE_LENGTH,
                    "senderNickname" to senderNickname,
                    "body" to body
                )
            )
        }
    }

    private fun validateRequiredFields() {
        if (senderNickname.isBlank() || body.isBlank()) {
            NotificationException.SMS_NOT_ALLOW_EMPTY.throwIt(
                contextData = mapOf(
                    "senderNickname" to senderNickname,
                    "body" to body
                )
            )
        }
    }
}
