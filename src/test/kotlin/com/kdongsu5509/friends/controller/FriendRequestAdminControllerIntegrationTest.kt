package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.FriendRequestRepository
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

class FriendRequestAdminControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var friendRequestRepository: FriendRequestRepository
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

    private fun friendRequestSliceResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.content[].id").description("친구 요청 식별자"),
        fieldWithPath("data.content[].message").description("친구 요청 메시지"),
        subsectionWithPath("data.content[].requester").description("요청자 정보"),
        subsectionWithPath("data.content[].receiver").description("수신자 정보"),
        fieldWithPath("data.content[].createdAt").description("생성일시").optional(),
        fieldWithPath("data.content[].updatedAt").description("수정일시").optional(),
        fieldWithPath("data.hasNext").description("다음 페이지 존재 여부")
    )

    private fun errorResponseFields() = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드"),
        fieldWithPath("message").description("에러 메시지"),
        fieldWithPath("data").description("없음").optional()
    )

    @Test
    @DisplayName("관리자는 전체 친구 요청 목록을 조회하고 문서화한다")
    fun findAllAdminSuccess() {
        val (requester, _) = createUserAndToken("req-admin-list@example.com", "req-admin-list")
        val (receiver, _) = createUserAndToken("rec-admin-list@example.com", "rec-admin-list")
        val (_, adminToken) = createAdminUserAndToken("friend-request-admin@example.com", "friend-request-admin")
        friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "관리자 조회용 요청입니다"))

        mockMvc.perform(
            get("/api/admin/friend-requests")
                .header("Authorization", "Bearer $adminToken")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].message").value("관리자 조회용 요청입니다"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-read-all-admin-success",
                    snippets = arrayOf(
                        queryParameters(
                            parameterWithName("page").description("페이지 번호").optional(),
                            parameterWithName("size").description("페이지 크기").optional()
                        ),
                        friendRequestSliceResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("일반 사용자가 관리자 친구 요청 목록 조회를 시도하면 403 FORBIDDEN을 반환한다")
    fun findAllAdminFailForbidden() {
        val (_, token) = createUserAndToken("user-friend-request-admin@example.com", "user")

        mockMvc.perform(
            get("/api/admin/friend-requests")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-read-all-admin-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자는 친구 요청을 삭제하고 문서화한다")
    fun deleteByIdAdminSuccess() {
        val (requester, _) = createUserAndToken("req-delete-admin@example.com", "req-delete-admin")
        val (receiver, _) = createUserAndToken("rec-delete-admin@example.com", "rec-delete-admin")
        val (_, adminToken) = createAdminUserAndToken("friend-request-delete-admin@example.com", "friend-request-delete-admin")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "관리자 삭제"))

        mockMvc.perform(
            delete("/api/admin/friend-requests/{id}", request.id)
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-delete-admin-success",
                    snippets = arrayOf(
                        pathParameters(parameterWithName("id").description("관리자가 삭제할 친구 요청 식별자"))
                    )
                )
            )
    }
}
