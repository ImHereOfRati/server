package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.domain.Notification

interface NotificationInboxUseCase {
    fun findByReceiverEmail(email: String, page: Int, size: Int): List<Notification>
    fun markAsRead(email: String, id: Long)
}
