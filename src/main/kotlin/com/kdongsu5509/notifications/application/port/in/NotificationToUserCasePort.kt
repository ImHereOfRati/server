package com.kdongsu5509.notifications.application.port.`in`

interface NotificationToUserCasePort {
    fun send(receiverEmail: String, type: String, body: String)
}
