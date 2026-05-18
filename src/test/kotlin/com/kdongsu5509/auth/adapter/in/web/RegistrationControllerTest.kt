package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.port.`in`.RegisterUseCase
import com.kdongsu5509.auth.config.SecurityConfig
import com.kdongsu5509.auth.domain.OAuth2Provider
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
    controllers = [RegistrationController::class],
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
class RegistrationControllerTest {

    companion object {
        const val REQUEST_API = "/api/auth/registration"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var registerUseCase: RegisterUseCase

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @Test
    fun register_success() {
        // given
        val request = OIDCAuthRequest(
            provider = OAuth2Provider.KAKAO,
            idToken = "test-id-token"
        )

        val token = ImHereJwtToken(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        given(registerUseCase.register(any(), any())).willReturn(token)

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
    }

    @Test
    @DisplayName("idToken이 빈값이면 400 에러를 던진다")
    fun register_fail_cause_of_no_idToken() {
        // given
        val request = mapOf(
            "provider" to "KAKAO",
            "idToken" to ""
        )

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("provider가 없으면 400 에러를 던진다")
    fun register_fail_cause_of_no_provider() {
        // given
        val request = mapOf(
            "idToken" to "test-id-token"
        )

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
