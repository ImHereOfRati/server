package com.kdongsu5509.imhere.notification.application.service

import com.kdongsu5509.imhere.common.exception.domain.notification.FcmTokenNotFoundException
import com.kdongsu5509.imhere.notification.application.domain.FcmToken
import com.kdongsu5509.imhere.notification.application.port.`in`.SelfNotificationUserCasePort
import com.kdongsu5509.imhere.notification.application.port.out.FindTokenPort
import com.kdongsu5509.imhere.notification.application.port.out.FirebasePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class SelfNotificationService(
    private val findTokenPort: FindTokenPort,
    private val firebasePort: FirebasePort
) : SelfNotificationUserCasePort {

    @Transactional(readOnly = true)
    override fun sendToMe(email: String) {
        val myFcmTokenInfo: FcmToken = findTokenPort.findByUserEmail(email)
            ?: throw FcmTokenNotFoundException()

        firebasePort.send(myFcmTokenInfo.fcmToken)
    }
}