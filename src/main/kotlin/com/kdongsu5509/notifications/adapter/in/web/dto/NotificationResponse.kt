package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.Notification
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long?,
    val senderAlias: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(domain: Notification) = NotificationResponse(
            id = domain.id,
            senderAlias = domain.senderAlias,
            title = domain.title,
            body = domain.body,
            type = domain.type.name,
            isRead = domain.isRead,
            createdAt = domain.createdAt
        )
    }
}
