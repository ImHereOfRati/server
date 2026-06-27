package com.kdongsu5509.auth.security.filter

import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class OttIpValidationFilterTest {

    private val allowedIp = "203.0.113.10"
    private val config = OttIpFilterConfig(id = "admin", nickname = "", allowedIps = listOf(allowedIp))
    private val filter = OttIpValidationFilter(config)

    @Test
    @DisplayName("허용 IP면 admin 모든 경로(로그인 페이지 포함)를 통과시킨다")
    fun allowsAllAdminPathsForAllowedIp() {
        val request = MockHttpServletRequest("GET", "/admin/login")
        request.addHeader("X-Real-IP", allowedIp)
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    @DisplayName("비허용 IP면 로그인 페이지조차 403으로 차단한다")
    fun blocksLoginPageForDisallowedIp() {
        val request = MockHttpServletRequest("GET", "/admin/login")
        request.addHeader("X-Real-IP", "198.51.100.7")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        verify(chain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("XFF 첫 요소로 허용 IP를 위조해도 X-Real-IP 기준이라 차단된다")
    fun blocksSpoofedXff() {
        val request = MockHttpServletRequest("POST", "/admin/ott/request")
        request.addHeader("X-Forwarded-For", "$allowedIp, 198.51.100.7")
        request.addHeader("X-Real-IP", "198.51.100.7")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        verify(chain, never()).doFilter(request, response)
        assertThat(response.status).isEqualTo(HttpStatus.FORBIDDEN.value())
    }
}
