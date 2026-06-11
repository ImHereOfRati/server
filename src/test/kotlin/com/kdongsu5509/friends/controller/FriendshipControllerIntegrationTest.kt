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
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
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

    private fun friendshipResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.id").description("친구 관계 식별자"),
        fieldWithPath("data.friendAlias").description("친구 별칭").optional(),
        fieldWithPath("data.createdAt").description("생성일시").optional(),
        fieldWithPath("data.updatedAt").description("수정일시").optional(),
        subsectionWithPath("data.owner").description("친구 관계의 주체 정보"),
        subsectionWithPath("data.friend").description("친구 정보")
    )

    private fun sliceResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.content[].id").description("친구 관계 식별자"),
        fieldWithPath("data.content[].friendAlias").description("친구 별칭").optional(),
        fieldWithPath("data.content[].createdAt").description("생성일시").optional(),
        fieldWithPath("data.content[].updatedAt").description("수정일시").optional(),
        subsectionWithPath("data.content[].owner").description("친구 관계의 주체 정보"),
        subsectionWithPath("data.content[].friend").description("친구 정보"),
        fieldWithPath("data.hasNext").description("다음 페이지 존재 여부")
    )

    private fun errorResponseFields() = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드"),
        fieldWithPath("message").description("에러 메시지"),
        fieldWithPath("data").description("없음").optional()
    )

    @Test
    @DisplayName("내 친구 목록을 조회하고 200 OK를 반환하며 문서화한다")
    fun readAllSuccess() {
        val (owner, token) = createUserAndToken("owner-list@example.com", "owner-list")
        val (friend, _) = createUserAndToken("friend-list@example.com", "friend-list")
        friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "절친"))

        mockMvc.perform(
            get("/api/friendships")
                .header("Authorization", "Bearer $token")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].friendAlias").value("절친"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-read-all-success",
                    snippets = arrayOf(
                        queryParameters(
                            parameterWithName("page").description("페이지 번호").optional(),
                            parameterWithName("size").description("페이지 크기").optional()
                        ),
                        sliceResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("대상 유저와의 친구 여부를 조회하고 문서화한다")
    fun checkFriendStatusSuccess() {
        val (owner, token) = createUserAndToken("owner-status@example.com", "owner-status")
        val (friend, _) = createUserAndToken("friend-status@example.com", "friend-status")
        friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구상태"))

        mockMvc.perform(
            get("/api/friendships/target/{targetUserId}", friend.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-check-status-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("targetUserId").description("친구 여부를 확인할 대상 유저 식별자")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data").description("친구 여부")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("친구 관계 단건 조회에 성공하고 문서화한다")
    fun readByIdSuccess() {
        val (owner, token) = createUserAndToken("owner-read@example.com", "owner-read")
        val (friend, _) = createUserAndToken("friend-read@example.com", "friend-read")
        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "단건조회"))

        mockMvc.perform(
            get("/api/friendships/{id}", friendship.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendAlias").value("단건조회"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-read-by-id-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("조회할 친구 관계 식별자")
                        ),
                        friendshipResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 관계를 조회하면 404 NOT_FOUND를 반환한다")
    fun readByIdFailNotFound() {
        val (_, token) = createUserAndToken("owner-read-missing@example.com", "owner-read-missing")

        mockMvc.perform(
            get("/api/friendships/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-read-by-id-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("타인의 친구 관계를 조회하면 403 FORBIDDEN을 반환한다")
    fun readByIdFailForbidden() {
        val (owner, _) = createUserAndToken("owner-read-forbidden@example.com", "owner-read-forbidden")
        val (friend, _) = createUserAndToken("friend-read-forbidden@example.com", "friend-read-forbidden")
        val (_, otherToken) = createUserAndToken("other-read-forbidden@example.com", "other-read-forbidden")
        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "비공개"))

        mockMvc.perform(
            get("/api/friendships/{id}", friendship.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-read-by-id-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
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
                        friendshipResponseFields()
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
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-update-alias-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
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
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-update-alias-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
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
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-block-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("차단할 친구 관계 식별자")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 관계를 차단하면 404 NOT_FOUND를 반환한다")
    fun blockFriendFailNotFound() {
        val (_, token) = createUserAndToken("owner-block-missing@example.com", "owner-block-missing")

        mockMvc.perform(
            post("/api/friendships/{id}/block", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-block-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("타인의 친구 관계를 차단하면 403 FORBIDDEN을 반환한다")
    fun blockFriendFailForbidden() {
        val (owner, _) = createUserAndToken("owner-block-forbidden@example.com", "owner-block-forbidden")
        val (friend, _) = createUserAndToken("friend-block-forbidden@example.com", "friend-block-forbidden")
        val (_, otherToken) = createUserAndToken("other-block-forbidden@example.com", "other-block-forbidden")
        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))

        mockMvc.perform(
            post("/api/friendships/{id}/block", friendship.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-block-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
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
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-delete-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("삭제할 친구 관계 식별자")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 관계를 삭제하면 404 NOT_FOUND를 반환한다")
    fun deleteFriendshipFailNotFound() {
        val (_, token) = createUserAndToken("owner-delete-missing@example.com", "owner-delete-missing")

        mockMvc.perform(
            delete("/api/friendships/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-delete-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("타인의 친구 관계를 삭제하면 403 FORBIDDEN을 반환한다")
    fun deleteFriendshipFailForbidden() {
        val (owner, _) = createUserAndToken("owner-delete-forbidden@example.com", "owner-delete-forbidden")
        val (friend, _) = createUserAndToken("friend-delete-forbidden@example.com", "friend-delete-forbidden")
        val (_, otherToken) = createUserAndToken("other-delete-forbidden@example.com", "other-delete-forbidden")
        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))

        mockMvc.perform(
            delete("/api/friendships/{id}", friendship.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-delete-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

}
