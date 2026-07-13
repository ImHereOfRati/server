package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationHistory
import com.kdongsu5509.notifications.domain.NotificationType
import org.springframework.stereotype.Component

@Component
class NotificationHistoryMapper {
    fun toDomain(entity: NotificationHistoryJpaEntity): NotificationHistory =
        NotificationHistory.reconstruct(
            id = entity.id,
            receiverEmail = entity.receiverEmail,
            senderNickname = entity.senderNickname,
            title = entity.title,
            body = entity.body,
            type = NotificationType.valueOf(entity.type),
            path = entity.path,
            isRead = entity.isRead,
            createdAt = entity.createdAt
        )

    fun toEntity(domain: NotificationHistory): NotificationHistoryJpaEntity =
        NotificationHistoryJpaEntity(
            id = domain.id,
            receiverEmail = domain.receiverEmail,
            senderNickname = domain.senderNickname,
            title = domain.title,
            body = domain.body,
            type = domain.type.name,
            path = domain.path,
            isRead = domain.isRead,
        )
}
