package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.Notification
import java.time.LocalDateTime

data class FailedNotificationResponse(
    val id: Long?,
    val targetIdentifier: String,
    val method: String,
    val senderEmail: String,
    val senderNickname: String,
    val type: String,
    val title: String,
    val body: String,
    val status: String,
    val attempts: Int,
    val lastError: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(notification: Notification): FailedNotificationResponse =
            FailedNotificationResponse(
                id = notification.id,
                targetIdentifier = notification.targetIdentifier,
                method = notification.method.name,
                senderEmail = notification.senderEmail,
                senderNickname = notification.senderNickname,
                type = notification.type.name,
                title = notification.title,
                body = notification.body,
                status = notification.status.name,
                attempts = notification.attempts,
                lastError = notification.lastError,
                createdAt = notification.createdAt,
            )
    }
}
