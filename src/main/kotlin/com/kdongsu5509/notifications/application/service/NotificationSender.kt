package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.support.exception.type.NotFoundException
import org.springframework.stereotype.Component

@Component
class NotificationSender(
    private val firebasePort: FirebasePort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
    private val externalMessagePort: ExternalMessagePort,
) {
    fun send(notification: Notification) = when (notification.method) {
        NotificationMethod.FCM -> sendFcm(notification)
        NotificationMethod.SMS -> sendSms(notification)
    }

    private fun sendFcm(notification: Notification) {
        val token = fcmTokenPersistencePort.findByUserEmail(notification.targetIdentifier)
            ?: throw NotFoundException(
                "수신자의 FCM 토큰을 찾을 수 없습니다.",
                contextData = mapOf("receiverEmail" to notification.targetIdentifier),
            )

        try {
            firebasePort.send(token.fcmToken, token.deviceType, notification.toRendered())
        } catch (exception: UnregisteredTokenException) {
            token.id?.let(fcmTokenPersistencePort::deleteById)
            throw exception
        }
    }

    private fun sendSms(notification: Notification) {
        val result = externalMessagePort.send(
            SMS(
                senderNickname = notification.senderNickname,
                receiverNumber = notification.targetIdentifier,
                body = notification.body,
            )
        )
        if (!result.isSuccess) {
            NotificationException.SMS_SEND_FAILED.throwIt(
                contextData = mapOf(
                    "receiverNumber" to notification.targetIdentifier,
                    "status" to result.status,
                    "message" to result.message,
                )
            )
        }
    }
}
