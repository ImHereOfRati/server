package com.kdongsu5509.notifications.application.port.`in`

interface NotificationUseCase {
    fun send(
        senderNickname: String,
        senderEmail: String,
        receiverEmail: String,
        type: String,
        extraData: Map<String, String> = emptyMap()
    )
}
