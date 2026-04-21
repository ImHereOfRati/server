package com.kdongsu5509.notifications.application.serivce

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.notifications.application.port.out.DeleteTokenPort
import com.kdongsu5509.notifications.application.port.out.FindTokenPort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.FCMMessageTitle
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FCMErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FCMNotificationService(
    private val findTokenPort: FindTokenPort,
    private val deleteTokenPort: DeleteTokenPort,
    private val firebasePort: FirebasePort
) : NotificationToUserCasePort {

    override fun send(
        senderNickname: String,
        senderEmail: String,
        receiverEmail: String,
        type: String,
        body: String,
        extraData: Map<String, String>
    ) {
        val fcmToken: FcmToken = findReceiverFcmToken(receiverEmail)
        val notificationType = parseNotificationType(type)
        val data = buildData(senderNickname, senderEmail, notificationType, extraData)

        try {
            firebasePort.send(
                fcmToken.fcmToken,
                convertTypeToMessageTitle(type),
                body,
                data
            )
        } catch (ex: BusinessException) {
            if (ex.errorCode == FCMErrorCode.FCM_TOKEN_UNREGISTERED) {
                deleteTokenPort.deleteById(fcmToken.id!!)
            }
        }
    }

    fun convertTypeToMessageTitle(type: String): FCMMessageTitle {
        return when (parseNotificationType(type)) {
            NotificationType.FRIEND_REQUEST -> FCMMessageTitle.FRIEND_REQUEST
            NotificationType.TERMS_UPDATE -> FCMMessageTitle.DEFAULT_NOTICE
            NotificationType.LOCATION_SHARE_RECIPIENT -> FCMMessageTitle.LOCATION_SHARE_RECIPIENT
            NotificationType.ARRIVAL_CONFIRMATION -> FCMMessageTitle.ARRIVAL_CONFIRMATION
            NotificationType.DELIVERY_RESULT_NOTICE -> FCMMessageTitle.DELIVERY_RESULT_NOTICE
        }
    }

    private fun parseNotificationType(type: String): NotificationType =
        runCatching { NotificationType.valueOf(type) }
            .getOrElse { throw BusinessException(FCMErrorCode.FCM_INVALID_ARGUMENT) }

    private fun buildData(
        senderNickname: String,
        senderEmail: String,
        notificationType: NotificationType,
        extraData: Map<String, String>
    ): Map<String, String> {
        val resolvedPath = resolvePath(notificationType.pathTemplate, extraData)
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
            extraData[key] ?: throw BusinessException(FCMErrorCode.FCM_INVALID_ARGUMENT)
        }
        return resolved
    }

    private fun findReceiverFcmToken(receiverEmail: String): FcmToken {
        return findTokenPort.findByUserEmail(receiverEmail)
            ?: throw BusinessException(FCMErrorCode.FCM_TOKEN_NOT_FOUND)
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{(\\w+)\\}")
    }
}
