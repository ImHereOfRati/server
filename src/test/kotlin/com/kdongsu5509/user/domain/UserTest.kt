package com.kdongsu5509.user.domain

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.exception.UserException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class UserTest {

    private fun createUser(status: UserStatus): User {
        return User(
            id = UUID.randomUUID(),
            email = "test@test.com",
            nickname = "test",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = status
        )
    }

    @Test
    @DisplayName("PENDING 상태의 유저를 활성화하면 ACTIVE 상태의 유저가 반환된다")
    fun activate_success() {
        val user = createUser(UserStatus.PENDING)

        val activatedUser = user.activate()

        assertThat(activatedUser.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 유저를 활성화하면 예외가 발생한다")
    fun activate_fail_invalid_status() {
        val user = createUser(UserStatus.ACTIVE)

        assertThatThrownBy {
            user.activate()
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.INVALID_USER_STATUS)
    }

    @Test
    @DisplayName("BLOCKED 상태가 아닌 유저를 차단하면 BLOCKED 상태의 유저가 반환된다")
    fun block_success() {
        val user = createUser(UserStatus.ACTIVE)

        val blockedUser = user.block()

        assertThat(blockedUser.status).isEqualTo(UserStatus.BLOCKED)
    }

    @Test
    @DisplayName("이미 BLOCKED 상태인 유저를 차단하면 예외가 발생한다")
    fun block_fail_invalid_status() {
        val user = createUser(UserStatus.BLOCKED)

        assertThatThrownBy {
            user.block()
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.INVALID_USER_STATUS)
    }

    @Test
    @DisplayName("BLOCKED 상태의 유저를 차단 해제하면 ACTIVE 상태의 유저가 반환된다")
    fun unblock_success() {
        val user = createUser(UserStatus.BLOCKED)

        val unblockedUser = user.unblock()

        assertThat(unblockedUser.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("BLOCKED 상태가 아닌 유저를 차단 해제하면 예외가 발생한다")
    fun unblock_fail_invalid_status() {
        val user = createUser(UserStatus.ACTIVE)

        assertThatThrownBy {
            user.unblock()
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.INVALID_USER_STATUS)
    }
}
