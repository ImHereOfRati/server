package com.kdongsu5509.imhere.notification.adapter.out.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.kdongsu5509.imhere.notification.application.port.out.FirebasePort
import org.springframework.stereotype.Component

@Component
class FirebaseAdapter : FirebasePort {

    private val firebaseMessaging: FirebaseMessaging

    constructor(firebaseMessaging: FirebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging
    }

    private val title = "전송 완료"
    private val body = "문자 메시지 발송에 성공하였습니다"

    override fun send(fcmToken: String) {
        val fcmMessage = createFcmMessage(fcmToken)
        firebaseMessaging.send(fcmMessage)
    }

    private fun createFcmMessage(
        fcmToken: String
    ): Message? {
        return Message.builder().setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()
        )
            .setToken(fcmToken)
            .build()
    }
}