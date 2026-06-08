package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.out.persistence.NotificationHistoryJpaEntity
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataNotificationHistoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NotificationReadControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var notificationHistoryRepository: SpringDataNotificationHistoryRepository

    private val userDetails = ImHereUserDetails(
        email = "receiver@example.com",
        nickname = "receiverNick",
        role = "USER",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("알림 조회 요청은 저장된 알림 목록을 반환한다")
    fun getNotificationsSuccess() {
        notificationHistoryRepository.save(
            NotificationHistoryJpaEntity(
                receiverEmail = userDetails.email,
                senderNickname = "senderNick",
                title = "title-1",
                body = "body-1",
                type = "NOTICE",
                path = null,
                isRead = false
            )
        )

        mockMvc.perform(
            get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .with(user(userDetails))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].senderNickname").value("senderNick"))
            .andExpect(jsonPath("$.data[0].title").value("title-1"))
            .andExpect(jsonPath("$.data[0].isRead").value(false))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-get-success",
                    snippets = arrayOf(
                        queryParameters(
                            parameterWithName("page").description("조회할 페이지 번호"),
                            parameterWithName("size").description("페이지 크기")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data[].id").description("알림 ID"),
                            fieldWithPath("data[].senderNickname").description("보낸 사람 닉네임"),
                            fieldWithPath("data[].title").description("알림 제목"),
                            fieldWithPath("data[].body").description("알림 본문"),
                            fieldWithPath("data[].type").description("알림 타입"),
                            fieldWithPath("data[].path").description("이동 경로").optional(),
                            fieldWithPath("data[].isRead").description("읽음 여부"),
                            fieldWithPath("data[].createdAt").description("생성 시각").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("알림 읽음 요청은 읽음 상태를 true로 변경한다")
    fun markAsReadSuccess() {
        val saved = notificationHistoryRepository.save(
            NotificationHistoryJpaEntity(
                receiverEmail = userDetails.email,
                senderNickname = "senderNick",
                title = "title-1",
                body = "body-1",
                type = "NOTICE",
                path = null,
                isRead = false
            )
        )

        mockMvc.perform(
            patch("/api/notifications/{id}/read", saved.id)
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isNoContent)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-mark-as-read-success"
                )
            )

        val updated = notificationHistoryRepository.findById(saved.id!!).orElseThrow()
        assertThat(updated.isRead).isTrue()
    }
}
