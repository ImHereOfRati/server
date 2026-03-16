package com.kdongsu5509.imhere.notification.adapter.out.persistence

import com.kdongsu5509.imhere.auth.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhere.auth.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhere.auth.domain.OAuth2Provider
import com.kdongsu5509.imhere.auth.domain.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class SpringDataFcmTokenRepositoryTest {

    @Autowired
    private lateinit var fcmTokenRepository: SpringDataFcmTokenRepository

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Test
    @DisplayName("이메일로 FCM 토큰 엔티티를 정상적으로 조회한다")
    fun findByUserEmail_success() {
        // given
        val email = "dongsu@test.com"

        val user = UserJpaEntity(email, UserRole.NORMAL, OAuth2Provider.KAKAO)
        userRepository.save(user)

        val fcmToken = FcmTokenEntity(token = "testToken", user = user)
        fcmTokenRepository.save(fcmToken)

        // when
        val result = fcmTokenRepository.findByUserEmail(email)

        // then
        assertThat(result).isNotNull
        assertThat(result?.token).isEqualTo("testToken")
        assertThat(result?.user?.email).isEqualTo(email)
    }
}