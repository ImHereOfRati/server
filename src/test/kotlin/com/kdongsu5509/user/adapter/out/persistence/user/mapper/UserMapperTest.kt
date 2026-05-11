package com.kdongsu5509.user.adapter.out.persistence.user.mapper

import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

class UserMapperTest {

    private val userMapper = UserMapper()

    @ParameterizedTest
    @EnumSource(UserStatus::class)
    @DisplayName("도메인 모델을 JPA 엔티티로 성공적으로 변환한다")
    fun toJpaEntity_success(userStatus: UserStatus) {
        // given
        val domainUser = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            nickname = "테스터",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = userStatus
        )

        // when
        val jpaEntity = userMapper.toJpaEntity(domainUser)

        // then
        assertThat(jpaEntity.email).isEqualTo(domainUser.email)
        assertThat(jpaEntity.nickname).isEqualTo(domainUser.nickname)
        assertThat(jpaEntity.role).isEqualTo(domainUser.role)
        assertThat(jpaEntity.provider).isEqualTo(domainUser.oauthProvider)
        assertThat(jpaEntity.status).isEqualTo(domainUser.status)
    }

    @ParameterizedTest
    @EnumSource(UserStatus::class)
    @DisplayName("JPA 엔티티를 도메인 모델로 성공적으로 변환한다")
    fun toDomain_success(userStatus: UserStatus) {
        // given
        val jpaEntity = UserJpaEntity(
            "test@example.com",
            "테스터",
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            userStatus
        )

        // when
        val domainUser = userMapper.toDomain(jpaEntity)

        // then
        assertThat(domainUser.email).isEqualTo(jpaEntity.email)
        assertThat(domainUser.nickname).isEqualTo(jpaEntity.nickname)
        assertThat(domainUser.role).isEqualTo(jpaEntity.role)
        assertThat(domainUser.oauthProvider).isEqualTo(jpaEntity.provider)
        assertThat(domainUser.status).isEqualTo(jpaEntity.status)
    }
}
