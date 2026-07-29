package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import java.time.LocalDateTime
import java.util.UUID

/**
 * 알림 한 건. 발송 요청부터 발송 결과, 수신자의 읽음까지를 하나의 애그리게이트가 소유한다.
 *
 * 예전에는 별도 이력이 "발송에 성공한 뒤 남기는 읽음 처리용 기록"일 뿐이었고,
 * 발송 중/실패/재시도 소진 상태를 큐 위치가 아니라 도메인 상태로 명시한다.
 * 브로커를 걷어내면서 그 책임을 도메인으로 되돌린다.
 *
 * [dedupeKey]에 걸린 유니크 제약이 곧 멱등성 장치다. 같은 발송 요청이 두 번 들어와도 행은 하나만 생기므로,
 * 인스턴스마다 따로 놀던 로컬 캐시 기반 멱등성 판정이 필요 없어진다.
 * 다만 이것으로 exactly-once가 되지는 않는다 — 외부 발송이 성공한 직후 [markSent] 커밋 전에 죽으면
 * 회수 과정에서 다시 발송될 수 있다. 보장 수준은 "at-least-once 전달 + 중복 억제"다.
 *
 * 상태 전이는 값을 바꾸지 않고 새 인스턴스를 돌려준다([replaced]). 되돌아온 값이 곧 규칙을 지킨 근거가 된다.
 */
class Notification internal constructor(
    val id: Long? = null,
    val dedupeKey: String,
    val targetIdentifier: String,
    val method: NotificationMethod,
    val senderEmail: String,
    val senderNickname: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val path: String? = null,
    val extraData: Map<String, String> = emptyMap(),
    val status: NotificationStatus = NotificationStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val sentAt: LocalDateTime? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime? = null,
) {
    companion object {
        /** 이 횟수만큼 실패하면 [NotificationStatus.DEAD]가 된다. */
        const val MAX_ATTEMPTS = 3

        /** 실패 사유는 원인 파악용이므로 컬럼 길이를 넘기지 않게 잘라 담는다. */
        const val LAST_ERROR_MAX_LENGTH = 500

        /**
         * 발송 요청을 접수한다.
         *
         * [rendered]가 이미 제목·본문·딥링크·푸시 data를 조립해 두었으므로 여기서는 옮겨 담기만 한다.
         */
        fun request(
            dedupeKey: String,
            targetIdentifier: String,
            method: NotificationMethod,
            senderEmail: String,
            rendered: RenderedNotification,
            bodyOverride: String? = null,
        ): Notification {
            val body = bodyOverride ?: rendered.body
            requireNotBlank(
                "dedupeKey" to dedupeKey,
                "targetIdentifier" to targetIdentifier,
                "senderEmail" to senderEmail,
                "title" to rendered.title,
                "body" to body,
            )
            return Notification(
                dedupeKey = dedupeKey,
                targetIdentifier = targetIdentifier,
                method = method,
                senderEmail = senderEmail,
                senderNickname = rendered.senderNickname,
                type = rendered.type,
                title = rendered.title,
                body = body,
                path = rendered.path,
                extraData = rendered.data,
            )
        }

        /**
         * 발행된 이벤트 하나가 만들어 낼 멱등 키.
         *
         * 같은 이벤트라도 발송 수단이 다르면 별개의 발송이므로 [method]까지 키에 넣는다.
         */
        fun dedupeKeyOf(eventId: UUID, method: NotificationMethod): String = "$eventId:$method"

        /** 영속성 계층에서 복원할 때 쓴다. 이미 한 번 검증을 통과한 값이므로 다시 확인하지 않는다. */
        fun reconstruct(
            id: Long?,
            dedupeKey: String,
            targetIdentifier: String,
            method: NotificationMethod,
            senderEmail: String,
            senderNickname: String,
            type: NotificationType,
            title: String,
            body: String,
            path: String?,
            extraData: Map<String, String>,
            status: NotificationStatus,
            attempts: Int,
            lastError: String?,
            sentAt: LocalDateTime?,
            isRead: Boolean,
            createdAt: LocalDateTime?,
        ): Notification = Notification(
            id, dedupeKey, targetIdentifier, method, senderEmail, senderNickname, type,
            title, body, path, extraData, status, attempts, lastError, sentAt, isRead, createdAt,
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

    /** 수신함에 보일 알림인가. SMS는 발송 이력으로만 남고 수신함 조회 대상이 아니다. */
    val isInbox: Boolean
        get() = method == NotificationMethod.FCM && status == NotificationStatus.SENT

    /** 지금 발송을 시도할 수 있는가. */
    val isDeliverable: Boolean
        get() = status.isDeliverable

    fun toRendered(): RenderedNotification =
        RenderedNotification(
            type = type,
            senderNickname = senderNickname,
            title = title,
            body = body,
            path = path.orEmpty(),
            data = extraData,
        )

    /**
     * 발송 성공을 기록한다. 이미 성공한 알림이면 그대로 돌려준다(idempotent).
     *
     * 재시도를 소진한 알림([NotificationStatus.DEAD])은 [retry]로 되살린 뒤에만 발송할 수 있다.
     */
    fun markSent(now: LocalDateTime): Notification {
        if (status == NotificationStatus.SENT) return this
        requireDeliverable()
        return replaced(status = NotificationStatus.SENT, sentAt = now, lastError = null)
    }

    /**
     * 발송 실패를 기록한다. 시도 횟수가 [MAX_ATTEMPTS]에 닿으면 [NotificationStatus.DEAD]가 된다.
     */
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

    /**
     * 운영자가 재발송을 건다. 시도 횟수를 되돌려 다시 [MAX_ATTEMPTS]만큼 기회를 준다.
     *
     * 재시도를 소진한 알림에만 걸 수 있다.
     */
    fun retry(): Notification {
        if (status != NotificationStatus.DEAD) NotificationException.NOTIFICATION_NOT_RETRYABLE.throwIt()
        return replaced(status = NotificationStatus.PENDING, attempts = 0, lastError = null)
    }

    /** 읽음 처리한다. 이미 읽었으면 그대로 돌려준다(idempotent). 발송되지 않은 알림은 읽을 수 없다. */
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
        dedupeKey = dedupeKey,
        targetIdentifier = targetIdentifier,
        method = method,
        senderEmail = senderEmail,
        senderNickname = senderNickname,
        type = type,
        title = title,
        body = body,
        path = path,
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
        return dedupeKey == other.dedupeKey
    }

    override fun hashCode(): Int = id?.hashCode() ?: dedupeKey.hashCode()
}
