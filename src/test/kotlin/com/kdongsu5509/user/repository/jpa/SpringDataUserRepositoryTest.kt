package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class SpringDataUserRepositoryTest @Autowired constructor(
    private val userRepository: SpringDataUserRepository,
    private val entityManager: EntityManager,
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

    @Test
    @DisplayName("탈퇴 사용자를 제외한 두 번째 Slice를 offset과 hasNext에 맞게 조회한다")
    fun findAllByStatusNot_applies_status_filter_and_page_offset() {
        userRepository.saveAllAndFlush(
            listOf(
                user("a@example.com", UserStatus.ACTIVE),
                user("b@example.com", UserStatus.PENDING),
                user("c@example.com", UserStatus.BLOCKED),
                user("d@example.com", UserStatus.WITHDRAWN),
            )
        )
        entityManager.clear()
        val pageable = PageRequest.of(1, 2, Sort.by("email").ascending())

        val result = userRepository.findAllByStatusNot(UserStatus.WITHDRAWN, pageable)

        assertThat(result.content.map(UserJpaEntity::email))
            .containsExactly("c@example.com")
        assertThat(result.hasNext()).isFalse()
        assertThat(result.number).isEqualTo(1)
    }

    @Test
    @DisplayName("사용자의 역할과 OAuth 제공자, 상태, OIDC subject, Refresh Token 버전을 보존한다")
    fun save_preserves_all_user_persistence_fields() {
        val saved = userRepository.saveAndFlush(
            UserJpaEntity(
                email = "all-fields@example.com",
                nickname = "전체필드",
                role = UserRole.ADMIN,
                provider = OAuth2Provider.GOOGLE,
                status = UserStatus.BLOCKED,
                oidcSubject = "google-subject",
                refreshTokenVersion = 7,
            )
        )
        val savedId = requireNotNull(saved.id)
        entityManager.clear()

        val result = userRepository.findById(savedId).orElseThrow()

        assertThat(result.email).isEqualTo("all-fields@example.com")
        assertThat(result.nickname).isEqualTo("전체필드")
        assertThat(result.role).isEqualTo(UserRole.ADMIN)
        assertThat(result.provider).isEqualTo(OAuth2Provider.GOOGLE)
        assertThat(result.status).isEqualTo(UserStatus.BLOCKED)
        assertThat(result.oidcSubject).isEqualTo("google-subject")
        assertThat(result.refreshTokenVersion).isEqualTo(7)
    }

    private fun user(email: String, status: UserStatus) = UserJpaEntity(
        email = email,
        nickname = email.substringBefore("@"),
        provider = OAuth2Provider.KAKAO,
        status = status,
    )
}
