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

@WebMvcTest(DeliveryResultNotificationController::class)
@ExtendWith(RestDocumentationExtension::class)
class DeliveryResultNotificationControllerTest {

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
        const val DELIVERY_RESULT_FCM_PATH = "/api/notification/fcm/delivery-result"
    }

    @Test
    @DisplayName("전송 결과 안내 FCM 알림 전송 - 본인에게 전송")
    fun send_delivery_result_notification() {
        val request = FcmNotificationRequest(
            receiverEmail = "any-receiver@example.com", // This should be ignored
            type = "DELIVERY_RESULT_NOTICE",
            body = "요청하신 알림이 성공적으로 전송되었습니다."
        )
        val userDetails = SimpleTokenUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            post(DELIVERY_RESULT_FCM_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andDo(
                document(
                    "delivery-result/success",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 정상 발송")
                            .summary("알림 정상 발송 전송 api")
                            .description("목적지 도착 알림을 정상적으로 발송한 경우 스스로에게 FCM을 통해 해당 내용을 전달")
                            .build()
                    )
                )
            )

        verify(notificationToUserCasePort).send(
            eq("sender-nick"),
            eq("sender@example.com"),
            eq("sender@example.com"), // Verified that it's sent to oneself
            eq("DELIVERY_RESULT_NOTICE"),
            eq("요청하신 알림이 성공적으로 전송되었습니다."),
            anyMap()
        )
    }

    @Test
    @DisplayName("body가 공백이면 400 반환")
    fun send_with_blank_body_returns_400() {
        val body = """{"receiverEmail":"receiver@example.com","type":"ANY_TYPE","body":""}"""
        val userDetails = SimpleTokenUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            post(DELIVERY_RESULT_FCM_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest)
            .andDo(
                document(
                    "delivery-result/empty-body",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 정상 발송")
                            .summary("알림 정상 발송 전송 api")
                            .description("목적지 도착 알림을 정상적으로 발송한 경우 스스로에게 FCM을 통해 해당 내용을 전달")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("인증 없이 요청 시 401")
    fun send_without_auth_returns_401() {
        val request = FcmNotificationRequest(
            receiverEmail = "any@example.com",
            type = "ANY_TYPE",
            body = "test"
        )

        mockMvc.perform(
            post(DELIVERY_RESULT_FCM_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isUnauthorized)
            .andDo(
                document(
                    "delivery-result/without-auth",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("알림 - 정상 발송")
                            .summary("알림 정상 발송 전송 api")
                            .description("목적지 도착 알림을 정상적으로 발송한 경우 스스로에게 FCM을 통해 해당 내용을 전달")
                            .build()
                    )
                )
            )
    }
}
