package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.friends.controller.dto.UpdateAliasRequest
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

class FriendshipControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var friendshipRepository: FriendshipRepository
    @Autowired private lateinit var tokenProviderPort: ImHereTokenProviderPort

    private fun createUserAndToken(email: String, nickname: String): Pair<User, String> {
        val user = User.createWithPendingStatus(email, nickname, OAuth2Provider.KAKAO).activate()
        val saved = userRepository.save(user)
        val token = tokenProviderPort.issue(JwtTokenClaims.fromUser(saved)).accessToken
        return Pair(saved, token)
    }

    @Test
    @DisplayName("정상적으로 친구 별칭을 수정하고 200 OK를 반환하며 문서화한다")
    fun updateAliasSuccessAndDocument() {
        val (owner, token) = createUserAndToken("owner1@example.com", "owner1")
        val (friend, _) = createUserAndToken("friend1@example.com", "friend1")

        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))

        val requestDto = UpdateAliasRequest(alias = "단짝")

        mockMvc.perform(
            patch("/api/friendships/{id}/alias", friendship.id)
                .header("Authorization", "Bearer $token")
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
                            fieldWithPath("data.friendAlias").description("변경된 별칭"),
                            fieldWithPath("data.createdAt").description("생성일시").optional(),
                            fieldWithPath("data.updatedAt").description("수정일시").optional(),
                            subsectionWithPath("data.owner").description("친구 관계의 주체 정보"),
                            subsectionWithPath("data.friend").description("친구 정보")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 관계의 별칭을 수정하려 하면 404 NOT_FOUND를 반환한다")
    fun updateAlias_Fail_NotFound() {
        val (_, token) = createUserAndToken("owner2@example.com", "owner2")
        val requestDto = UpdateAliasRequest(alias = "단짝")

        mockMvc.perform(
            patch("/api/friendships/{id}/alias", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isNotFound)
            .andDo(MockMvcRestDocumentationWrapper.document("friendship-update-alias-fail-not-found"))
    }

    @Test
    @DisplayName("본인의 친구 관계가 아닌 별칭을 수정하려 하면 403 FORBIDDEN을 반환한다")
    fun updateAlias_Fail_OwnerMismatch() {
        val (owner, _) = createUserAndToken("owner3@example.com", "owner3")
        val (friend, _) = createUserAndToken("friend3@example.com", "friend3")
        val (_, otherToken) = createUserAndToken("other@example.com", "other")

        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))
        val requestDto = UpdateAliasRequest(alias = "단짝")

        mockMvc.perform(
            patch("/api/friendships/{id}/alias", friendship.id)
                .header("Authorization", "Bearer $otherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isForbidden)
            .andDo(MockMvcRestDocumentationWrapper.document("friendship-update-alias-fail-forbidden"))
    }

    @Test
    @DisplayName("친구를 차단하면 200 OK를 반환한다")
    fun blockFriend_Success() {
        val (owner, token) = createUserAndToken("owner4@example.com", "owner4")
        val (friend, _) = createUserAndToken("friend4@example.com", "friend4")

        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))

        mockMvc.perform(
            post("/api/friendships/{id}/block", friendship.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(MockMvcRestDocumentationWrapper.document("friendship-block-success"))
    }

    @Test
    @DisplayName("친구 관계를 삭제하면 204 NO_CONTENT를 반환한다")
    fun deleteFriendship_Success() {
        val (owner, token) = createUserAndToken("owner5@example.com", "owner5")
        val (friend, _) = createUserAndToken("friend5@example.com", "friend5")

        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))

        mockMvc.perform(
            delete("/api/friendships/{id}", friendship.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
            .andDo(MockMvcRestDocumentationWrapper.document("friendship-delete-success"))
    }
}
