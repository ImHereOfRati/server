package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.controller.dto.UpdateAliasRequest
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.user.domain.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*

class FriendshipControllerIntegrationTest : WebIntegrationTestSupport() {

    private val owner = User(
        id = UUID.randomUUID(),
        email = "owner@example.com",
        nickname = "owner-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val friend = User(
        id = UUID.randomUUID(),
        email = "friend@example.com",
        nickname = "friend-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Test
    @WithMockUser(username = "owner@example.com", roles = ["USER"])
    @DisplayName("정상적으로 친구 별칭을 수정하고 200 OK를 반환하며 문서화한다")
    fun updateAliasSuccessAndDocument() {
        val friendshipId = UUID.randomUUID()
        val requestDto = UpdateAliasRequest(alias = "단짝")

        val friendship = Friendship(
            id = friendshipId,
            owner = owner,
            friend = friend,
            friendAlias = requestDto.alias,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        mockMvc.perform(
            patch("/api/friendships/{id}/alias", friendshipId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-update-alias-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("alias").description("변경할 친구 별칭")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.id").description("친구 관계 식별자"),
                            fieldWithPath("data.friendAlias").description("변경된 별칭")
                        )
                    )
                )
            )
    }
}
