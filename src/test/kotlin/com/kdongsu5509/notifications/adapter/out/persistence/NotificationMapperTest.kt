package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NotificationMapperTest {

    private val mapper = NotificationMapper()
    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 29, 15, 0, 0)

    private fun requested(): Notification = Notification.request(
        dedupeKey = "event-1:FCM",
        targetIdentifier = "receiver@imhere.com",
        method = NotificationMethod.FCM,
        rendered = NotificationTemplate.render(
            type = NotificationType.ARRIVAL,
            senderAlias = "길동이",
            extraData = mapOf(NotificationTemplate.PLACE_NAME_KEY to "학교"),
        ),
    )

    @Test
    @DisplayName("도메인을 엔티티로 옮길 때 발송 상태와 추가 데이터가 보존된다")
    fun toEntity_success() {
        // given
        val domain = requested().markFailed("일시적 오류")

        // when
        val entity = mapper.toEntity(domain)

        // then
        assertThat(entity.dedupeKey).isEqualTo(domain.deduplicationKey)
        assertThat(entity.targetIdentifier).isEqualTo(domain.targetIdentifier)
        assertThat(entity.method).isEqualTo(NotificationMethod.FCM)
        assertThat(entity.type).isEqualTo(NotificationType.ARRIVAL.name)
        assertThat(entity.status).isEqualTo(NotificationStatus.FAILED)
        assertThat(entity.attempts).isEqualTo(1)
        assertThat(entity.lastError).isEqualTo("일시적 오류")
        assertThat(entity.extraData).isEqualTo(domain.extraData)
    }

    @Test
    @DisplayName("엔티티에서 도메인으로 복원해도 값이 그대로다")
    fun toDomain_success() {
        // given
        val original = requested().markSent(now)

        // when
        val restored = mapper.toDomain(mapper.toEntity(original))

        // then
        assertThat(restored.deduplicationKey).isEqualTo(original.deduplicationKey)
        assertThat(restored.type).isEqualTo(original.type)
        assertThat(restored.title).isEqualTo(original.title)
        assertThat(restored.body).isEqualTo(original.body)
        assertThat(restored.senderAlias).isEqualTo(original.senderAlias)
        assertThat(restored.extraData).isEqualTo(original.extraData)
        assertThat(restored.status).isEqualTo(NotificationStatus.SENT)
        assertThat(restored.sentAt).isEqualTo(now)
        assertThat(restored.isInbox).isTrue()
    }
}
