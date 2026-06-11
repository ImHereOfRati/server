package com.kdongsu5509.auth.security.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication

@ExtendWith(MockitoExtension::class)
class OttLoginSuccessHandlerTest {
    private lateinit var handler: OttLoginSuccessHandler

    @BeforeEach
    fun setUp() {
        handler = OttLoginSuccessHandler()
    }

    @Test
    @DisplayName("onAuthenticationSuccess 호출 시 관리자 대시보드로 리다이렉트한다")
    fun onAuthenticationSuccess() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val authentication = mock(Authentication::class.java)

        handler.onAuthenticationSuccess(request, response, authentication)

        assertThat(response.status).isEqualTo(302)
        assertThat(response.redirectedUrl).isEqualTo("/admin")
    }
}
