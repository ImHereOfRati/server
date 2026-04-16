package com.kdongsu5509.user.application.service.user

import com.kdongsu5509.support.config.SecurityConstants
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.io.PrintWriter
import java.io.StringWriter

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    companion object {
        const val VALID_TOKEN = "valid-jwt-token"
        const val INVALID_TOKEN = "invalid-jwt-token"
        const val NICKNAME = "rati"
        const val ACTIVE_STATUS = "ACTIVE"
        const val PENDING_STATUS = "PENDING"
        const val TEST_EMAIL = "test@example.com"
        const val NORMAL_ROLE = "ROLE_NORMAL"
        const val bearerToken = "Bearer $VALID_TOKEN"
    }

    @Mock
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    @Mock
    private lateinit var securityConstants: SecurityConstants

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtTokenUtil, securityConstants)
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증을 성공적으로 처리한다")
    fun doFilterInternal_validToken_success() {
        // given
        `when`(request.getHeader("Authorization")).thenReturn(bearerToken)
        `when`(jwtTokenUtil.validateToken(VALID_TOKEN)).thenReturn(true)
        `when`(jwtTokenUtil.getUserEmailFromToken(VALID_TOKEN)).thenReturn(TEST_EMAIL)
        `when`(jwtTokenUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(NORMAL_ROLE)
        `when`(jwtTokenUtil.getUserNicknameFromToken(VALID_TOKEN)).thenReturn(NICKNAME)
        `when`(jwtTokenUtil.getStatusFromToken(VALID_TOKEN)).thenReturn(ACTIVE_STATUS)

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(jwtTokenUtil).validateToken(VALID_TOKEN)
        verify(jwtTokenUtil).getUserEmailFromToken(VALID_TOKEN)
        verify(jwtTokenUtil).getRoleFromToken(VALID_TOKEN)
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
        verify(jwtTokenUtil, Mockito.never()).validateToken(ArgumentMatchers.anyString())
        verify(filterChain).doFilter(request, response)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 필터를 통과한다")
    fun doFilterInternal_noBearerPrefix_passesThrough() {
        // given
        `when`(request.getHeader("Authorization")).thenReturn("invalid-token")

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(jwtTokenUtil, Mockito.never()).validateToken(ArgumentMatchers.anyString())
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 401 응답을 반환한다")
    fun doFilterInternal_invalidToken_returns401() {
        // given
        val bearerToken = "Bearer $INVALID_TOKEN"

        `when`(request.getHeader("Authorization")).thenReturn(bearerToken)
        `when`(jwtTokenUtil.validateToken(INVALID_TOKEN)).thenReturn(false)
        `when`(response.writer).thenReturn(PrintWriter(StringWriter()))

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(jwtTokenUtil).validateToken(INVALID_TOKEN)
        verify(response).status = 401
        verify(response).contentType = "application/json;charset=UTF-8"
        verify(filterChain, Mockito.never()).doFilter(request, response)
    }

    @Test
    @DisplayName("이미 인증된 사용자가 있으면 새로운 인증을 설정하지 않는다")
    fun doFilterInternal_alreadyAuthenticated_doesNotSetNewAuthentication() {
        // given
        val existingAuth = UsernamePasswordAuthenticationToken(
            "existing@example.com",
            null,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        SecurityContextHolder.getContext().authentication = existingAuth

        `when`(request.getHeader("Authorization")).thenReturn(bearerToken)
        `when`(jwtTokenUtil.validateToken(VALID_TOKEN)).thenReturn(true)

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // then
        verify(jwtTokenUtil).validateToken(VALID_TOKEN)
        verify(filterChain).doFilter(request, response)
        // 기존 인증이 유지되어야 함
        assertThat(SecurityContextHolder.getContext().authentication).isEqualTo(existingAuth)
    }
}
