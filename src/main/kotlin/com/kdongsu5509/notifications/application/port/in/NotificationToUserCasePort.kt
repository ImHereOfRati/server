package com.kdongsu5509.notifications.application.port.`in`

interface NotificationToUserCasePort {
    fun send(
        senderNickname: String,
        senderEmail: String,
        receiverEmail: String,
        type: String,
        body: String,
        extraData: Map<String, String> = emptyMap()
    )
}
