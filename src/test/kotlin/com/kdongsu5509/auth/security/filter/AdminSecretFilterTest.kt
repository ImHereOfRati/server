package com.kdongsu5509.auth.security.filter

import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AdminSecretFilterTest {

    private lateinit var filter: AdminSecretFilter
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = AdminSecretFilter("secret")
        filterChain = mock(FilterChain::class.java)
    }

    @Test
    @DisplayName("일반 API 요청은 검증 없이 통과한다")
    fun doFilterInternal_normalApi() {
        val request = MockHttpServletRequest("GET", "/api/user/info")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("어드민 인증 API 요청 시 시크릿 키가 일치하면 통과한다")
    fun doFilterInternal_adminApi_validSecret() {
        val request = MockHttpServletRequest("POST", "/api/admin/auth/ott")
        request.addHeader("X-ADMIN-SECRET", "secret")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("어드민 인증 API 요청 시 시크릿 키가 일치하지 않으면 403을 반환하고 통과시키지 않는다")
    fun doFilterInternal_adminApi_invalidSecret() {
        val request = MockHttpServletRequest("POST", "/api/admin/auth/ott")
        request.addHeader("X-ADMIN-SECRET", "wrong")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        Assertions.assertThat(response.status).isEqualTo(403)
    }

    @Test
    @DisplayName("어드민 인증 API 요청 시 시크릿 키가 없으면 403을 반환하고 통과시키지 않는다")
    fun doFilterInternal_adminApi_missingSecret() {
        val request = MockHttpServletRequest("POST", "/api/admin/auth/ott")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        verify(filterChain, never()).doFilter(request, response)
        Assertions.assertThat(response.status).isEqualTo(403)
    }
}
