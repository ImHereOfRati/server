package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NotificationHistoryMapperTest {

    private val mapper = NotificationHistoryMapper()

    @Test
    @DisplayName("Domain을 Entity로 변환한다")
    fun toEntity() {
        // given
        val domain = NotificationHistory(
            id = 1L,
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = "path",
            isRead = false,
            createdAt = LocalDateTime.now()
        )

        // when
        val entity = mapper.toEntity(domain)

        // then
        assertThat(entity.id).isEqualTo(domain.id)
        assertThat(entity.receiverEmail).isEqualTo(domain.receiverEmail)
        assertThat(entity.senderNickname).isEqualTo(domain.senderNickname)
        assertThat(entity.title).isEqualTo(domain.title)
        assertThat(entity.body).isEqualTo(domain.body)
        assertThat(entity.type).isEqualTo(domain.type)
        assertThat(entity.path).isEqualTo(domain.path)
        assertThat(entity.isRead).isEqualTo(domain.isRead)
    }

    @Test
    @DisplayName("Entity를 Domain으로 변환한다")
    fun toDomain() {
        // given
        val entity = NotificationHistoryJpaEntity(
            id = 1L,
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = "path",
            isRead = false
        ).apply {
            createdAt = LocalDateTime.now()
        }

        // when
        val domain = mapper.toDomain(entity)

        // then
        assertThat(domain.id).isEqualTo(entity.id)
        assertThat(domain.receiverEmail).isEqualTo(entity.receiverEmail)
        assertThat(domain.senderNickname).isEqualTo(entity.senderNickname)
        assertThat(domain.title).isEqualTo(entity.title)
        assertThat(domain.body).isEqualTo(entity.body)
        assertThat(domain.type).isEqualTo(entity.type)
        assertThat(domain.path).isEqualTo(entity.path)
        assertThat(domain.isRead).isEqualTo(entity.isRead)
        assertThat(domain.createdAt).isEqualTo(entity.createdAt)
    }
}
