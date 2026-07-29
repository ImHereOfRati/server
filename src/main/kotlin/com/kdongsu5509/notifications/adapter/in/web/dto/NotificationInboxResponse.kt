package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.Notification
import java.time.LocalDateTime

data class NotificationInboxResponse(
    val id: Long?,
    val senderNickname: String,
    val title: String,
    val body: String,
    val type: String,
    val path: String?,
    val isRead: Boolean,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(domain: Notification) = NotificationInboxResponse(
            id = domain.id,
            senderNickname = domain.senderNickname,
            title = domain.title,
            body = domain.body,
            type = domain.type.name,
            path = domain.path,
            isRead = domain.isRead,
            createdAt = domain.createdAt
        )
    }
}
