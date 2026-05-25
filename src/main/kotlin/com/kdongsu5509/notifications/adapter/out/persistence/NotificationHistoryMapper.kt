package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationHistory
import org.springframework.stereotype.Component

@Component
class NotificationHistoryMapper {
    fun toDomain(entity: NotificationHistoryJpaEntity): NotificationHistory {
        return NotificationHistory(
            id = entity.id,
            receiverEmail = entity.receiverEmail,
            senderNickname = entity.senderNickname,
            title = entity.title,
            body = entity.body,
            type = entity.type,
            path = entity.path,
            isRead = entity.isRead,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(domain: NotificationHistory): NotificationHistoryJpaEntity {
        return NotificationHistoryJpaEntity(
            id = domain.id,
            receiverEmail = domain.receiverEmail,
            senderNickname = domain.senderNickname,
            title = domain.title,
            body = domain.body,
            type = domain.type,
            path = domain.path,
            isRead = domain.isRead,
        )
    }
}
