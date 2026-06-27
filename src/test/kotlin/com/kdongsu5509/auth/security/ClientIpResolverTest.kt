package com.kdongsu5509.auth.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpResolverTest {

    @Test
    @DisplayName("X-Real-IP가 있으면 최우선으로 사용한다")
    fun preferXRealIp() {
        val request = MockHttpServletRequest()
        request.addHeader("X-Real-IP", "203.0.113.10")
        request.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.10")
        request.remoteAddr = "10.0.0.1"

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10")
    }

    @Test
    @DisplayName("X-Forwarded-For 첫 요소를 위조해도 X-Real-IP 기준이라 무력화된다")
    fun spoofedXffFirstElementIsIgnored() {
        val request = MockHttpServletRequest()
        // 공격자가 허용 IP를 XFF 첫 요소에 주입, nginx가 실제 IP를 X-Real-IP로 덮어쓴 상황
        request.addHeader("X-Forwarded-For", "39.118.242.177, 198.51.100.7")
        request.addHeader("X-Real-IP", "198.51.100.7")
        request.remoteAddr = "10.0.0.1"

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.7")
    }

    @Test
    @DisplayName("X-Real-IP 부재 시 X-Forwarded-For의 마지막(append된 실제 peer) 요소를 사용한다")
    fun fallbackToLastXffHop() {
        val request = MockHttpServletRequest()
        request.addHeader("X-Forwarded-For", "39.118.242.177, 198.51.100.7")
        request.remoteAddr = "10.0.0.1"

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.7")
    }

    @Test
    @DisplayName("프록시 헤더가 없으면 remoteAddr로 fallback 한다")
    fun fallbackToRemoteAddr() {
        val request = MockHttpServletRequest()
        request.remoteAddr = "192.0.2.55"

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.0.2.55")
    }

    @Test
    @DisplayName("X-Real-IP가 unknown이면 신뢰하지 않고 fallback 한다")
    fun ignoreUnknownXRealIp() {
        val request = MockHttpServletRequest()
        request.addHeader("X-Real-IP", "unknown")
        request.remoteAddr = "192.0.2.99"

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.0.2.99")
    }
}
