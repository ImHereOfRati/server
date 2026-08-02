package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class NotificationReadControllerIntegrationTest : WebIntegrationTestSupport() {
    @Autowired
    private lateinit var persistencePort: NotificationPersistencePort

    private val userDetails = ImHereUserDetails(
        email = "receiver@example.com",
        nickname = "receiver",
        role = "USER",
        status = "ACTIVE",
        userId = UUID.randomUUID(),
    )

    @Test
    @DisplayName("수신함은 SENT FCM만 기존 응답 스키마로 반환하고 SMS와 미발송 알림을 제외한다")
    fun inbox_filters_by_method_and_status() {
        val sentFcm =
            persistencePort.save(requested("sent-fcm", NotificationMethod.FCM).markSent(java.time.LocalDateTime.now()))
        persistencePort.save(requested("pending-fcm", NotificationMethod.FCM))
        persistencePort.save(requested("sent-sms", NotificationMethod.SMS).markSent(java.time.LocalDateTime.now()))

        mockMvc.perform(get("/api/notifications").with(user(userDetails)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(sentFcm.id))
            .andExpect(jsonPath("$.data[0].senderAlias").value("sender"))
            .andExpect(jsonPath("$.data[0].type").value(NotificationType.FRIEND_REQUEST_RECEIVED.name))
    }

    @Test
    @DisplayName("수신함 알림 읽음 처리는 Notification 애그리게이트에 저장된다")
    fun mark_as_read_updates_notification() {
        val sent = persistencePort.save(
            requested(
                "read-target",
                NotificationMethod.FCM
            ).markSent(java.time.LocalDateTime.now())
        )

        mockMvc.perform(
            patch("/api/notifications/{id}/read", sent.id)
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isNoContent)

        assertThat(persistencePort.findById(requireNotNull(sent.id))!!.isRead).isTrue()
    }

    private fun requested(key: String, method: NotificationMethod): Notification =
        Notification.request(
            dedupeKey = "$key:$method",
            targetIdentifier = userDetails.requiredUserId.toString(),
            method = method,
            rendered = NotificationTemplate.render(
                type = NotificationType.FRIEND_REQUEST_RECEIVED,
                senderAlias = "sender",
            ),
            bodyOverride = if (method == NotificationMethod.SMS) "문자 본문" else null,
        )
}
