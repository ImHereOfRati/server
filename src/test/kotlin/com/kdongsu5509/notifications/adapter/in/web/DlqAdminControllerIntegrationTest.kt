package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqQueueInfoResponse
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqReplayResponse
import com.kdongsu5509.notifications.application.service.DlqAdminService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.whenever

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
                    identifier = "notifications-dlq-purge-success"
                )
            )

        verify(dlqAdminService).purgeQueue("friend.dlq")
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
                    identifier = "notifications-dlq-access-forbidden"
                )
            )
    }
}
