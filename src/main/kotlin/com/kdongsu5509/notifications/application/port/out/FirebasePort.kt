package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.FCMMessageTitle

interface FirebasePort {
    fun send(
        fcmToken: String,
        title: FCMMessageTitle,
        body: String,
        data: Map<String, String> = emptyMap()
    )
}