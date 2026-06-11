package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FriendshipAdminControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var friendshipRepository: FriendshipRepository
    @Autowired private lateinit var tokenProviderPort: ImHereTokenProviderPort

    private fun createUserAndToken(email: String, nickname: String): Pair<User, String> {
        val user = User.createWithPendingStatus(email, nickname, OAuth2Provider.KAKAO).activate()
        val saved = userRepository.save(user)
        val token = tokenProviderPort.issue(JwtTokenClaims.fromUser(saved)).accessToken
        return Pair(saved, token)
    }

    private fun createAdminUserAndToken(email: String, nickname: String): Pair<User, String> {
        val user = User.createWithPendingStatus(email, nickname, OAuth2Provider.KAKAO).activate()
        val saved = userRepository.save(user)
        val token = tokenProviderPort.issue(JwtTokenClaims(uid = saved.id!!, email = saved.email, nickname = saved.nickname, role = "ADMIN", status = saved.statusName())).accessToken
        return Pair(saved, token)
    }

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
    @DisplayName("관리자는 전체 친구 관계 목록을 조회하고 문서화한다")
    fun readAllAdminSuccess() {
        val (owner, _) = createUserAndToken("owner-admin@example.com", "owner-admin")
        val (friend, _) = createUserAndToken("friend-admin@example.com", "friend-admin")
        val (_, adminToken) = createAdminUserAndToken("admin-friendship@example.com", "admin")
        friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "관리자조회"))

        mockMvc.perform(
            get("/api/admin/friendships")
                .header("Authorization", "Bearer $adminToken")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].friendAlias").value("관리자조회"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-read-all-admin-success",
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
    @DisplayName("일반 사용자가 전체 친구 관계 목록 조회를 시도하면 403 FORBIDDEN을 반환한다")
    fun readAllAdminFailForbidden() {
        val (_, token) = createUserAndToken("user-admin-read@example.com", "user")

        mockMvc.perform(
            get("/api/admin/friendships")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-read-all-admin-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자는 친구 관계를 삭제하고 204 NO_CONTENT를 반환한다")
    fun deleteFriendshipAdminSuccess() {
        val (owner, _) = createUserAndToken("owner-delete-admin@example.com", "owner-delete-admin")
        val (friend, _) = createUserAndToken("friend-delete-admin@example.com", "friend-delete-admin")
        val (_, adminToken) = createAdminUserAndToken("admin-delete@example.com", "admin-delete")
        val friendship = friendshipRepository.save(Friendship(owner = owner, friend = friend, friendAlias = "친구"))

        mockMvc.perform(
            delete("/api/admin/friendships/{id}", friendship.id)
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isNoContent)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friendship-delete-admin-success",
                    snippets = arrayOf(
                        pathParameters(parameterWithName("id").description("관리자가 삭제할 친구 관계 식별자"))
                    )
                )
            )
    }
}
