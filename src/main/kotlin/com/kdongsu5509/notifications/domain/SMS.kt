package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt

/**
 * SMS 도메인 객체.
 * 발송 대상 정보와 본문을 보유하며, 생성 시 필수 값·본문 길이 불변식을 스스로 검증합니다.
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
