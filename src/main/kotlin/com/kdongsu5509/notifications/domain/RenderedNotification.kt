package com.kdongsu5509.notifications.domain

data class RenderedNotification(
    val type: NotificationType,
    val senderAlias: String,
    val title: String,
    val body: String,
    val channel: PushChannel,
    val data: Map<String, String>,
)
