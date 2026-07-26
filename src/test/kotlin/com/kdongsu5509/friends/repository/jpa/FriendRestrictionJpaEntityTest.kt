package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class FriendRestrictionJpaEntityTest {

    @Test
    @DisplayName("expiredAt을 넘겨서 생성하면 계산 없이 그 값을 그대로 저장한다")
    fun create_storesGivenExpiredAtAsIs() {
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

        val givenExpiredAt = LocalDateTime.now().plusDays(30)

        // when
        val restriction = FriendRestrictionJpaEntity.create(actor, target, FriendRestrictionType.REJECT, givenExpiredAt)

        // then
        assertThat(restriction.type).isEqualTo(FriendRestrictionType.REJECT)
        assertThat(restriction.expiredAt).isEqualTo(givenExpiredAt)
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
