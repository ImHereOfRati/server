package com.kdongsu5509.auth.security.filter

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.support.exception.ImHereBaseException
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    @Mock
    private lateinit var tokenParser: ImHereTokenParserPort

    private lateinit var filterChain: FilterChain
    private lateinit var filter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        filterChain = mock(FilterChain::class.java)
        filter = JwtAuthenticationFilter(tokenParser, listOf("/api/auth", "/actuator/**"))
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("permit-all 경로는 JWT 검증 없이 통과한다")
    fun doFilterInternal_permitAllPath() {
        val request = MockHttpServletRequest().apply {
            servletPath = "/api/auth"
            addHeader("Authorization", "Bearer invalidToken")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        verify(tokenParser, never()).validate(anyString())
    }

    @Test
    @DisplayName("토큰이 없는 요청은 검증 없이 통과한다")
    fun doFilterInternal_noToken() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 401을 반환하고 통과시키지 않는다")
    fun doFilterInternal_invalidToken() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer invalidToken")
        val response = MockHttpServletResponse()

        whenever(tokenParser.validate("invalidToken")).thenReturn(false)

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(401)
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보를 저장하고 통과한다")
    fun doFilterInternal_validToken() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer validToken")
        val response = MockHttpServletResponse()

        val claims = JwtTokenClaims(UUID.randomUUID(), "test@test.com", "Tester", "ROLE_USER", "ACTIVE")
        whenever(tokenParser.validate("validToken")).thenReturn(true)
        whenever(tokenParser.parse("validToken")).thenReturn(claims)

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assertThat(authentication).isNotNull
        assertThat(authentication?.name).isEqualTo("test@test.com")
    }

    @Test
    @DisplayName("비활성화된 계정이면 401을 반환한다")
    fun doFilterInternal_disabledUser() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer validToken")
        val response = MockHttpServletResponse()

        val claims = JwtTokenClaims(UUID.randomUUID(), "test@test.com", "Tester", "ROLE_USER", "DISABLED")
        whenever(tokenParser.validate("validToken")).thenReturn(true)
        whenever(tokenParser.parse("validToken")).thenReturn(claims)

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(401)
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 Authorization 헤더는 토큰으로 보지 않고 통과시킨다")
    fun doFilterInternal_authorizationHeaderWithoutBearerPrefix() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        verify(tokenParser, never()).validate(anyString())
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    @DisplayName("검증 단계에서 ImHereBaseException이 던져지면 해당 에러코드로 응답한다")
    fun doFilterInternal_validateThrowsImHereException() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer expiredToken")
        val response = MockHttpServletResponse()

        whenever(tokenParser.validate("expiredToken"))
            .thenThrow(ImHereBaseException(AuthException.IMHERE_EXPIRED_TOKEN))

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(AuthException.IMHERE_EXPIRED_TOKEN.httpStatus.value())
        assertThat(response.contentAsString).contains(AuthException.IMHERE_EXPIRED_TOKEN.imhereErrorCode)
    }

    @Test
    @DisplayName("인증 생성 중 예상치 못한 예외가 발생하면 IMHERE_ACCESS_DENIED로 응답한다")
    fun doFilterInternal_unexpectedExceptionDuringAuthenticationCreation() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer validToken")
        val response = MockHttpServletResponse()

        whenever(tokenParser.validate("validToken")).thenReturn(true)
        whenever(tokenParser.parse("validToken")).thenThrow(IllegalStateException("claims broken"))

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(AuthException.IMHERE_ACCESS_DENIED.httpStatus.value())
        assertThat(response.contentAsString).contains("claims broken")
    }

    @Test
    @DisplayName("파싱 중 예외가 발생하면 예외 응답을 반환한다")
    fun doFilterInternal_parseException() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer validToken")
        val response = MockHttpServletResponse()

        whenever(tokenParser.validate("validToken")).thenReturn(true)
        whenever(tokenParser.parse("validToken")).thenThrow(ImHereBaseException(AuthException.IMHERE_EXPIRED_TOKEN))

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(401)
    }
}
