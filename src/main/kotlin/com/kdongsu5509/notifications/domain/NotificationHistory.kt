package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import java.time.LocalDateTime

/**
 * 알림 이력 도메인 객체.
 *
 * - [create]: 새 알림 이력 생성 시 사용. isRead는 항상 false로 고정됩니다.
 * - [reconstruct]: 영속성 레이어에서 복원 시 사용.
 * - [markAsRead]: 읽음 처리합니다. 이미 읽음 상태라면 현재 객체를 그대로 반환합니다(idempotent).
 */
class NotificationHistory internal constructor(
    val id: Long? = null,
    val receiverEmail: String,
    val senderNickname: String,
    val title: String,
    val body: String,
    val type: String,
    val path: String? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime? = null
) {
    companion object {
        /** 새 알림 이력 생성. isRead는 항상 false */
        fun create(
            receiverEmail: String,
            senderNickname: String,
            title: String,
            body: String,
            type: String,
            path: String?
        ): NotificationHistory {
            requireNotBlank(receiverEmail, senderNickname, title, body, type)
            return NotificationHistory(
                id = null,
                receiverEmail = receiverEmail,
                senderNickname = senderNickname,
                title = title,
                body = body,
                type = type,
                path = path,
                isRead = false,
                createdAt = null
            )
        }

        private fun requireNotBlank(
            receiverEmail: String,
            senderNickname: String,
            title: String,
            body: String,
            type: String
        ) {
            val blankFields = buildList {
                if (receiverEmail.isBlank()) add("receiverEmail")
                if (senderNickname.isBlank()) add("senderNickname")
                if (title.isBlank()) add("title")
                if (body.isBlank()) add("body")
                if (type.isBlank()) add("type")
            }
            if (blankFields.isNotEmpty()) {
                NotificationException.NOTIFICATION_INVALID_FIELD.throwIt(
                    contextData = mapOf("blankFields" to blankFields)
                )
            }
        }

        /** 영속성 레이어에서 복원 시 사용 */
        fun reconstruct(
            id: Long?,
            receiverEmail: String,
            senderNickname: String,
            title: String,
            body: String,
            type: String,
            path: String?,
            isRead: Boolean,
            createdAt: LocalDateTime?
        ): NotificationHistory = NotificationHistory(
            id, receiverEmail, senderNickname, title, body, type, path, isRead, createdAt
        )
    }

    /**
     * 읽음 처리된 새 NotificationHistory를 반환합니다.
     * 이미 읽음 상태인 경우 현재 객체를 그대로 반환합니다 (idempotent).
     */
    fun markAsRead(): NotificationHistory {
        if (isRead) return this
        return NotificationHistory(id, receiverEmail, senderNickname, title, body, type, path, true, createdAt)
    }
}
