package com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.kdongsu5509.shared.notification.dto.NotificationPersonInfo
import java.time.LocalDateTime
import java.util.*

data class NotificationMessageDto(
    val category: NotificationType,
    val sender: NotificationPersonInfo,
    val receiver: NotificationPersonInfo,
    val data: Map<String, String>? = null,

    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    /** 메시지 멱등성 키. Producer에서 반드시 생성하여 전달해야 합니다. */
    val messageId: UUID
)
