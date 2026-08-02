package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
class SpringDataFcmTokenRepositoryTest {

    @Autowired
    private lateinit var repository: SpringDataFcmTokenRepository

    @Test
    @DisplayName("소유자 식별자로 FcmToken을 정상적으로 조회한다")
    fun findByOwnerId_success() {
        // given
        val ownerId = UUID.randomUUID()
        val entity = FcmTokenJpaEntity(
            token = "test-token",
            ownerId = ownerId,
            deviceType = DeviceType.IOS
        )
        repository.save(entity)

        // when
        val found = repository.findByOwnerId(ownerId)

        // then
        assertThat(found).isNotNull
        assertThat(found?.ownerId).isEqualTo(ownerId)
        assertThat(found?.token).isEqualTo("test-token")
        assertThat(found?.deviceType).isEqualTo(DeviceType.IOS)
    }

    @Test
    @DisplayName("존재하지 않는 소유자 식별자로 조회하면 null을 반환한다")
    fun findByOwnerId_returnsNull() {
        // when
        val found = repository.findByOwnerId(UUID.randomUUID())

        // then
        assertThat(found).isNull()
    }
}
