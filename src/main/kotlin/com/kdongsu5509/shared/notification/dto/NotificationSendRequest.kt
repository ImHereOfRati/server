package com.kdongsu5509.shared.notification.dto

data class NotificationSendRequest(
    val category: NotificationCategory,
    val sender: NotificationPersonInfo,
    val receiver: NotificationPersonInfo
)
