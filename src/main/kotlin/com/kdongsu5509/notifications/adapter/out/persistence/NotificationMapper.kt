package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationType
import org.springframework.stereotype.Component

@Component
class NotificationMapper {

    fun toDomain(entity: NotificationJpaEntity): Notification =
        Notification.reconstruct(
            id = entity.id,
            dedupeKey = entity.dedupeKey,
            targetIdentifier = entity.targetIdentifier,
            method = entity.method,
            senderAlias = entity.senderAlias,
            type = NotificationType.valueOf(entity.type),
            title = entity.title,
            body = entity.body,
            extraData = entity.extraData,
            status = entity.status,
            attempts = entity.attempts,
            lastError = entity.lastError,
            sentAt = entity.sentAt,
            isRead = entity.isRead,
            createdAt = entity.createdAt,
        )

    fun toEntity(domain: Notification): NotificationJpaEntity =
        NotificationJpaEntity(
            id = domain.id,
            dedupeKey = domain.deduplicationKey,
            targetIdentifier = domain.targetIdentifier,
            method = domain.method,
            senderAlias = domain.senderAlias,
            type = domain.type.name,
            title = domain.title,
            body = domain.body,
            extraData = domain.extraData,
            status = domain.status,
            attempts = domain.attempts,
            lastError = domain.lastError,
            sentAt = domain.sentAt,
            isRead = domain.isRead,
        ).apply {
            domain.createdAt?.let { createdAt = it }
        }
}
