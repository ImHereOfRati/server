package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.controller.dto.CreateFriendRestrictionRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.user.domain.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*

class FriendRestrictionControllerIntegrationTest : WebIntegrationTestSupport() {

    private val restrictor = User(
        id = UUID.randomUUID(),
        email = "restrictor@example.com",
        nickname = "restrictor-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val restricted = User(
        id = UUID.randomUUID(),
        email = "restricted@example.com",
        nickname = "restricted-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Test
    @WithMockUser(username = "restrictor@example.com", roles = ["USER"])
    @DisplayName("정상적으로 유저를 차단하고 200 OK를 반환하며 문서화한다")
    fun restrictUserSuccessAndDocument() {
        val requestDto = CreateFriendRestrictionRequest(
            targetUserId = restricted.id!!
        )

        val restriction = FriendRestriction(
            id = UUID.randomUUID(),
            restrictor = restrictor,
            restricted = restricted,
            type = FriendRestrictionType.BLOCK,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        mockMvc.perform(
            post("/api/friends/restrictions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-create-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("targetUserId").description("차단할 유저의 식별자")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.id").description("생성된 차단 내역 식별자"),
                            fieldWithPath("data.type").description("제한 타입 (예: BLOCK)")
                        )
                    )
                )
            )
    }
}
