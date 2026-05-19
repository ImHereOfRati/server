package com.kdongsu5509.auth.security.filter

import com.kdongsu5509.auth.application.JwtTokenClaims
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.security.SecurityWhiteList
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.context.SecurityContextHolder
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    companion object {
        const val VALID_TOKEN = "valid-jwt-token"
        const val INVALID_TOKEN = "invalid-jwt-token"
        const val NICKNAME = "rati"
        const val ACTIVE_STATUS = "ACTIVE"
        const val TEST_EMAIL = "test@example.com"
        const val NORMAL_ROLE = "NORMAL"
        const val bearerToken = "Bearer $VALID_TOKEN"
    }

    @Mock
    private lateinit var tokenParser: ImHereTokenParserPort

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    @Mock
    private lateinit var securityWhiteList: SecurityWhiteList

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        jwtAuthenticationFilter = JwtAuthenticationFilter(tokenParser, securityWhiteList)
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증을 성공적으로 처리한다")
    fun doFilterInternal_validToken_success() {
        // given
        val claims = JwtTokenClaims(
            uid = UUID.randomUUID(),
            email = TEST_EMAIL,
            nickname = NICKNAME,
            role = NORMAL_ROLE,
            status = ACTIVE_STATUS,
            expiration = LocalDateTime.now().plusHours(1)
        )
        `when`(request.getHeader("Authorization")).thenReturn(bearerToken)
        `when`(tokenParser.validate(VALID_TOKEN)).thenReturn(true)
        `when`(tokenParser.parse(VALID_TOKEN)).thenReturn(claims)

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(tokenParser).validate(VALID_TOKEN)
        verify(tokenParser).parse(VALID_TOKEN)
        verify(filterChain).doFilter(request, response)
        assertThat(SecurityContextHolder.getContext().authentication).isNotNull()
        assertThat(SecurityContextHolder.getContext().authentication!!.name).isEqualTo(TEST_EMAIL)
    }

    @Test
    @DisplayName("토큰이 없으면 필터를 통과한다")
    fun doFilterInternal_noToken_passesThrough() {
        // given
        `when`(request.getHeader("Authorization")).thenReturn(null)

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(tokenParser, Mockito.never()).validate(ArgumentMatchers.anyString())
        verify(filterChain).doFilter(request, response)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 401 응답을 반환한다")
    fun doFilterInternal_invalidToken_returns401() {
        // given
        val bearerToken = "Bearer $INVALID_TOKEN"

        `when`(request.getHeader("Authorization")).thenReturn(bearerToken)
        `when`(tokenParser.validate(INVALID_TOKEN)).thenReturn(false)
        `when`(response.writer).thenReturn(PrintWriter(StringWriter()))

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(tokenParser).validate(INVALID_TOKEN)
        verify(response).status = 401
        verify(response).contentType = "application/json;charset=UTF-8"
        verify(filterChain, Mockito.never()).doFilter(request, response)
    }
}
