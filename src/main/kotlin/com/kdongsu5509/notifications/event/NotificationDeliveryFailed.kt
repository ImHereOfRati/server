package com.kdongsu5509.notifications.event

import com.kdongsu5509.shared.event.DomainEvent
import java.time.LocalDateTime
import java.util.UUID

data class NotificationDeliveryFailed(
    val notificationId: Long?,
    val targetIdentifier: String,
    val notificationType: String,
    val errorType: String,
    val errorMessage: String?,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
