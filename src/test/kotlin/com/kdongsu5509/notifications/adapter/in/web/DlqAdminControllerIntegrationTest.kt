package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqQueueInfoResponse
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqReplayResponse
import com.kdongsu5509.notifications.application.service.DlqAdminService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DlqAdminControllerIntegrationTest : WebIntegrationTestSupport() {

    @MockitoBean
    private lateinit var dlqAdminService: DlqAdminService

    private val adminUserDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    private val normalUserDetails = ImHereUserDetails(
        email = "user@example.com",
        nickname = "user",
        role = "USER",
        status = "ACTIVE"
    )

    private fun errorResponseFields() = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드"),
        fieldWithPath("message").description("에러 메시지"),
        fieldWithPath("data").description("없음").optional()
    )

    @Test
    @DisplayName("관리자는 DLQ 전체 정보를 조회할 수 있다")
    fun getAllDlqInfoSuccess() {
        whenever(dlqAdminService.getAllDlqInfo()).thenReturn(
            listOf(
                DlqQueueInfoResponse(queueName = "friend.dlq", messageCount = 3, consumerCount = 1)
            )
        )

        mockMvc.perform(
            get("/api/admin/dead-letter-queues")
                .with(user(adminUserDetails))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].queueName").value("friend.dlq"))
            .andExpect(jsonPath("$.data[0].messageCount").value(3))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-get-all-success",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data[].queueName").description("DLQ 이름"),
                            fieldWithPath("data[].messageCount").description("메시지 수"),
                            fieldWithPath("data[].consumerCount").description("컨슈머 수")
                        )
                    )
                )
            )

        verify(dlqAdminService).getAllDlqInfo()
    }

    @Test
    @DisplayName("관리자는 특정 DLQ 정보를 조회할 수 있다")
    fun getDlqInfoSuccess() {
        whenever(dlqAdminService.getQueueInfo("friend.dlq")).thenReturn(
            DlqQueueInfoResponse(queueName = "friend.dlq", messageCount = 7, consumerCount = 2)
        )

        mockMvc.perform(
            get("/api/admin/dead-letter-queues/{queueName}", "friend.dlq")
                .with(user(adminUserDetails))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.queueName").value("friend.dlq"))
            .andExpect(jsonPath("$.data.messageCount").value(7))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-get-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("queueName").description("조회할 DLQ 이름")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.queueName").description("DLQ 이름"),
                            fieldWithPath("data.messageCount").description("메시지 수"),
                            fieldWithPath("data.consumerCount").description("컨슈머 수")
                        )
                    )
                )
            )

        verify(dlqAdminService).getQueueInfo("friend.dlq")
    }

    @Test
    @DisplayName("알 수 없는 DLQ 정보 조회 요청은 500 Internal Server Error를 반환한다")
    fun getDlqInfoFailUnknownQueue() {
        whenever(dlqAdminService.getQueueInfo("unknown.dlq")).thenThrow(IllegalArgumentException("알 수 없는 DLQ입니다"))

        mockMvc.perform(
            get("/api/admin/dead-letter-queues/{queueName}", "unknown.dlq")
                .with(user(adminUserDetails))
        )
            .andExpect(status().isInternalServerError)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-get-fail-unknown-queue",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자는 DLQ 재처리 요청을 count 값과 함께 전달할 수 있다")
    fun replayMessagesSuccess() {
        whenever(dlqAdminService.replayMessages("friend.dlq", 5)).thenReturn(
            DlqReplayResponse(queueName = "friend.dlq", replayedCount = 5)
        )

        mockMvc.perform(
            post("/api/admin/dead-letter-queues/{queueName}/replay-jobs", "friend.dlq")
                .with(csrf())
                .param("count", "5")
                .with(user(adminUserDetails))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.queueName").value("friend.dlq"))
            .andExpect(jsonPath("$.data.replayedCount").value(5))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-replay-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("queueName").description("재처리할 DLQ 이름")
                        ),
                        queryParameters(
                            parameterWithName("count").description("재처리할 최대 메시지 수").optional()
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.queueName").description("DLQ 이름"),
                            fieldWithPath("data.replayedCount").description("재처리된 메시지 수")
                        )
                    )
                )
            )

        verify(dlqAdminService).replayMessages("friend.dlq", 5)
    }

    @Test
    @DisplayName("알 수 없는 DLQ 재처리 요청은 500 Internal Server Error를 반환한다")
    fun replayMessagesFailUnknownQueue() {
        whenever(dlqAdminService.replayMessages("unknown.dlq", 5)).thenThrow(IllegalArgumentException("알 수 없는 DLQ입니다"))

        mockMvc.perform(
            post("/api/admin/dead-letter-queues/{queueName}/replay-jobs", "unknown.dlq")
                .with(csrf())
                .param("count", "5")
                .with(user(adminUserDetails))
        )
            .andExpect(status().isInternalServerError)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-replay-fail-unknown-queue",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자는 DLQ 메시지를 전체 삭제할 수 있다")
    fun purgeQueueSuccess() {
        mockMvc.perform(
            delete("/api/admin/dead-letter-queues/{queueName}/messages", "friend.dlq")
                .with(csrf())
                .with(user(adminUserDetails))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-purge-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("queueName").description("비울 DLQ 이름")
                        )
                    )
                )
            )

        verify(dlqAdminService).purgeQueue("friend.dlq")
    }

    @Test
    @DisplayName("알 수 없는 DLQ 삭제 요청은 500 Internal Server Error를 반환한다")
    fun purgeQueueFailUnknownQueue() {
        doThrow(IllegalArgumentException("알 수 없는 DLQ입니다")).whenever(dlqAdminService).purgeQueue("unknown.dlq")

        mockMvc.perform(
            delete("/api/admin/dead-letter-queues/{queueName}/messages", "unknown.dlq")
                .with(csrf())
                .with(user(adminUserDetails))
        )
            .andExpect(status().isInternalServerError)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-purge-fail-unknown-queue",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자가 아니면 DLQ 관리자 API 접근이 거부된다")
    fun forbiddenForNonAdmin() {
        mockMvc.perform(
            get("/api/admin/dead-letter-queues")
                .with(user(normalUserDetails))
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-dlq-access-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }
}
