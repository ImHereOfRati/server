package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FcmTokenMapperTest {

    private val fcmTokenMapper = FcmTokenMapper()

    @Test
    @DisplayName("JpaEntity를 DomainEntity로 올바르게 매핑")
    fun mapToDomainEntity_success() {
        // given
        val updatedAt = LocalDateTime.now()
        val jpaEntity = FcmTokenJpaEntity(
            token = "sample-token",
            userEmail = "user@example.com",
            deviceType = DeviceType.AOS
        ).apply {
            this.id = 1L
            this.updatedAt = updatedAt
        }

        // when
        val domainEntity = fcmTokenMapper.mapToDomainEntity(jpaEntity)

        // then
        assertThat(domainEntity.id).isEqualTo(jpaEntity.id)
        assertThat(domainEntity.fcmToken).isEqualTo(jpaEntity.token)
        assertThat(domainEntity.userEmail).isEqualTo(jpaEntity.userEmail)
        assertThat(domainEntity.deviceType).isEqualTo(jpaEntity.deviceType)
        assertThat(domainEntity.updatedAt).isEqualTo(jpaEntity.updatedAt)
    }
}