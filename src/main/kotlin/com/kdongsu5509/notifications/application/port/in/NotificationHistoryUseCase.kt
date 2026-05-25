package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.domain.NotificationHistory

interface NotificationHistoryUseCase {
    fun findByReceiverEmail(email: String, page: Int, size: Int): List<NotificationHistory>
    fun markAsRead(email: String, id: Long)
}
