package com.kdongsu5509.user.adapter.out.messageQueue.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class NotificationMessageDto(
    val type: NotificationType,
    val receiverEmail: String?,
    val senderEmail: String,
    val message: String,
    val data: Map<String, String>? = null,

    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
)
