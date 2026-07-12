package com.kdongsu5509.friends.domain

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class FriendshipTest {

    private fun user(nickname: String, id: UUID = UUID.randomUUID()): User = User(
        id = id,
        email = "$nickname@test.com",
        nickname = nickname,
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Test
    @DisplayName("owner와 friend가 같은 사용자면 생성 시 예외를 발생시킨다")
    fun selfFriendship_throws() {
        val same = user("me")

        assertThrows<ImHereBaseException> {
            Friendship(owner = same, friend = same, friendAlias = "alias")
        }
    }

    @Test
    @DisplayName("alias가 20자를 넘으면 생성 시 예외를 발생시킨다")
    fun aliasTooLong_throws() {
        val owner = user("owner")
        val friend = user("friend")

        assertThrows<IllegalArgumentException> {
            Friendship(owner = owner, friend = friend, friendAlias = "a".repeat(21))
        }
    }

    @Test
    @DisplayName("isOwnedBy는 owner의 이메일과 일치할 때만 true다")
    fun isOwnedBy() {
        val owner = user("owner")
        val friend = user("friend")
        val friendship = Friendship(owner = owner, friend = friend, friendAlias = "alias")

        assertThat(friendship.isOwnedBy(owner.email)).isTrue()
        assertThat(friendship.isOwnedBy(friend.email)).isFalse()
    }

    @Test
    @DisplayName("ownerId/friendId는 각 User의 id를 반환한다")
    fun ownerIdAndFriendId() {
        val ownerId = UUID.randomUUID()
        val friendId = UUID.randomUUID()
        val owner = user("owner", id = ownerId)
        val friend = user("friend", id = friendId)
        val friendship = Friendship(owner = owner, friend = friend, friendAlias = "alias")

        assertThat(friendship.ownerId()).isEqualTo(ownerId)
        assertThat(friendship.friendId()).isEqualTo(friendId)
    }
}
