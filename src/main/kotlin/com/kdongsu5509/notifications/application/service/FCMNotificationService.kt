package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.application.port.out.NotificationHistoryPersistencePort
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.domain.NotificationHistory
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.support.exception.type.InvalidInputException
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
        type: String,
        extraData: Map<String, String>
    ) {
        val fcmToken: FcmToken = findReceiverFcmToken(receiverEmail)
        val notificationType = parseNotificationType(type)
        val data = buildData(senderNickname, senderEmail, notificationType, extraData)

        val title = NotificationMessageGenerator.getMessageTitle(notificationType)
        val body = NotificationMessageGenerator.getMessageBody(notificationType, senderNickname)

        try {
            firebasePort.send(
                fcmToken.fcmToken,
                title,
                body,
                data
            )
        } catch (ex: ImHereBaseException) {
            if (ex.contextData["unregistered"] == true) {
                fcmTokenPersistencePort.deleteById(fcmToken.id!!)
                return // 토큰 만료 → 발송 실패로 간주, 이력 저장 하지 않음
            }
            throw ex // 그 외 예외는 재전파
        }

        notificationHistoryPersistencePort.save(
            NotificationHistory.create(
                receiverEmail = receiverEmail,
                senderNickname = senderNickname,
                title = title,
                body = body,
                type = type,
                path = data["path"]
            )
        )
    }

    private fun parseNotificationType(type: String): NotificationType =
        runCatching { NotificationType.valueOf(type) }
            .getOrElse { throw InvalidInputException("잘못된 알림 타입입니다: $type") }

    private fun buildData(
        senderNickname: String,
        senderEmail: String,
        notificationType: NotificationType,
        extraData: Map<String, String>
    ): Map<String, String> {
        val resolvedPath = resolvePath(notificationType.appPath, extraData)
        return extraData + mapOf(
            "senderNickname" to senderNickname,
            "senderEmail" to senderEmail,
            "type" to notificationType.name,
            "path" to resolvedPath
        )
    }

    private fun resolvePath(template: String, extraData: Map<String, String>): String {
        val resolved = PLACEHOLDER_REGEX.replace(template) { match ->
            val key = match.groupValues[1]
            extraData[key] ?: throw InvalidInputException("알림 경로 생성 중 필수 데이터($key)가 누락되었습니다.")
        }
        return resolved
    }

    private fun findReceiverFcmToken(receiverEmail: String): FcmToken {
        return fcmTokenPersistencePort.findByUserEmail(receiverEmail)
            ?: throw NotFoundException(
                "수신자의 FCM 토큰을 찾을 수 없습니다.",
                contextData = mapOf("receiverEmail" to receiverEmail)
            )
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{(\\w+)\\}")
    }
}
