package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.domain.NotificationType

interface NotificationUseCase {
    fun send(
        senderNickname: String,
        senderEmail: String,
        receiverEmail: String,
        type: NotificationType,
        extraData: Map<String, String> = emptyMap()
    )
}
