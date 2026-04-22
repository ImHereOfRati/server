package com.kdongsu5509.notifications.adapter.`in`.web

import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmNotificationRequest
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.support.config.SecurityConstants
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.application.service.user.JwtTokenUtil
import com.kdongsu5509.user.application.service.user.SimpleTokenUserDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
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

@WebMvcTest(ArrivalNotificationController::class)
@ExtendWith(RestDocumentationExtension::class)
class ArrivalNotificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var notificationToUserCasePort: NotificationToUserCasePort

    @MockitoBean
    private lateinit var accessLogPrinter: AccessLogPrinter

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @MockitoBean
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @MockitoBean
    private lateinit var securityConstants: SecurityConstants

    @Autowired
    private lateinit var objectMapper: JsonMapper

    @BeforeEach
    fun setUp(
        webApplicationContext: WebApplicationContext,
        restDocumentation: RestDocumentationContextProvider
    ) {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val ARRIVAL_FCM_PATH = "/api/notification/fcm/arrival"
    }

    @Test
    @DisplayName("도착 알림 FCM 알림 전송 (NOTI-006)")
    fun send_arrival_notification() {
        val request = FcmNotificationRequest(
            receiverEmail = "receiver@example.com",
            type = "ANY_TYPE",
            body = "목적지에 도착했습니다."
        )
        val userDetails = SimpleTokenUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            post(ARRIVAL_FCM_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andDo(
                document(
                    "arrival-notification/success",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 도착")
                            .summary("도착 알림 FCM 발송")
                            .description("목적지 도착 알림을 FCM을 통해 특정 상대방에게 전달합니다.")
                            .build()
                    )
                )
            )

        verify(notificationToUserCasePort).send(
            eq("sender-nick"),
            eq("sender@example.com"),
            eq("receiver@example.com"),
            eq("ARRIVAL_CONFIRMATION"),
            eq("목적지에 도착했습니다."),
            anyMap()
        )
    }

    @Test
    @DisplayName("receiverEmail 형식이 잘못되면 400 반환")
    fun send_with_invalid_receiver_email_returns_400() {
        val body = """{"receiverEmail":"not-an-email","type":"ANY_TYPE","body":"도착했습니다."}"""
        val userDetails = SimpleTokenUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            post(ARRIVAL_FCM_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
            .andDo(
                document(
                    "arrival-notification/invalid-email",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 도착")
                            .summary("도착 알림 FCM 발송")
                            .description("목적지 도착 알림을 FCM을 통해 특정 상대방에게 전달합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("body가 공백이면 400 반환")
    fun send_with_blank_body_returns_400() {
        val body = """{"receiverEmail":"receiver@example.com","type":"ANY_TYPE","body":""}"""
        val userDetails = SimpleTokenUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            post(ARRIVAL_FCM_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
            .andDo(
                document(
                    "arrival-notification/blank-body",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 도착")
                            .summary("도착 알림 FCM 발송")
                            .description("목적지 도착 알림을 FCM을 통해 특정 상대방에게 전달합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("인증 없이 요청 시 401")
    fun send_without_auth_returns_401() {
        val request = FcmNotificationRequest(
            receiverEmail = "receiver@example.com",
            type = "ANY_TYPE",
            body = "도착했습니다."
        )

        mockMvc.perform(
            post(ARRIVAL_FCM_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isUnauthorized)
            .andDo(
                document(
                    "arrival-notification/without-auth-request",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 도착")
                            .summary("도착 알림 FCM 발송")
                            .description("목적지 도착 알림을 FCM을 통해 특정 상대방에게 전달합니다.")
                            .build()
                    )
                )
            )
    }
}
