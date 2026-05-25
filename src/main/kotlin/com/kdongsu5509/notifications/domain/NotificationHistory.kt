package com.kdongsu5509.notifications.domain

import java.time.LocalDateTime

data class NotificationHistory(
    val id: Long? = null,
    val receiverEmail: String,
    val senderNickname: String,
    val title: String,
    val body: String,
    val type: String,
    val path: String?,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun markAsRead(): NotificationHistory {
        return this.copy(isRead = true)
    }
}
