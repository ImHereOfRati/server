package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.support.external.UserErrorAlertNotifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(NotificationReadController::class)
class NotificationReadControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var userErrorAlertNotifier: UserErrorAlertNotifier

    @MockitoBean
    private lateinit var notificationInboxUseCase: NotificationUseCase

    private val userDetails = ImHereUserDetails(
        email = "test@example.com",
        nickname = "tester",
        role = "USER",
        status = "ACTIVE",
        userId = UUID.randomUUID(),
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
    @DisplayName("알림 목록을 페이징 조회하여 200 OK를 반환한다")
    fun getNotifications_success() {
        val history = Notification.reconstruct(
            id = 1L,
            dedupeKey = "event:FCM",
            targetIdentifier = userDetails.requiredUserId.toString(),
            method = NotificationMethod.FCM,
            senderAlias = "sender",
            title = "test title",
            body = "test body",
            type = NotificationType.TERMS_UPDATE_NOTICE,
            extraData = emptyMap(),
            status = NotificationStatus.SENT,
            attempts = 0,
            lastError = null,
            sentAt = LocalDateTime.now(),
            isRead = false,
            createdAt = LocalDateTime.now()
        )

        whenever(notificationInboxUseCase.findByRecipientId(userDetails.requiredUserId, 0, 20))
            .thenReturn(listOf(history))

        mockMvc.perform(
            get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .with(user(userDetails))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.[0].id").value(1))
            .andExpect(jsonPath("$.data.[0].title").value("test title"))

        verify(notificationInboxUseCase).findByRecipientId(userDetails.requiredUserId, 0, 20)
    }

    @Test
    @DisplayName("알림 읽음 처리 시 204 No Content를 반환한다")
    fun markAsRead_success() {
        val notificationId = 10L

        mockMvc.perform(
            patch("/api/notifications/{id}/read", notificationId)
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isNoContent)

        verify(notificationInboxUseCase).markAsRead(userDetails.requiredUserId, notificationId)
    }
}
