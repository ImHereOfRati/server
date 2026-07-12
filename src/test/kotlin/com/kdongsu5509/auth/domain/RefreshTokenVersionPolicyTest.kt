package com.kdongsu5509.auth.domain

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.support.exception.ImHereBaseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RefreshTokenVersionPolicyTest {

    @Test
    @DisplayName("토큰 버전이 현재 버전과 일치하면 통과한다")
    fun matches_passes() {
        assertThatCode { RefreshTokenVersionPolicy.assertMatches(currentVersion = 3L, tokenVersion = 3L) }
            .doesNotThrowAnyException()
    }

    @Test
    @DisplayName("토큰 버전이 현재 버전과 다르면 IMHERE_INVALID_TOKEN 예외를 던진다")
    fun mismatch_throws() {
        val exception = assertThrows<ImHereBaseException> {
            RefreshTokenVersionPolicy.assertMatches(currentVersion = 1L, tokenVersion = 0L)
        }
        assertThat(exception.errorCode).isEqualTo(AuthException.IMHERE_INVALID_TOKEN)
    }
}
