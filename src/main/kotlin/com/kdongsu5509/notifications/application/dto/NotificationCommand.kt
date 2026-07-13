package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType

data class NotificationCommand(
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap()
) {
    /** 본문 기반 알림(SMS 등)의 본문. `extraData["body"]`의 명명된 접근자이며 공백은 null로 간주한다. */
    val body: String? get() = extraData[BODY_KEY]?.takeIf { it.isNotBlank() }

    companion object {
        const val BODY_KEY = "body"

        /** 발송 결과 수신증을 보내는 시스템 발송자 표기. */
        private const val DELIVERY_RECEIPT_SENDER = "ImHere"

        /**
         * 발송 요청자(sender)에게 결과 수신증을 보내는 커맨드를 만든다.
         * 성공/실패 경로에서 손으로 조립하던 "ImHere·FCM·자기 자신 발송" 규칙을 한곳에 응집한다.
         */
        fun deliveryReceipt(senderEmail: String, type: NotificationType): NotificationCommand =
            NotificationCommand(
                senderNickname = DELIVERY_RECEIPT_SENDER,
                senderEmail = senderEmail,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifier = senderEmail,
                type = type,
                extraData = emptyMap(),
            )
    }
}
