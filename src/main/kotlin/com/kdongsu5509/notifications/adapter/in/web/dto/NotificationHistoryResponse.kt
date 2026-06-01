package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.NotificationHistory
import java.time.LocalDateTime

data class NotificationHistoryResponse(
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
        fun from(domain: NotificationHistory) = NotificationHistoryResponse(
            id = domain.id,
            senderNickname = domain.senderNickname,
            title = domain.title,
            body = domain.body,
            type = domain.type,
            path = domain.path,
            isRead = domain.isRead,
            createdAt = domain.createdAt
        )
    }
}
