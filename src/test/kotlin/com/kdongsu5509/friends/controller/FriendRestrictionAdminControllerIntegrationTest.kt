package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
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

class FriendRestrictionAdminControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var friendRestrictionRepository: FriendRestrictionRepository
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

    private fun restrictionSliceResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.content[].id").description("차단 식별자").optional(),
        fieldWithPath("data.content[].type").description("제한 타입"),
        subsectionWithPath("data.content[].restrictor").description("차단자 정보"),
        subsectionWithPath("data.content[].restricted").description("피차단자 정보"),
        fieldWithPath("data.content[].createdAt").description("생성일시").optional(),
        fieldWithPath("data.content[].updatedAt").description("수정일시").optional(),
        fieldWithPath("data.content[].expiredAt").description("만료일시").optional(),
        fieldWithPath("data.hasNext").description("다음 페이지 존재 여부")
    )

    private fun errorResponseFields() = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드"),
        fieldWithPath("message").description("에러 메시지"),
        fieldWithPath("data").description("없음").optional()
    )

    @Test
    @DisplayName("관리자는 전체 차단 목록을 조회하고 문서화한다")
    fun findAllAdminSuccess() {
        val (restrictor, _) = createUserAndToken("restrictor-admin@example.com", "restrictor-admin")
        val (target, _) = createUserAndToken("target-admin@example.com", "target-admin")
        val (_, adminToken) = createAdminUserAndToken("admin-restriction@example.com", "admin-restriction")
        friendRestrictionRepository.save(FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK))

        mockMvc.perform(
            get("/api/admin/friend-restrictions")
                .header("Authorization", "Bearer $adminToken")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].type").value("BLOCK"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-read-all-admin-success",
                    snippets = arrayOf(
                        queryParameters(
                            parameterWithName("page").description("페이지 번호").optional(),
                            parameterWithName("size").description("페이지 크기").optional()
                        ),
                        restrictionSliceResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("일반 사용자가 전체 차단 목록 조회를 시도하면 403 FORBIDDEN을 반환한다")
    fun findAllAdminFailForbidden() {
        val (_, token) = createUserAndToken("user-read-admin-restriction@example.com", "user")

        mockMvc.perform(
            get("/api/admin/friend-restrictions")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-read-all-admin-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자는 차단 정보를 삭제하고 문서화한다")
    fun deleteByIdAdminSuccess() {
        val (restrictor, _) = createUserAndToken("restrictor-delete-admin@example.com", "restrictor-delete-admin")
        val (target, _) = createUserAndToken("target-delete-admin@example.com", "target-delete-admin")
        val (_, adminToken) = createAdminUserAndToken("admin-delete-restriction@example.com", "admin-delete-restriction")
        val restriction = friendRestrictionRepository.save(FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK))

        mockMvc.perform(
            delete("/api/admin/friend-restrictions/{id}", restriction.id)
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-delete-admin-success",
                    snippets = arrayOf(
                        pathParameters(parameterWithName("id").description("관리자가 삭제할 차단 식별자"))
                    )
                )
            )
    }
}
