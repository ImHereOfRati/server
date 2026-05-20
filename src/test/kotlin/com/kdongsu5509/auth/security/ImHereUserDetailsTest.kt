package com.kdongsu5509.auth.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority

class ImHereUserDetailsTest {

    companion object {
        const val TEST_EMAIL = "test@kakao.com"
        const val ROLE_NORMAL = "NORMAL"
        const val EXPECTED_ROLE_NORMAL = "ROLE_NORMAL"
        const val TEST_NICKNAME = "테스티"
        const val ACTIVE_STATUS = "ACTIVE"
        const val PENDING_STATUS = "PENDING"
        const val BLOCKED_STATUS = "BLOCKED"
        const val DEACTIVATED_STATUS = "DEACTIVATED"
    }

    @Test
    @DisplayName("이메일과 권한으로 UserDetails를 생성한다")
    fun createUserDetails_success() {
        // when
        val userDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)

        // then
        assertThat(userDetails.username).isEqualTo(TEST_EMAIL)
        assertThat(userDetails.password).isNull()
        assertThat(userDetails.authorities).hasSize(1)
        assertThat(userDetails.authorities.first()).isEqualTo(SimpleGrantedAuthority(EXPECTED_ROLE_NORMAL))
    }

    @Nested
    inner class IsAccountNonLockedTest {
        @Test
        @DisplayName("계정 상태가 BANNED가 아니면 잠기지 않았다고 반환한다")
        fun isAccountNonLocked_returnsTrue() {
            // given
            val activeDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)
            val pendingDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, PENDING_STATUS)

            // when & then
            assertThat(activeDetails.isAccountNonLocked).isTrue()
            assertThat(pendingDetails.isAccountNonLocked).isTrue()
        }

        @Test
        @DisplayName("계정 상태가 BLOCKED이면 잠겼다는 false를 반환한다")
        fun isAccountNonLocked_returnsFalse() {
            // given
            val userDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, BLOCKED_STATUS)

            // when
            val result = userDetails.isAccountNonLocked

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    inner class IsEnabledTest {
        @Test
        @DisplayName("ACTIVE이면 활성화되어 있다고 반환한다")
        fun isEnabled_returnsTrue() {
            // given
            val activeDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)

            // when & then
            assertThat(activeDetails.isEnabled).isTrue()
        }

        @Test
        @DisplayName("pending이면 활성화되어 있다고 반환한다")
        fun isEnabled_returnTure_when_Pending() {
            // given
            val pendingDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, PENDING_STATUS)

            // when && then
            assertThat(pendingDetails.isEnabled).isTrue()
        }

        @Test
        @DisplayName("DEACTIVATED이면 비활성화되어 있다고 반환한다")
        fun isEnabled_returnsFalse() {
            // given
            val deactivatedDetails = ImHereUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, DEACTIVATED_STATUS)

            // when & then
            assertThat(deactivatedDetails.isEnabled).isFalse()
        }
    }
}
