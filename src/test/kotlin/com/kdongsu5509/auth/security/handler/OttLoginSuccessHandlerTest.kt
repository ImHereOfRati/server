package com.kdongsu5509.auth.security.handler

import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication

@ExtendWith(MockitoExtension::class)
class OttLoginSuccessHandlerTest {

    @Mock
    private lateinit var tokenIssuer: ImHereTokenIssuerPort

    private val adminId = "admin"
    private val adminNickname = "Admin"
    private lateinit var handler: OttLoginSuccessHandler

    @BeforeEach
    fun setUp() {
        handler = OttLoginSuccessHandler(tokenIssuer, adminId, adminNickname)
    }

    @Test
    @DisplayName("onAuthenticationSuccess 호출 시 액세스 토큰을 생성하여 응답에 담는다")
    fun onAuthenticationSuccess() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val authentication = mock(Authentication::class.java)

        whenever(tokenIssuer.createAdminAccessToken(any())).thenReturn("admin.jwt.token")

        handler.onAuthenticationSuccess(request, response, authentication)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.contentAsString).contains("admin.jwt.token")
    }
}
