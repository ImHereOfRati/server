package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmTokenEnrollRequest
import com.kdongsu5509.notifications.application.port.`in`.FcmTokenEnrollUseCase
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(FcmTokenEnrollController::class)
class FcmTokenEnrollControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var fcmTokenEnrollUseCase: FcmTokenEnrollUseCase

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    private val userDetails = ImHereUserDetails(
        email = "test@example.com",
        nickname = "tester",
        role = "USER",
        status = "ACTIVE"
    )

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    @Test
    @DisplayName("FCM 토큰 등록 성공 시 201 CREATED를 반환한다")
    fun enroll_success() {
        val requestDto = FcmTokenEnrollRequest(
            fcmToken = "test-fcm-token",
            deviceType = DeviceType.IOS
        )

        mockMvc.perform(
            post("/api/fcm-tokens")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        ).andExpect(status().isCreated)

        verify(fcmTokenEnrollUseCase).save(
            userDetails.email,
            requestDto.fcmToken,
            requestDto.deviceType
        )
    }

    @Test
    @DisplayName("FCM 토큰 값이 없으면 400 Bad Request를 반환한다")
    fun enroll_fail_when_token_is_blank() {
        val requestDto = FcmTokenEnrollRequest(
            fcmToken = "",
            deviceType = DeviceType.IOS
        )

        mockMvc.perform(
            post("/api/fcm-tokens")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        ).andExpect(status().isBadRequest)
    }
}
