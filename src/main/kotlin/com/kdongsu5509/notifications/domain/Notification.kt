package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.domain.Notification.Companion.MAX_ATTEMPTS
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import java.time.LocalDateTime
import java.util.*

class Notification internal constructor(
    val id: Long? = null,
    val deduplicationKey: String, // 멱등성 키 : DeduplicationKey
    val targetIdentifier: String,
    val method: NotificationMethod,
    val senderAlias: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val extraData: Map<String, String> = emptyMap(),
    val status: NotificationStatus = NotificationStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val sentAt: LocalDateTime? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime? = null,
) {
    companion object {
        const val MAX_ATTEMPTS = 3

        const val LAST_ERROR_MAX_LENGTH = 500

        fun request(
            dedupeKey: String,
            targetIdentifier: String,
            method: NotificationMethod,
            rendered: RenderedNotification,
            bodyOverride: String? = null,
        ): Notification {
            val body = bodyOverride ?: rendered.body
            requireNotBlank(
                "dedupeKey" to dedupeKey,
                "targetIdentifier" to targetIdentifier,
                "title" to rendered.title,
                "body" to body,
            )
            return Notification(
                deduplicationKey = dedupeKey,
                targetIdentifier = targetIdentifier,
                method = method,
                senderAlias = rendered.senderAlias,
                type = rendered.type,
                title = rendered.title,
                body = body,
                extraData = rendered.data,
            )
        }

        fun dedupeKeyOf(eventId: UUID, method: NotificationMethod): String = "$eventId:$method"

        fun reconstruct(
            id: Long?,
            dedupeKey: String,
            targetIdentifier: String,
            method: NotificationMethod,
            senderAlias: String,
            type: NotificationType,
            title: String,
            body: String,
            extraData: Map<String, String>,
            status: NotificationStatus,
            attempts: Int,
            lastError: String?,
            sentAt: LocalDateTime?,
            isRead: Boolean,
            createdAt: LocalDateTime?,
        ): Notification = Notification(
            id, dedupeKey, targetIdentifier, method, senderAlias, type,
            title, body, extraData, status, attempts, lastError, sentAt, isRead, createdAt,
        )

        private fun requireNotBlank(vararg fields: Pair<String, String>) {
            val blankFields = fields.filter { it.second.isBlank() }.map { it.first }
            if (blankFields.isNotEmpty()) {
                NotificationException.NOTIFICATION_INVALID_FIELD.throwIt(
                    contextData = mapOf("blankFields" to blankFields)
                )
            }
        }
    }

    val isInbox: Boolean
        get() = method == NotificationMethod.FCM && status == NotificationStatus.SENT

    val isDeliverable: Boolean
        get() = status.isSendable()

    fun toRendered(): RenderedNotification =
        RenderedNotification(
            type = type,
            senderAlias = senderAlias,
            title = title,
            body = body,
            channel = PushChannel.of(type),
            data = extraData,
        )

    fun markSent(now: LocalDateTime): Notification {
        if (status == NotificationStatus.SENT) return this
        requireDeliverable()
        return replaced(status = NotificationStatus.SENT, sentAt = now, lastError = null)
    }

    fun markFailed(reason: String): Notification {
        requireDeliverable()
        val nextAttempts = attempts + 1
        val nextStatus =
            if (nextAttempts >= MAX_ATTEMPTS) NotificationStatus.DEAD else NotificationStatus.FAILED
        return replaced(
            status = nextStatus,
            attempts = nextAttempts,
            lastError = reason.take(LAST_ERROR_MAX_LENGTH),
        )
    }

    fun retry(): Notification {
        if (status != NotificationStatus.DEAD) NotificationException.NOTIFICATION_NOT_RETRYABLE.throwIt()
        return replaced(status = NotificationStatus.PENDING, attempts = 0, lastError = null)
    }

    fun markAsRead(): Notification {
        if (isRead) return this
        if (status != NotificationStatus.SENT) NotificationException.NOTIFICATION_NOT_DELIVERED.throwIt()
        return replaced(isRead = true)
    }

    private fun requireDeliverable() {
        if (!isDeliverable) {
            NotificationException.NOTIFICATION_NOT_DELIVERABLE.throwIt(
                contextData = mapOf("status" to status.name)
            )
        }
    }

    private fun replaced(
        status: NotificationStatus = this.status,
        attempts: Int = this.attempts,
        lastError: String? = this.lastError,
        sentAt: LocalDateTime? = this.sentAt,
        isRead: Boolean = this.isRead,
    ): Notification = Notification(
        id = id,
        deduplicationKey = deduplicationKey,
        targetIdentifier = targetIdentifier,
        method = method,
        senderAlias = senderAlias,
        type = type,
        title = title,
        body = body,
        extraData = extraData,
        status = status,
        attempts = attempts,
        lastError = lastError,
        sentAt = sentAt,
        isRead = isRead,
        createdAt = createdAt,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Notification) return false

        if (id != null && other.id != null) return id == other.id
        return deduplicationKey == other.deduplicationKey
    }

    override fun hashCode(): Int = id?.hashCode() ?: deduplicationKey.hashCode()
}
