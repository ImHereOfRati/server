package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiNotificationRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.out.NotificationProducePort
import com.kdongsu5509.notifications.domain.NotificationMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NotificationCommandControllerIntegrationTest : WebIntegrationTestSupport() {

    @MockitoBean
    private lateinit var notificationProducePort: NotificationProducePort

    private val userDetails = ImHereUserDetails(
        email = "sender@example.com",
        nickname = "senderNick",
        role = "USER",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("단일 알림 발행 요청은 큐에 적재되고 202 Accepted를 반환한다")
    fun sendSuccess() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetId = "target@example.com",
            type = NotificationType.FRIEND_REQUEST_RECEIVED,
            extraData = mapOf("key" to "value")
        )

        mockMvc.perform(
            post("/api/notifications")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isAccepted)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-success",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetId").description("대상 식별자"),
                            fieldWithPath("type").description("알림 타입"),
                            subsectionWithPath("extraData").description("추가 데이터").optional()
                        ),
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data").description("없음").optional()
                        )
                    )
                )
            )

        val captor = argumentCaptor<NotificationCommand>()
        verify(notificationProducePort).send(captor.capture())

        val command = captor.firstValue
        assertThat(command.senderNickname).isEqualTo("senderNick")
        assertThat(command.senderEmail).isEqualTo("sender@example.com")
        assertThat(command.notificationMethod).isEqualTo(NotificationMethod.FCM)
        assertThat(command.targetIdentifier).isEqualTo("target@example.com")
        assertThat(command.type).isEqualTo(NotificationType.FRIEND_REQUEST_RECEIVED.name)
        assertThat(command.extraData).containsEntry("key", "value")
    }

    @Test
    @DisplayName("배치 알림 발행 요청은 대상별로 큐에 적재된다")
    fun sendMultipleSuccess() {
        val request = MultiNotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = listOf("target1@example.com", "target2@example.com"),
            type = NotificationType.FRIEND_REQUEST_RECEIVED,
            extraData = mapOf("key" to "value")
        )

        mockMvc.perform(
            post("/api/notifications/batch")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isAccepted)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-batch-success",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetIds").description("대상 식별자 목록"),
                            fieldWithPath("type").description("알림 타입"),
                            subsectionWithPath("extraData").description("추가 데이터").optional()
                        ),
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data").description("없음").optional()
                        )
                    )
                )
            )

        val captor = argumentCaptor<NotificationCommand>()
        verify(notificationProducePort, times(2)).send(captor.capture())

        assertThat(captor.allValues).hasSize(2)
        assertThat(captor.allValues.map { it.targetIdentifier }).containsExactly(
            "target1@example.com",
            "target2@example.com"
        )
        assertThat(captor.allValues.map { it.type }).containsOnly(NotificationType.FRIEND_REQUEST_RECEIVED.name)
    }

    @Test
    @DisplayName("대상 ID가 비어 있으면 400 Bad Request를 반환한다")
    fun sendFailsWhenTargetIdIsBlank() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetId = "",
            type = NotificationType.FRIEND_REQUEST_RECEIVED,
            extraData = emptyMap()
        )

        mockMvc.perform(
            post("/api/notifications")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-fail-blank-target-id",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetId").description("대상 식별자"),
                            fieldWithPath("type").description("알림 타입"),
                            subsectionWithPath("extraData").description("추가 데이터").optional()
                        ),
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드"),
                            fieldWithPath("message").description("에러 메시지"),
                            fieldWithPath("data").description("없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("발송 방식이 없으면 400 Bad Request를 반환한다")
    fun sendFailsWhenNotificationMethodMissing() {
        val requestJson = """
            {
              "targetId": "target@example.com",
              "type": "FRIEND_REQUEST_RECEIVED",
              "extraData": {"key":"value"}
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/notifications")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-fail-missing-notification-method",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식").type(JsonFieldType.STRING).optional(),
                            fieldWithPath("targetId").description("대상 식별자"),
                            fieldWithPath("type").description("알림 타입"),
                            subsectionWithPath("extraData").description("추가 데이터").optional()
                        ),
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드"),
                            fieldWithPath("message").description("에러 메시지"),
                            fieldWithPath("data").description("없음").optional()
                        )
                    )
                )
            )
    }
}
