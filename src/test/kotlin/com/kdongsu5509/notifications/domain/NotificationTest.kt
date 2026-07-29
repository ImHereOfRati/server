package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.ImHereBaseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class NotificationTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 29, 15, 0, 0)

    private fun rendered(): RenderedNotification =
        NotificationType.FRIEND_REQUEST_RECEIVED.render(
            senderNickname = "홍길동",
            senderEmail = "sender@imhere.com",
        )

    private fun requested(
        method: NotificationMethod = NotificationMethod.FCM,
        dedupeKey: String = "event-1:FCM",
    ): Notification = Notification.request(
        dedupeKey = dedupeKey,
        targetIdentifier = "receiver@imhere.com",
        method = method,
        senderEmail = "sender@imhere.com",
        rendered = rendered(),
    )

    /** MAX_ATTEMPTS 회 실패시켜 DEAD로 만든다. */
    private fun dead(): Notification {
        var notification = requested()
        repeat(Notification.MAX_ATTEMPTS) { notification = notification.markFailed("일시적 오류") }
        return notification
    }

    @Nested
    @DisplayName("발송 요청 접수")
    inner class Request {

        @Test
        @DisplayName("접수한 알림은 PENDING 상태이고 렌더링 결과를 그대로 담는다")
        fun request_success() {
            // when
            val notification = requested()

            // then
            assertThat(notification.status).isEqualTo(NotificationStatus.PENDING)
            assertThat(notification.attempts).isZero()
            assertThat(notification.sentAt).isNull()
            assertThat(notification.lastError).isNull()
            assertThat(notification.isRead).isFalse()
            assertThat(notification.type).isEqualTo(NotificationType.FRIEND_REQUEST_RECEIVED)
            assertThat(notification.title).isEqualTo(rendered().title)
            assertThat(notification.body).isEqualTo(rendered().body)
            assertThat(notification.path).isEqualTo(rendered().path)
            assertThat(notification.extraData).isEqualTo(rendered().data)
        }

        @Test
        @DisplayName("접수한 알림은 발송을 시도할 수 있는 상태다")
        fun request_success_isDeliverable() {
            assertThat(requested().isDeliverable).isTrue()
        }

        @Test
        @DisplayName("필수 항목이 비어 있으면 접수를 거부한다")
        fun request_fail_when_required_field_blank() {
            assertThatThrownBy {
                Notification.request(
                    dedupeKey = " ",
                    targetIdentifier = "receiver@imhere.com",
                    method = NotificationMethod.FCM,
                    senderEmail = "sender@imhere.com",
                    rendered = rendered(),
                )
            }.isInstanceOf(ImHereBaseException::class.java)
        }

        @Test
        @DisplayName("멱등 키는 이벤트 하나와 발송 수단의 조합이다")
        fun dedupeKeyOf_success() {
            // given
            val eventId = UUID.fromString("3f68f41c-5351-43fe-b71b-2fafdc31e57a")

            // when & then - 같은 이벤트라도 수단이 다르면 별개의 발송이다
            assertThat(Notification.dedupeKeyOf(eventId, NotificationMethod.FCM))
                .isEqualTo("$eventId:FCM")
            assertThat(Notification.dedupeKeyOf(eventId, NotificationMethod.SMS))
                .isNotEqualTo(Notification.dedupeKeyOf(eventId, NotificationMethod.FCM))
        }
    }

    @Nested
    @DisplayName("발송 성공 기록")
    inner class MarkSent {

        @Test
        @DisplayName("발송에 성공하면 SENT가 되고 발송 시각이 남는다")
        fun markSent_success() {
            // when
            val sent = requested().markSent(now)

            // then
            assertThat(sent.status).isEqualTo(NotificationStatus.SENT)
            assertThat(sent.sentAt).isEqualTo(now)
            assertThat(sent.isDeliverable).isFalse()
        }

        @Test
        @DisplayName("이미 성공한 알림을 다시 기록해도 그대로다")
        fun markSent_success_idempotent() {
            // given
            val sent = requested().markSent(now)

            // when
            val again = sent.markSent(now.plusMinutes(5))

            // then
            assertThat(again).isSameAs(sent)
            assertThat(again.sentAt).isEqualTo(now)
        }

        @Test
        @DisplayName("재시도 도중 성공하면 직전 실패 사유를 지운다")
        fun markSent_success_clears_last_error() {
            // given
            val failed = requested().markFailed("일시적 오류")

            // when
            val sent = failed.markSent(now)

            // then
            assertThat(sent.status).isEqualTo(NotificationStatus.SENT)
            assertThat(sent.lastError).isNull()
        }

        @Test
        @DisplayName("재시도를 소진한 알림은 바로 성공 처리할 수 없다")
        fun markSent_fail_when_dead() {
            assertThatThrownBy { dead().markSent(now) }
                .isInstanceOf(ImHereBaseException::class.java)
        }
    }

    @Nested
    @DisplayName("발송 실패 기록")
    inner class MarkFailed {

        @Test
        @DisplayName("실패하면 시도 횟수가 늘고 사유가 남는다")
        fun markFailed_success() {
            // when
            val failed = requested().markFailed("FCM 일시적 오류")

            // then
            assertThat(failed.status).isEqualTo(NotificationStatus.FAILED)
            assertThat(failed.attempts).isEqualTo(1)
            assertThat(failed.lastError).isEqualTo("FCM 일시적 오류")
            assertThat(failed.isDeliverable).isTrue()
        }

        @Test
        @DisplayName("시도 횟수가 한계에 닿으면 DEAD가 된다")
        fun markFailed_success_becomes_dead_at_max_attempts() {
            // when
            val notification = dead()

            // then
            assertThat(notification.status).isEqualTo(NotificationStatus.DEAD)
            assertThat(notification.attempts).isEqualTo(Notification.MAX_ATTEMPTS)
            assertThat(notification.isDeliverable).isFalse()
        }

        @Test
        @DisplayName("긴 실패 사유는 잘라 담는다")
        fun markFailed_success_truncates_long_reason() {
            // given
            val longReason = "오".repeat(Notification.LAST_ERROR_MAX_LENGTH + 100)

            // when
            val failed = requested().markFailed(longReason)

            // then
            assertThat(failed.lastError).hasSize(Notification.LAST_ERROR_MAX_LENGTH)
        }

        @Test
        @DisplayName("이미 재시도를 소진한 알림에는 실패를 더 기록하지 않는다")
        fun markFailed_fail_when_dead() {
            assertThatThrownBy { dead().markFailed("또 실패") }
                .isInstanceOf(ImHereBaseException::class.java)
        }

        @Test
        @DisplayName("이미 성공한 알림에는 실패를 기록하지 않는다")
        fun markFailed_fail_when_already_sent() {
            assertThatThrownBy { requested().markSent(now).markFailed("실패") }
                .isInstanceOf(ImHereBaseException::class.java)
        }
    }

    @Nested
    @DisplayName("운영자 재발송")
    inner class Retry {

        @Test
        @DisplayName("재시도를 소진한 알림은 되살려 다시 기회를 준다")
        fun retry_success() {
            // when
            val revived = dead().retry()

            // then
            assertThat(revived.status).isEqualTo(NotificationStatus.PENDING)
            assertThat(revived.attempts).isZero()
            assertThat(revived.lastError).isNull()
            assertThat(revived.isDeliverable).isTrue()
        }

        @Test
        @DisplayName("아직 재시도가 남은 알림에는 재발송을 걸 수 없다")
        fun retry_fail_when_not_dead() {
            assertThatThrownBy { requested().markFailed("일시적 오류").retry() }
                .isInstanceOf(ImHereBaseException::class.java)
        }

        @Test
        @DisplayName("이미 성공한 알림에는 재발송을 걸 수 없다")
        fun retry_fail_when_sent() {
            assertThatThrownBy { requested().markSent(now).retry() }
                .isInstanceOf(ImHereBaseException::class.java)
        }
    }

    @Nested
    @DisplayName("읽음 처리")
    inner class MarkAsRead {

        @Test
        @DisplayName("발송된 알림은 읽음 처리한다")
        fun markAsRead_success() {
            // when
            val read = requested().markSent(now).markAsRead()

            // then
            assertThat(read.isRead).isTrue()
        }

        @Test
        @DisplayName("이미 읽은 알림을 다시 읽어도 그대로다")
        fun markAsRead_success_idempotent() {
            // given
            val read = requested().markSent(now).markAsRead()

            // when & then
            assertThat(read.markAsRead()).isSameAs(read)
        }

        @Test
        @DisplayName("아직 발송되지 않은 알림은 읽을 수 없다")
        fun markAsRead_fail_when_not_sent() {
            assertThatThrownBy { requested().markAsRead() }
                .isInstanceOf(ImHereBaseException::class.java)
        }
    }

    @Nested
    @DisplayName("수신함 노출 판정")
    inner class Inbox {

        @Test
        @DisplayName("발송에 성공한 FCM 알림만 수신함에 보인다")
        fun isInbox_success_when_sent_fcm() {
            assertThat(requested().markSent(now).isInbox).isTrue()
        }

        @Test
        @DisplayName("SMS는 이력으로만 남고 수신함에는 보이지 않는다")
        fun isInbox_false_when_sms() {
            // given
            val sms = requested(method = NotificationMethod.SMS).markSent(now)

            // then
            assertThat(sms.isInbox).isFalse()
        }

        @Test
        @DisplayName("아직 발송되지 않은 알림은 수신함에 보이지 않는다")
        fun isInbox_false_when_not_sent() {
            assertThat(requested().isInbox).isFalse()
            assertThat(requested().markFailed("실패").isInbox).isFalse()
            assertThat(dead().isInbox).isFalse()
        }
    }

    @Nested
    @DisplayName("동일성")
    inner class Equality {

        @Test
        @DisplayName("식별자가 없으면 멱등 키로 같은 알림인지 가린다")
        fun equals_success_by_dedupe_key() {
            assertThat(requested()).isEqualTo(requested())
            assertThat(requested()).isNotEqualTo(requested(dedupeKey = "event-2:FCM"))
        }

        @Test
        @DisplayName("상태를 바꿔도 같은 알림이다")
        fun equals_success_after_transition() {
            // given
            val notification = requested()

            // when & then
            assertThat(notification.markSent(now)).isEqualTo(notification)
        }
    }
}
