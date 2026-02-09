package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

class UserMapperTest {

    private val userMapper = UserMapper()

    @ParameterizedTest
    @EnumSource(UserStatus::class)
    @DisplayName("Domain 객체를 JpaEntity로 올바르게 변환해야 한다")
    fun shouldMapDomainToJpaEntity(userStatus: UserStatus) {
        // given
        val domainUser = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            nickname = "동수",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = userStatus
        )

        // when
        val jpaEntity = userMapper.mapToJpaEntity(domainUser)

        // then
        assertThat(jpaEntity.email).isEqualTo(domainUser.email)
        assertThat(jpaEntity.nickname).isEqualTo(domainUser.nickname)
        assertThat(jpaEntity.role).isEqualTo(domainUser.role)
        assertThat(jpaEntity.provider).isEqualTo(domainUser.oauthProvider)
        assertThat(jpaEntity.status).isEqualTo(domainUser.status)
    }

    @ParameterizedTest
    @EnumSource(UserStatus::class)
    @DisplayName("기본적인 jpaEntity를 Domain 객체로 올바르게 변환해야 한다")
    fun shouldMapJpaEntityToDomain(userStatus: UserStatus) {
        // given
        val jpaEntity = UserJpaEntity(
            "test@example.com",
            "동수",
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            userStatus
        )

        // when
        val domainUser = userMapper.mapToDomainEntity(jpaEntity)

        // then
        assertThat(domainUser.email).isEqualTo(jpaEntity.email)
        assertThat(domainUser.nickname).isEqualTo(jpaEntity.nickname)
        assertThat(domainUser.role).isEqualTo(jpaEntity.role)
        assertThat(domainUser.oauthProvider).isEqualTo(jpaEntity.provider)
        assertThat(domainUser.status).isEqualTo(jpaEntity.status)
    }
}