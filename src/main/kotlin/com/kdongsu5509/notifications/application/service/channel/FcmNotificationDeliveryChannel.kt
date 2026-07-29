package com.kdongsu5509.notifications.application.service.channel

import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.type.NotFoundException
import org.springframework.stereotype.Component

@Component
class FcmNotificationDeliveryChannel(
    private val firebasePort: FirebasePort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
) : NotificationDeliveryChannel {
    override val method: NotificationMethod = NotificationMethod.FCM

    override fun send(notification: Notification) {
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
}
