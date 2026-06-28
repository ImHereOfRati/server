package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiNotificationRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
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

@WebMvcTest(NotificationCommandController::class)
class NotificationCommandControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var notificationEnqueueUseCase: NotificationEnqueueUseCase

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier


    private val userDetails = ImHereUserDetails(
        email = "sender@example.com",
        nickname = "senderNickname",
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
    @DisplayName("단건 알림 발송 요청 시 202 ACCEPTED를 반환한다")
    fun send_success() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetId = "target@example.com",
            type = NotificationType.LOCATION_SHARE_RECEIVED,
            extraData = mapOf("key" to "value")
        )

        mockMvc.perform(
            post("/api/notifications")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isAccepted)

        val captor = argumentCaptor<NotificationCommand>()
        verify(notificationEnqueueUseCase).enqueue(captor.capture())

        val cmd = captor.firstValue
        assertThat(cmd.senderNickname).isEqualTo("senderNickname")
        assertThat(cmd.senderEmail).isEqualTo("sender@example.com")
        assertThat(cmd.targetIdentifier).isEqualTo("target@example.com")
        assertThat(cmd.type).isEqualTo(NotificationType.LOCATION_SHARE_RECEIVED.name)
    }

    @Test
    @DisplayName("출발 알림 요청도 클라이언트가 발송할 수 있다")
    fun send_departure_success() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetId = "target@example.com",
            type = NotificationType.DEPARTURE,
            extraData = mapOf("body" to "[ImHere]\n우리집 출발")
        )

        mockMvc.perform(
            post("/api/notifications")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isAccepted)

        val captor = argumentCaptor<NotificationCommand>()
        verify(notificationEnqueueUseCase).enqueue(captor.capture())

        assertThat(captor.firstValue.type).isEqualTo(NotificationType.DEPARTURE.name)
    }

    @Test
    @DisplayName("다건 알림 발송 요청 시 202 ACCEPTED를 반환한다")
    fun sendMultiple_success() {
        val request = MultiNotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = listOf("target1@example.com", "target2@example.com"),
            type = NotificationType.LOCATION_SHARE_RECEIVED,
            extraData = mapOf("key" to "value")
        )

        mockMvc.perform(
            post("/api/notifications/batch")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isAccepted)

        val captor = argumentCaptor<MultipleNotificationCommand>()
        verify(notificationEnqueueUseCase).enqueueMultiple(captor.capture())

        val cmd = captor.firstValue
        assertThat(cmd.senderNickname).isEqualTo("senderNickname")
        assertThat(cmd.senderEmail).isEqualTo("sender@example.com")
        assertThat(cmd.targetIdentifiers).hasSize(2)
        assertThat(cmd.type).isEqualTo(NotificationType.LOCATION_SHARE_RECEIVED.name)
    }
}
