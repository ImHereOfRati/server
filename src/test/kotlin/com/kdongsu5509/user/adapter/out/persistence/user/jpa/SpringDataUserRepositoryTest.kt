package com.kdongsu5509.user.adapter.out.persistence.user.jpa

import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class SpringDataUserRepositoryTest @Autowired constructor(
    private val userRepository: SpringDataUserRepository
) {

    @Test
    @DisplayName("이메일로 사용자를 정확히 조회한다")
    fun findByEmail_success() {
        // given
        val email = "test@example.com"
        val user = UserJpaEntity(
            email = email,
            nickname = "테스터",
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        userRepository.save(user)

        // when
        val result = userRepository.findByEmail(email)

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo(email)
        assertThat(result?.nickname).isEqualTo("테스터")
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 null을 반환한다")
    fun findByEmail_fail() {
        // when
        val result = userRepository.findByEmail("non-existent@example.com")

        // then
        assertThat(result).isNull()
    }
}
