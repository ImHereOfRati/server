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
        body: String
    ) {
        val fcmToken: FcmToken = findReceiverFcmToken(receiverEmail)
        val data = mapOf(
            "senderNickname" to senderNickname,
            "senderEmail" to senderEmail
        )

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
        return when (type) {
            NotificationType.FRIEND_REQUEST.name -> FCMMessageTitle.FRIEND_REQUEST
            NotificationType.TERMS_UPDATE.name -> FCMMessageTitle.DEFAULT_NOTICE
            NotificationType.LOCATION_SHARE_RECIPIENT.name -> FCMMessageTitle.LOCATION_SHARE_RECIPIENT
            NotificationType.ARRIVAL_CONFIRMATION.name -> FCMMessageTitle.ARRIVAL_CONFIRMATION
            NotificationType.DELIVERY_RESULT_NOTICE.name -> FCMMessageTitle.DELIVERY_RESULT_NOTICE
            else -> throw BusinessException(FCMErrorCode.FCM_INVALID_ARGUMENT)
        }
    }

    private fun findReceiverFcmToken(receiverEmail: String): FcmToken {
        return findTokenPort.findByUserEmail(receiverEmail)
            ?: throw BusinessException(FCMErrorCode.FCM_TOKEN_NOT_FOUND)
    }
}
