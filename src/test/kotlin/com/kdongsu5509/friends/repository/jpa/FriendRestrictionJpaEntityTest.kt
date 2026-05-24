package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class FriendRestrictionJpaEntityTest {

    @Test
    @DisplayName("거절(REJECT) 타입의 제한을 생성하면 만료일이 30일 뒤로 자동 설정된다")
    fun createFromRejection_setsExpiredAtTo30Days() {
        // given
        val actor = UserJpaEntity(
            email = "actor@test.com",
            nickname = "actor",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        actor.id = UUID.randomUUID()

        val target = UserJpaEntity(
            email = "target@test.com",
            nickname = "target",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        target.id = UUID.randomUUID()

        // when
        val restriction = FriendRestrictionJpaEntity.createRejectionType(actor, target)

        // then
        assertThat(restriction.type).isEqualTo(FriendRestrictionType.REJECT)
        assertThat(restriction.expiredAt).isNotNull()

        val expectedExpiration = LocalDateTime.now().plusDays(30)
        assertThat(restriction.expiredAt).isCloseTo(expectedExpiration, within(1, ChronoUnit.MINUTES))
    }

    @Test
    @DisplayName("차단(BLOCK) 타입의 제한을 생성하면 만료일은 null로 설정된다")
    fun createBlock_expiredAtIsNull() {
        // given
        val actor = UserJpaEntity(
            email = "actor@test.com",
            nickname = "actor",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        actor.id = UUID.randomUUID()

        val target = UserJpaEntity(
            email = "target@test.com",
            nickname = "target",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        target.id = UUID.randomUUID()

        // when
        val restriction = FriendRestrictionJpaEntity.create(actor, target, FriendRestrictionType.BLOCK)

        // then
        assertThat(restriction.type).isEqualTo(FriendRestrictionType.BLOCK)
        assertThat(restriction.expiredAt).isNull()
    }
}
