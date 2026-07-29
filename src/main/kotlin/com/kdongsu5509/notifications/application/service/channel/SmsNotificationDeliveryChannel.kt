package com.kdongsu5509.notifications.application.service.channel

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Component

@Component
class SmsNotificationDeliveryChannel(
    private val externalMessagePort: ExternalMessagePort,
) : NotificationDeliveryChannel {
    override val method: NotificationMethod = NotificationMethod.SMS

    override fun send(notification: Notification) {
        val response = externalMessagePort.send(
            SMS(
                senderNickname = notification.senderNickname,
                receiverNumber = notification.targetIdentifier,
                body = notification.body,
            )
        )
        if (!response.isSuccess) {
            NotificationException.SMS_SEND_FAILED.throwIt(
                contextData = mapOf(
                    "receiverNumber" to notification.targetIdentifier,
                    "status" to response.status,
                    "message" to response.message,
                )
            )
        }
    }
}
