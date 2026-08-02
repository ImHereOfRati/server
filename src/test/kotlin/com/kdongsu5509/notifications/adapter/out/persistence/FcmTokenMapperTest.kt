package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

class FcmTokenMapperTest {

    private val mapper = FcmTokenMapper()

    @Test
    @DisplayName("JpaEntity를 Domain 객체로 정상적으로 변환한다")
    fun toDomain() {
        // given
        val ownerId = UUID.randomUUID()
        val jpaEntity = FcmTokenJpaEntity(
            token = "test-token-value",
            ownerId = ownerId,
            deviceType = DeviceType.IOS
        ).apply {
            id = 100L
            ReflectionTestUtils.setField(this, "updatedAt", LocalDateTime.of(2026, 1, 1, 10, 0))
        }

        // when
        val domain = mapper.toDomain(jpaEntity)

        // then
        assertThat(domain.id).isEqualTo(100L)
        assertThat(domain.ownerId).isEqualTo(ownerId)
        assertThat(domain.fcmToken).isEqualTo("test-token-value")
        assertThat(domain.deviceType).isEqualTo(DeviceType.IOS)
        assertThat(domain.updatedAt).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0))
    }

    @Test
    @DisplayName("Domain 객체를 JpaEntity로 정상적으로 변환한다")
    fun toEntity() {
        // given
        val ownerId = UUID.randomUUID()
        val domain = FcmToken(
            id = 200L,
            ownerId = ownerId,
            fcmToken = "domain-token-value",
            deviceType = DeviceType.AOS,
            createdAt = LocalDateTime.of(2026, 2, 1, 10, 0),
            updatedAt = LocalDateTime.of(2026, 2, 1, 10, 0)
        )

        // when
        val entity = mapper.toEntity(domain)

        // then
        assertThat(entity.id).isEqualTo(200L)
        assertThat(entity.ownerId).isEqualTo(ownerId)
        assertThat(entity.token).isEqualTo("domain-token-value")
        assertThat(entity.deviceType).isEqualTo(DeviceType.AOS)
    }
}
