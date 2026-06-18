package com.kdongsu5509.notifications.application.port.`in`

interface MessageSendUseCase {
    fun send(senderNickname: String, receiverNumber: String, body: String)
    fun sendMultiple(
        senderNickname: String,
        receiverNumbers: List<String>,
        body: String
    )
}
