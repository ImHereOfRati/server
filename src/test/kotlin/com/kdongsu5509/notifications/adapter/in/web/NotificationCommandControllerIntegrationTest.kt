package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@RecordApplicationEvents
class NotificationCommandControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var applicationEvents: ApplicationEvents

    private val userDetails = ImHereUserDetails(
        email = "sender@example.com",
        nickname = "senderNick",
        role = "USER",
        status = "ACTIVE",
        userId = UUID.randomUUID(),
    )

    private val targetId = UUID.randomUUID()
    private val otherTargetId = UUID.randomUUID()

    private fun publishedEvents(): List<NotificationEvent> =
        applicationEvents.stream(NotificationEvent::class.java).toList()

    private fun perform(body: String): ResultActions = mockMvc.perform(
        post("/api/notifications")
            .with(csrf())
            .with(user(userDetails))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
    )

    private fun perform(request: NotificationRequest): ResultActions =
        perform(jsonMapper.writeValueAsString(request))

    @Test
    @DisplayName("단일 알림 발행 요청은 큐에 적재되고 202 Accepted를 반환한다")
    fun sendSuccess() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = listOf(targetId.toString()),
            type = NotificationType.LOCATION_TARGET,
            extraData = mapOf("key" to "value")
        )

        perform(request).andExpect(status().isAccepted)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-success",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetIds").description("대상 식별자 목록"),
                            fieldWithPath("type").description("알림 타입"),
                            fieldWithPath("isClientAllowedType").ignored(),
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

        val events = publishedEvents()
        assertThat(events).hasSize(1)

        val event = events.first()
        assertThat(event.senderNickname).isEqualTo("senderNick")
        assertThat(event.senderId).isEqualTo(userDetails.requiredUserId)
        assertThat(event.notificationMethod).isEqualTo(NotificationMethod.FCM)
        assertThat(event.targetIdentifier).isEqualTo(targetId.toString())
        assertThat(event.type).isEqualTo(NotificationType.LOCATION_TARGET)
        assertThat(event.extraData).containsEntry("key", "value")
    }

    @Test
    @DisplayName("대상이 여럿이면 대상 수만큼 이벤트가 발행된다")
    fun sendMultipleSuccess() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = listOf(targetId.toString(), otherTargetId.toString()),
            type = NotificationType.LOCATION_TARGET,
            extraData = mapOf("key" to "value")
        )

        perform(request).andExpect(status().isAccepted)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-multiple-success",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetIds").description("대상 식별자 목록"),
                            fieldWithPath("type").description("알림 타입"),
                            fieldWithPath("isClientAllowedType").ignored(),
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

        val events = publishedEvents()
        assertThat(events).hasSize(2)
        assertThat(events.map { it.targetIdentifier })
            .containsExactly(targetId.toString(), otherTargetId.toString())
        assertThat(events.map { it.type }).containsOnly(NotificationType.LOCATION_TARGET)
    }

    @Test
    @DisplayName("출발 알림 요청도 클라이언트가 발송할 수 있다")
    fun sendDepartureSuccess() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = listOf(targetId.toString()),
            type = NotificationType.DEPARTURE,
            extraData = mapOf("body" to "[ImHere]\n우리집 출발")
        )

        perform(request).andExpect(status().isAccepted)

        assertThat(publishedEvents().map { it.type }).containsExactly(NotificationType.DEPARTURE)
    }

    @Test
    @DisplayName("대상 ID가 비어 있으면 400 Bad Request를 반환한다")
    fun sendFailsWhenTargetIdIsBlank() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = listOf(""),
            type = NotificationType.LOCATION_TARGET,
            extraData = emptyMap()
        )

        perform(request).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-fail-blank-target-id",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetIds").description("대상 식별자 목록"),
                            fieldWithPath("type").description("알림 타입"),
                            fieldWithPath("isClientAllowedType").ignored(),
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

        assertThat(publishedEvents()).isEmpty()
    }

    @Test
    @DisplayName("대상 목록이 비어 있으면 400 Bad Request를 반환한다")
    fun sendFailsWhenTargetIdsEmpty() {
        val request = NotificationRequest(
            notificationMethod = NotificationMethod.FCM,
            targetIds = emptyList(),
            type = NotificationType.LOCATION_TARGET,
            extraData = emptyMap()
        )

        perform(request).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-fail-empty-target-ids",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식"),
                            fieldWithPath("targetIds").description("대상 식별자 목록"),
                            fieldWithPath("type").description("알림 타입"),
                            fieldWithPath("isClientAllowedType").ignored(),
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

        assertThat(publishedEvents()).isEmpty()
    }

    @Test
    @DisplayName("발송 방식이 없으면 400 Bad Request를 반환한다")
    fun sendFailsWhenNotificationMethodMissing() {
        val requestJson = """
            {
              "targetIds": ["$targetId"],
              "type": "LOCATION_TARGET",
              "extraData": {"key":"value"}
            }
        """.trimIndent()

        perform(requestJson).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-send-fail-missing-notification-method",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("notificationMethod").description("발송 방식").type(JsonFieldType.STRING)
                                .optional(),
                            fieldWithPath("targetIds").description("대상 식별자 목록"),
                            fieldWithPath("type").description("알림 타입"),
                            subsectionWithPath("extraData").description("추가 데이터").optional()
                        )
                    )
                )
            )

        assertThat(publishedEvents()).isEmpty()
    }
}
