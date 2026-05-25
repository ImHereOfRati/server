package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class SpringDataFcmTokenRepositoryTest {

    @Autowired
    private lateinit var repository: SpringDataFcmTokenRepository

    @Test
    @DisplayName("이메일로 FcmToken을 정상적으로 조회한다")
    fun findByEmail_success() {
        // given
        val email = "test@example.com"
        val entity = FcmTokenJpaEntity(
            token = "test-token",
            email = email,
            deviceType = DeviceType.IOS
        )
        repository.save(entity)

        // when
        val found = repository.findByEmail(email)

        // then
        assertThat(found).isNotNull
        assertThat(found?.email).isEqualTo(email)
        assertThat(found?.token).isEqualTo("test-token")
        assertThat(found?.deviceType).isEqualTo(DeviceType.IOS)
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 null을 반환한다")
    fun findByEmail_returnsNull() {
        // when
        val found = repository.findByEmail("non-existent@example.com")

        // then
        assertThat(found).isNull()
    }
}
