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

    override fun send(receiverEmail: String, type: String, body: String) {
        val fcmToken: FcmToken = findReceiverFcmToken(receiverEmail)
        try {
            firebasePort.send(receiverEmail, convertTypeToMessageTitle(type), body)
        } catch (ex: BusinessException) {
            if (ex.errorCode == FCMErrorCode.FCM_TOKEN_UNREGISTERED) {
                deleteTokenPort.deleteById(fcmToken.id!!)
            }
        }
    }

    fun convertTypeToMessageTitle(type: String): FCMMessageTitle {
        if (type == NotificationType.FRIEND_REQUEST.name) {
            return FCMMessageTitle.FRIEND_REQUEST
        }

        if (type == NotificationType.TERMS_UPDATE.name) {
            return FCMMessageTitle.DEFAULT_NOTICE
        }

        throw BusinessException(FCMErrorCode.FCM_INVALID_ARGUMENT)
    }

    private fun findReceiverFcmToken(receiverEmail: String): FcmToken {
        return findTokenPort.findByUserEmail(receiverEmail)
            ?: throw BusinessException(FCMErrorCode.FCM_TOKEN_NOT_FOUND)
    }
}
