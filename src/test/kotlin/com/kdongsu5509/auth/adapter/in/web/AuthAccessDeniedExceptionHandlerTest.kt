package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.AuthException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class AuthAccessDeniedExceptionHandlerTest {

    private val handler = AuthAccessDeniedExceptionHandler()

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("SecurityContext에 인증이 없으면 401과 IMHERE_INVALID_TOKEN을 반환한다")
    fun handleAuthorizationDeniedException_returns_401_when_authentication_is_null() {
        // given: SecurityContextHolder를 비운 상태

        // when
        val response = handler.handleAuthorizationDeniedException(AccessDeniedException("denied"))

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.imhereResponseCode).isEqualTo(AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode)
        assertThat(response.body?.message).isEqualTo("인증이 필요합니다.")
        assertThat(response.body?.data).isNull()
    }

    @Test
    @DisplayName("익명 토큰(AnonymousAuthenticationToken)이면 401을 반환한다")
    fun handleAuthorizationDeniedException_returns_401_when_authentication_is_anonymous_token() {
        // given
        givenAuthentication(
            AnonymousAuthenticationToken(
                "key",
                "anonymous",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            )
        )

        // when
        val response = handler.handleAuthorizationDeniedException(AccessDeniedException("denied"))

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.imhereResponseCode).isEqualTo(AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode)
    }

    @Test
    @DisplayName("익명 토큰 타입이 아니어도 이름이 anonymousUser면 401을 반환한다")
    fun handleAuthorizationDeniedException_returns_401_when_principal_name_is_anonymous_user() {
        // given
        givenAuthentication(
            UsernamePasswordAuthenticationToken("anonymousUser", null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        )

        // when
        val response = handler.handleAuthorizationDeniedException(AccessDeniedException("denied"))

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.imhereResponseCode).isEqualTo(AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode)
    }

    @Test
    @DisplayName("인증된 사용자가 권한이 부족하면 403과 IMHERE_ACCESS_DENIED를 반환한다")
    fun handleAuthorizationDeniedException_returns_403_when_authenticated_user_lacks_authority() {
        // given
        givenAuthentication(
            UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                listOf(SimpleGrantedAuthority("ROLE_NORMAL"))
            )
        )

        // when
        val response = handler.handleAuthorizationDeniedException(AccessDeniedException("denied"))

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.imhereResponseCode).isEqualTo(AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode)
        assertThat(response.body?.message).isEqualTo("접근 권한이 없습니다.")
        assertThat(response.body?.data).isNull()
    }

    @Test
    @DisplayName("AuthorizationDeniedException도 같은 판정으로 처리한다")
    fun handleAuthorizationDeniedException_handles_authorization_denied_exception() {
        // given
        givenAuthentication(
            UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                listOf(SimpleGrantedAuthority("ROLE_NORMAL"))
            )
        )

        // when
        val response = handler.handleAuthorizationDeniedException(
            AuthorizationDeniedException("denied")
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.imhereResponseCode).isEqualTo(AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode)
    }

    private fun givenAuthentication(authentication: Authentication) {
        SecurityContextHolder.getContext().authentication = authentication
    }
}
