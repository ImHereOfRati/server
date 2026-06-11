package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class NewFriendRequestJpaEntityTest {

    @Test
    @DisplayName("FriendRequestJpaEntity가 올바르게 인스턴스화된다")
    fun instantiation_success() {
        // given
        val requester = UserJpaEntity(
            email = "requester@test.com",
            nickname = "requester",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        requester.id = UUID.randomUUID()

        val receiver = UserJpaEntity(
            email = "receiver@test.com",
            nickname = "receiver",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        receiver.id = UUID.randomUUID()

        // when
        val request = FriendRequestJpaEntity(
            requester = requester,
            receiver = receiver,
            message = "Hello"
        )

        // then
        assertThat(request.requester).isEqualTo(requester)
        assertThat(request.receiver).isEqualTo(receiver)
        assertThat(request.message).isEqualTo("Hello")
    }
}
