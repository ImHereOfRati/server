package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.exception.UserException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

class UserTest {

    private fun createUserWithSpecificUserStatus(status: UserStatus): User =
        User(
            id = UUID.randomUUID(),
            email = "test@test.com",
            nickname = "test",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = status
        )

    @Test
    @DisplayName("PENDING 상태의 유저를 활성화하면 ACTIVE 상태의 유저가 반환된다")
    fun activate_success() {
        val user = createUserWithSpecificUserStatus(UserStatus.PENDING)

        val activatedUser = user.activate()

        assertThat(activatedUser.status).isEqualTo(UserStatus.ACTIVE)
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus::class,
        names = ["PENDING"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("PENDING 상태가 아닌 유저를 활성화하면 예외가 발생한다")
    fun activate_fail_invalid_status(status: UserStatus) {
        val user = createUserWithSpecificUserStatus(status)

        assertThatThrownBy { user.activate() }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.ONLY_PENDING_CAN_BE_ACTIVE_USER)
    }

    @Test
    @DisplayName("ACTIVE 상태의 사용자를 차단하면 BLOCKED 상태가 된다")
    fun block_success() {
        val user = createUserWithSpecificUserStatus(UserStatus.ACTIVE)

        val blockedUser = user.block()

        assertThat(blockedUser.status).isEqualTo(UserStatus.BLOCKED)
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus::class,
        names = ["ACTIVE", "WITHDRAWN"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("ACTIVE가 아닌 사용자는 차단할 수 없다")
    fun block_fail_invalid_status(status: UserStatus) {
        val user = createUserWithSpecificUserStatus(status)

        assertThatThrownBy { user.block() }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.ONLY_ACTIVE_USER_CAN_BE_BLOCKED)
    }

    @Test
    @DisplayName("탈퇴한 사용자는 차단할 수 없다")
    fun block_fail_withdrawn_status() {
        val user = createUserWithSpecificUserStatus(UserStatus.WITHDRAWN)

        assertThatThrownBy { user.block() }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.WITHDRAW_USER)
    }

    @Test
    @DisplayName("BLOCKED 상태의 사용자를 차단 해제하면 ACTIVE 상태가 된다")
    fun unblock_success() {
        val user = createUserWithSpecificUserStatus(UserStatus.BLOCKED)

        val unblockedUser = user.unblock()

        assertThat(unblockedUser.status).isEqualTo(UserStatus.ACTIVE)
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus::class,
        names = ["BLOCKED", "WITHDRAWN"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("BLOCKED가 아닌 사용자는 차단 해제할 수 없다")
    fun unblock_fail_invalid_status(status: UserStatus) {
        val user = createUserWithSpecificUserStatus(status)

        assertThatThrownBy { user.unblock() }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.ONLY_BLOCKED_USER_CAN_BE_UNBLOCKED)
    }

    @Test
    @DisplayName("탈퇴한 사용자는 차단 해제할 수 없다")
    fun unblock_fail_withdrawn_status() {
        val user = createUserWithSpecificUserStatus(UserStatus.WITHDRAWN)

        assertThatThrownBy { user.unblock() }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.WITHDRAW_USER)
    }


    @ParameterizedTest
    @EnumSource(value = UserStatus::class, names = ["PENDING", "ACTIVE", "BLOCKED"])
    @DisplayName("PENDING, ACTIVE 또는 BLOCKED 상태의 사용자는 탈퇴할 수 있다")
    fun withdraw_success(status: UserStatus) {
        val user = createUserWithSpecificUserStatus(status)

        val withdrawnUser = user.withdraw()

        assertThat(withdrawnUser.status).isEqualTo(UserStatus.WITHDRAWN)
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자는 다시 탈퇴할 수 없다")
    fun withdraw_fail_withdrawn_status() {
        val user = createUserWithSpecificUserStatus(UserStatus.WITHDRAWN)

        assertThatThrownBy { user.withdraw() }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.WITHDRAW_USER)
    }

    @Test
    @DisplayName("신규 사용자는 기본 역할과 PENDING 상태로 생성된다")
    fun new_user_has_expected_defaults() {
        val user = User(
            email = "new@test.com",
            nickname = "newbie",
            oauthProvider = OAuth2Provider.GOOGLE,
            oidcSubject = "oidc-subject",
        )

        assertThat(user.id).isNull()
        assertThat(user.role).isEqualTo(UserRole.NORMAL)
        assertThat(user.status).isEqualTo(UserStatus.PENDING)
        assertThat(user.oidcSubject).isEqualTo("oidc-subject")
        assertThat(user.refreshTokenVersion).isZero()
    }

    @Test
    @DisplayName("닉네임을 변경하면 원본을 유지한 새 사용자 객체가 반환된다")
    fun updateNickname_returns_copy_without_mutating_original_user() {
        val user = createUserWithSpecificUserStatus(UserStatus.ACTIVE)

        val updated = user.updateNickname("changed")

        assertThat(updated.nickname).isEqualTo("changed")
        assertThat(user.nickname).isEqualTo("test")
        assertThat(updated).isNotSameAs(user)
    }

    @Test
    @DisplayName("Refresh Token 버전을 증가시키면 다른 사용자 정보는 유지된다")
    fun rotateRefreshTokenVersion_increments_only_token_version() {
        val user = createUserWithSpecificUserStatus(UserStatus.ACTIVE)

        val rotated = user.rotateRefreshTokenVersion()

        assertThat(rotated.refreshTokenVersion).isEqualTo(1)
        assertThat(rotated.copy(refreshTokenVersion = user.refreshTokenVersion)).isEqualTo(user)
    }
}
