package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.application.port.`in`.ActivateUserUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.UserActivationCommand
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(UserActivationController::class)
@Import(UserActivationControllerWebMvcTest.MethodSecurityTestConfig::class)
class UserActivationControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var activateUserUseCase: ActivateUserUseCase

    @MockitoBean
    private lateinit var tokenParser: ImHereTokenParserPort

    @MockitoBean
    private lateinit var securityWhiteList: SecurityWhiteList

    @MockitoBean
    private lateinit var accessLogPrinter: AccessLogPrinter

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val REQUEST_API = "/api/auth/activation"
        const val TEST_EMAIL = "pending@example.com"
    }

    @Test
    @DisplayName("PENDING 사용자가 약관 동의를 제출하면 사용자를 활성화하고 JWT 토큰을 반환한다")
    fun activate_success() {
        // given
        val userDetails = pendingUserDetails()
        val request = mapOf(
            "consents" to listOf(
                mapOf("id" to 1L, "agreed" to true),
                mapOf("id" to 2L, "agreed" to false),
            )
        )
        val token = ImHereJwtToken(accessToken = "access-token", refreshToken = "refresh-token")

        given(activateUserUseCase.activate(any(), any())).willReturn(token)

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("OK"))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))

        val commandCaptor = argumentCaptor<UserActivationCommand>()
        then(activateUserUseCase).should().activate(commandCaptor.capture(), any())

        assertThat(commandCaptor.firstValue.email).isEqualTo(TEST_EMAIL)
        assertThat(commandCaptor.firstValue.consents).containsExactly(
            UserActivationCommand.TermConsentCommand(id = 1L, isAgreed = true),
            UserActivationCommand.TermConsentCommand(id = 2L, isAgreed = false),
        )
    }

    @Test
    @DisplayName("약관 동의 목록이 비어 있으면 400 Bad Request를 반환한다")
    fun activate_fail_empty_consents() {
        // given
        val request = mapOf("consents" to emptyList<Map<String, Any>>())

        // when & then
        mockMvc.perform(
            post(REQUEST_API)
                .with(csrf())
                .with(user(pendingUserDetails()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-000"))

        then(activateUserUseCase).shouldHaveNoInteractions()
    }

    private fun pendingUserDetails(): ImHereUserDetails {
        return ImHereUserDetails(
            email = TEST_EMAIL,
            nickname = "pending",
            role = UserRole.NORMAL.name,
            status = UserStatus.PENDING.name
        )
    }

    @TestConfiguration
    @EnableMethodSecurity
    class MethodSecurityTestConfig
}
