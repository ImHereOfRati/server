package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.application.port.out.NotificationHistoryPersistencePort
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.type.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FCMNotificationService(
    private val firebasePort: FirebasePort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
    private val notificationHistoryPersistencePort: NotificationHistoryPersistencePort
) : NotificationUseCase {

    override fun send(
        senderNickname: String,
        senderEmail: String,
        receiverEmail: String,
        type: NotificationType,
        extraData: Map<String, String>
    ) {
        val fcmToken: FcmToken = findReceiverFcmToken(receiverEmail)
        val rendered = type.render(senderNickname, senderEmail, extraData)

        try {
            firebasePort.send(fcmToken.fcmToken, rendered.title, rendered.body, rendered.data)
        } catch (ex: UnregisteredTokenException) {
            // 토큰 만료 → 발송 실패로 간주, 토큰 삭제 후 이력 저장 하지 않음
            fcmTokenPersistencePort.deleteById(fcmToken.id!!)
            return
        }

        notificationHistoryPersistencePort.save(rendered.toHistory(receiverEmail))
    }

    private fun findReceiverFcmToken(receiverEmail: String): FcmToken {
        return fcmTokenPersistencePort.findByUserEmail(receiverEmail)
            ?: throw NotFoundException(
                "수신자의 FCM 토큰을 찾을 수 없습니다.",
                contextData = mapOf("receiverEmail" to receiverEmail)
            )
    }
}
