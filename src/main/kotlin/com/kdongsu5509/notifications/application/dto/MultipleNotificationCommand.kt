package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod

data class MultipleNotificationCommand(
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifiers: List<String>,
    val type: String,
    val extraData: Map<String, String> = emptyMap()
)
