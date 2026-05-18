package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.adapter.`in`.web.dto.TokenRefreshRequest
import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.port.`in`.TokenRefreshUseCase
import com.kdongsu5509.auth.config.SecurityConfig
import com.kdongsu5509.support.config.LoggingConfig
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(
    controllers = [RefreshController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [
                SecurityConfig::class,
                LoggingConfig::class
            ]
        )
    ]
)
@AutoConfigureMockMvc(addFilters = false)
class RefreshControllerTest {

    companion object {
        const val REQUEST_API = "/api/auth/refresh"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var tokenRefreshUseCase: TokenRefreshUseCase

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @Test
    @DisplayName("RefreshToken을 사용하여 토큰을 갱신한다")
    fun refresh_success() {
        // given
        val request = TokenRefreshRequest(refreshToken = "valid-refresh-token")
        val token = ImHereJwtToken(accessToken = "new-access-token", refreshToken = "new-refresh-token")
        given(tokenRefreshUseCase.refresh(any())).willReturn(token)

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
    }

    @Test
    @DisplayName("refreshToken이 비어있으면 400 에러를 반환한다")
    fun refresh_fail_empty_token() {
        // given
        val request = TokenRefreshRequest(refreshToken = "")

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
