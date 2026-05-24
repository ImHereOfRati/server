package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class FriendshipJpaEntityTest {

    @Test
    @DisplayName("자기 자신을 친구로 등록하려고 하면 예외가 발생한다")
    fun selfFriendship_throwsException() {
        // given
        val user = UserJpaEntity(
            email = "user@test.com",
            nickname = "user",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        user.id = UUID.randomUUID()

        // when & then
        assertThatThrownBy {
            FriendshipJpaEntity(user, user, "alias")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("자기 자신을 친구로 등록할 수 없습니다.")
    }


}
