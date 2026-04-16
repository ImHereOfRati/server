package com.kdongsu5509.user.application.service.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority

class SimpleTokenUserDetailsTest {

    companion object {
        const val TEST_EMAIL = "test@kakao.com"
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_NORMAL = "NORMAL"
        const val EXPECTED_ROLE_ADMIN = "ROLE_ADMIN"
        const val EXPECTED_ROLE_NORMAL = "ROLE_NORMAL"
        const val TEST_NICKNAME = "라티"
        const val ACTIVE_STATUS = "ACTIVE"
        const val PENDING_STATUS = "PENDING"
        const val BLOCKED_STATUS = "BLOCKED"
    }

    @Test
    @DisplayName("이메일과 역할로 UserDetails를 생성한다")
    fun createUserDetails_success() {
        // when
        val userDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)

        // then
        assertThat(userDetails.username).isEqualTo(TEST_EMAIL)
        assertThat(userDetails.password).isNull()
        assertThat(userDetails.authorities).hasSize(1)
        assertThat(userDetails.authorities.first()).isEqualTo(SimpleGrantedAuthority(EXPECTED_ROLE_NORMAL))
    }

    @Nested
    inner class IsAccountNonLockedTest {
        @Test
        @DisplayName("계정 상태가 BLOCKED가 아니면 잠기지 않았다고 반환한다")
        fun isAccountNonLocked_returnsTrue() {
            // given
            val activeDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)
            val pendingDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, PENDING_STATUS)

            // when & then
            assertThat(activeDetails.isAccountNonLocked).isTrue()
            assertThat(pendingDetails.isAccountNonLocked).isTrue()
        }

        @Test
        @DisplayName("계정 상태가 BLOCKED이면 잠겼다는 false를 반환한다")
        fun isAccountNonLocked_returnsFalse() {
            // given
            val userDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, BLOCKED_STATUS)

            // when
            val result = userDetails.isAccountNonLocked

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    inner class IsEnabledTest {
        @Test
        @DisplayName("BLOCKED가 아니면 계정이 활성화되어 있다고 반환한다")
        fun isEnabled_returnsTrue() {
            // given
            val activeDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)
            val pendingDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, PENDING_STATUS)

            // when & then
            assertThat(activeDetails.isEnabled).isTrue()
            assertThat(pendingDetails.isEnabled).isTrue()
        }

        @Test
        @DisplayName("BLOCKED이면 계정이 비활성화되어 있다고 반환한다")
        fun isEnabled_returnsFalse() {
            // given
            val userDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, BLOCKED_STATUS)

            // when
            val result = userDetails.isEnabled

            // then
            assertThat(result).isFalse()
        }
    }

    @Test
    @DisplayName("계정이 만료되지 않았다고 반환한다")
    fun isAccountNonExpired_returnsTrue() {
        // given
        val userDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)

        // when
        val result = userDetails.isAccountNonExpired

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("자격 증명이 만료되지 않았다고 반환한다")
    fun isCredentialsNonExpired_returnsTrue() {
        // given
        val userDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)

        // when
        val result = userDetails.isCredentialsNonExpired

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("다양한 역할로 UserDetails를 생성할 수 있다")
    fun createUserDetailsWithDifferentRoles() {
        // given & when & then
        val adminDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_ADMIN, ACTIVE_STATUS)
        assertThat(adminDetails.authorities.first().authority).isEqualTo(EXPECTED_ROLE_ADMIN)

        val userDetails = SimpleTokenUserDetails(TEST_EMAIL, TEST_NICKNAME, ROLE_NORMAL, ACTIVE_STATUS)
        assertThat(userDetails.authorities.first().authority).isEqualTo(EXPECTED_ROLE_NORMAL)
    }
}

