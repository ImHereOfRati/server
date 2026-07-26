package com.kdongsu5509.auth.domain

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginEligibilityPolicyTest {

    @Test
    @DisplayName("BLOCKED 상태면 USER_DISABLED 예외를 던진다")
    fun blocked_throws_disabled() {
        val exception = assertThrows<ImHereBaseException> {
            LoginEligibilityPolicy.assertLoginable(UserStatus.BLOCKED)
        }
        assertThat(exception.errorCode).isEqualTo(AuthException.USER_DISABLED)
    }

    @Test
    @DisplayName("WITHDRAWN 상태면 USER_WITHDRAWN 예외를 던진다")
    fun withdrawn_throws_withdrawn() {
        val exception = assertThrows<ImHereBaseException> {
            LoginEligibilityPolicy.assertLoginable(UserStatus.WITHDRAWN)
        }
        assertThat(exception.errorCode).isEqualTo(AuthException.USER_WITHDRAWN)
    }

    @Test
    @DisplayName("PENDING/ACTIVE 상태면 통과한다")
    fun pending_active_pass() {
        assertThatCode { LoginEligibilityPolicy.assertLoginable(UserStatus.PENDING) }
            .doesNotThrowAnyException()
        assertThatCode { LoginEligibilityPolicy.assertLoginable(UserStatus.ACTIVE) }
            .doesNotThrowAnyException()
    }
}
