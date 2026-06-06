package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.controller.dto.NewFriendRequest
import com.kdongsu5509.friends.domain.FriendRequest
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

class FriendRequestControllerIntegrationTest : WebIntegrationTestSupport() {

    private val requester = User(
        id = UUID.randomUUID(),
        email = "requester@example.com",
        nickname = "requester-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val receiver = User(
        id = UUID.randomUUID(),
        email = "receiver@example.com",
        nickname = "receiver-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Test
    @WithMockUser(username = "requester@example.com", roles = ["USER"])
    @DisplayName("정상적으로 친구 요청을 수행하고 200 OK를 반환하며 문서화한다")
    fun requestFriendSuccessAndDocument() {
        val requestDto = NewFriendRequest(
            targetId = receiver.id!!,
            message = "안녕하세요. 친하게 지내요!"
        )

        val friendRequest = FriendRequest(
            id = UUID.randomUUID(),
            requester = requester,
            receiver = receiver,
            message = requestDto.message,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-create-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("targetId").description("친구 요청을 보낼 대상 유저의 식별자"),
                            fieldWithPath("message").description("친구 요청 메시지 (10자 이상 255자 이하)")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.friendRequestId").description("생성된 친구 요청 식별자")
                        )
                    )
                )
            )
    }
}
