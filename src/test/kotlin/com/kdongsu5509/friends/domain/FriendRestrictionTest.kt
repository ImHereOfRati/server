package com.kdongsu5509.friends.domain

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class FriendRestrictionTest {

    private fun user(nickname: String): User = User(
        id = UUID.randomUUID(),
        email = "$nickname@test.com",
        nickname = nickname,
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Test
    @DisplayName("reject()로 생성하면 만료일이 30일 뒤로 자동 설정된다")
    fun reject_setsExpiredAtTo30Days() {
        val restrictor = user("restrictor")
        val restricted = user("restricted")

        val restriction = FriendRestriction.reject(restrictor, restricted)

        val expected = LocalDateTime.now().plusDays(30)
        assertThat(restriction.type).isEqualTo(FriendRestrictionType.REJECT)
        assertThat(restriction.expiredAt).isCloseTo(expected, within(1, ChronoUnit.MINUTES))
    }

    @Test
    @DisplayName("block()로 생성하면 만료일은 null이다")
    fun block_expiredAtIsNull() {
        val restrictor = user("restrictor")
        val restricted = user("restricted")

        val restriction = FriendRestriction.block(restrictor, restricted)

        assertThat(restriction.type).isEqualTo(FriendRestrictionType.BLOCK)
        assertThat(restriction.expiredAt).isNull()
    }

    @Test
    @DisplayName("isExpired(now)는 만료일이 now 이전이거나 같으면 true, null이면 항상 false")
    fun isExpired_boundary() {
        val restrictor = user("restrictor")
        val restricted = user("restricted")
        val now = LocalDateTime.now()

        val expiredExactlyNow = FriendRestriction.reject(restrictor, restricted).copy(expiredAt = now)
        val notYetExpired = FriendRestriction.reject(restrictor, restricted).copy(expiredAt = now.plusSeconds(1))
        val neverExpires = FriendRestriction.block(restrictor, restricted)

        assertThat(expiredExactlyNow.isExpired(now)).isTrue()
        assertThat(notYetExpired.isExpired(now)).isFalse()
        assertThat(neverExpires.isExpired(now)).isFalse()
    }

    @Test
    @DisplayName("isOwnedBy는 restrictor의 이메일과 일치할 때만 true다")
    fun isOwnedBy() {
        val restrictor = user("restrictor")
        val restricted = user("restricted")
        val restriction = FriendRestriction.block(restrictor, restricted)

        assertThat(restriction.isOwnedBy(restrictor.email)).isTrue()
        assertThat(restriction.isOwnedBy(restricted.email)).isFalse()
    }
}
