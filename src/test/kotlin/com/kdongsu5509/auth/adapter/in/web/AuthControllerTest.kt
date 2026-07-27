package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.application.port.`in`.AuthUseCase
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.user.domain.OAuth2Provider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@ImHereLightWebMvcTest(controllers = [AuthController::class])
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    companion object {
        const val REQUEST_API = "/api/auth"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var authUseCase: AuthUseCase

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @Test
    @DisplayName("정상 요청이면 200 OK와 토큰을 반환한다")
    fun auth_success() {
        // given
        val request = OIDCAuthRequest(
            provider = OAuth2Provider.KAKAO,
            idToken = "test-id-token",
            nonce = "test-nonce"
        )

        val token = ImHereJwtToken(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        given(authUseCase.auth(any(), any(), any())).willReturn(token)

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
    }

    @Test
    @DisplayName("idToken이 빈값이면 400 에러를 던진다")
    fun auth_fail_cause_of_no_idToken() {
        // given
        val request = mapOf(
            "provider" to "KAKAO",
            "idToken" to "",
            "nonce" to "test-nonce"
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
    fun auth_fail_cause_of_no_provider() {
        // given
        val request = mapOf(
            "idToken" to "test-id-token",
            "nonce" to "test-nonce"
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
    @DisplayName("nonce가 없으면 400 에러를 던진다")
    fun auth_fail_cause_of_no_nonce() {
        // given
        val request = mapOf(
            "provider" to "KAKAO",
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
