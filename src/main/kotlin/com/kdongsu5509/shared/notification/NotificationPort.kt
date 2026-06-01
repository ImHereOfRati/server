package com.kdongsu5509.shared.notification

import com.kdongsu5509.shared.notification.dto.NotificationSendRequest

interface NotificationPort {
    fun send(request: NotificationSendRequest)
}
