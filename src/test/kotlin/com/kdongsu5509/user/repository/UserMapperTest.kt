package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.user.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class UserMapperTest {
    private val userMapper = UserMapper()

    @Test
    @DisplayName("UserJpaEntity를 User 도메인 모델로 정상적으로 매핑한다")
    fun toDomain_success() {
        // given
        val id = UUID.randomUUID()
        val entity = UserJpaEntity(
            email = "test@test.com",
            nickname = "테스트",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ).apply {
            this.id = id
        }

        // when
        val domain = userMapper.toDomain(entity)

        // then
        val expected = User(
            id = id,
            email = "test@test.com",
            nickname = "테스트",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        assertThat(domain).isEqualTo(expected)
    }

    @Test
    @DisplayName("null UserJpaEntity를 받으면 null을 반환한다")
    fun toDomain_null() {
        // when
        val domain = userMapper.toDomain(null)

        // then
        assertThat(domain).isNull()
    }

    @Test
    @DisplayName("User 도메인 모델을 UserJpaEntity로 정상적으로 매핑한다")
    fun toEntity_success() {
        // given
        val domain = User(
            id = UUID.randomUUID(),
            email = "test@test.com",
            nickname = "테스트",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        // when
        val entity = userMapper.toEntity(domain)

        // then
        val expected = UserJpaEntity(
            email = "test@test.com",
            nickname = "테스트",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        assertThat(entity)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(expected)
    }
}
