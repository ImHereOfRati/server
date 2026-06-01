package com.kdongsu5509.shared.notification.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.*

data class NotificationQueueMessage(
    val category: NotificationCategory,
    val sender: NotificationPersonInfo,
    val receiver: NotificationPersonInfo,
    val data: Map<String, String>? = null,

    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    val messageId: UUID = UUID.randomUUID()
) {
    companion object {
        fun from(request: NotificationSendRequest, data: Map<String, String>? = null) = NotificationQueueMessage(
            category = request.category,
            sender = request.sender,
            receiver = request.receiver,
            data = data
        )
    }
}
