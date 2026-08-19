package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.domain.MessageSendResult
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.springframework.stereotype.Component
import java.util.*

/**
 * 알림을 실제 외부 채널로 내보낸다. 트랜잭션을 열지 않는다.
 *
 * 발송 과정에서 쓸모없다고 판명된 FCM 토큰을 정리하는 것도 이 클래스의 책임이다.
 * 그대로 두면 같은 토큰으로 계속 실패하며 재시도만 태우게 된다.
 */
@Component
class NotificationChannelSender(
    private val firebasePort: FirebasePort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
    private val externalMessagePort: ExternalMessagePort,
) {
    fun sendViaExternalMethod(notification: Notification): MessageSendResult? = when (notification.method) {
        NotificationMethod.FCM -> sendFcm(notification)
        NotificationMethod.SMS -> sendSms(notification)
    }

    private fun sendFcm(notification: Notification): MessageSendResult? {
        val receiverId = notification.targetIdentifier.toUuidOrNull()
            ?: throw InvalidInputException(
                "FCM 알림의 수신자는 사용자 식별자여야 합니다.",
                contextData = mapOf("targetIdentifier" to notification.targetIdentifier),
            )

        val token = fcmTokenPersistencePort.findByOwnerId(receiverId)
            ?: NotificationException.FCM_TOKEN_NOT_FOUND.throwIt(
                contextData = mapOf("receiverId" to receiverId),
            )

        try {
            firebasePort.send(
                token.fcmToken,
                token.deviceType,
                notification.toRendered()
            )
        } catch (exception: UnregisteredTokenException) {
            deleteTokenAndRethrowException(token, exception)
        }
        return null
    }

    private fun sendSms(notification: Notification): MessageSendResult {
        val newSms = SMS(
            senderNickname = notification.senderAlias,
            receiverNumber = notification.targetIdentifier,
            body = notification.body,
        )
        val result = externalMessagePort.send(newSms)

        return result
    }

    private fun deleteTokenAndRethrowException(
        token: FcmToken,
        exception: UnregisteredTokenException
    ): Nothing {
        token.id?.let(fcmTokenPersistencePort::deleteById)
        throw exception
    }

    private fun String.toUuidOrNull(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}
