package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.friends.controller.dto.CreateFriendRestrictionRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
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

class FriendRestrictionControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var friendRestrictionRepository: FriendRestrictionRepository
    @Autowired private lateinit var tokenProviderPort: ImHereTokenProviderPort

    private fun createUserAndToken(email: String, nickname: String): Pair<User, String> {
        val user = User.createWithPendingStatus(email, nickname, OAuth2Provider.KAKAO).activate()
        val saved = userRepository.save(user)
        val token = tokenProviderPort.issue(JwtTokenClaims.fromUser(saved)).accessToken
        return Pair(saved, token)
    }

    private fun restrictionResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.id").description("차단 식별자").optional(),
        fieldWithPath("data.type").description("제한 타입"),
        subsectionWithPath("data.restrictor").description("차단자 정보"),
        subsectionWithPath("data.restricted").description("피차단자 정보"),
        fieldWithPath("data.createdAt").description("생성일시").optional(),
        fieldWithPath("data.updatedAt").description("수정일시").optional(),
        fieldWithPath("data.expiredAt").description("만료일시").optional()
    )

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
    @DisplayName("내 차단 목록을 조회하고 200 OK를 반환하며 문서화한다")
    fun findAllSuccess() {
        val (restrictor, token) = createUserAndToken("restrictor-list@example.com", "restrictor-list")
        val (target, _) = createUserAndToken("target-list@example.com", "target-list")
        friendRestrictionRepository.save(
            FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK)
        )

        mockMvc.perform(
            get("/api/friends/restrictions")
                .header("Authorization", "Bearer $token")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].type").value("BLOCK"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-read-all-success",
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
    @DisplayName("정상적으로 유저를 차단하고 200 OK를 반환하며 문서화한다")
    fun restrictUserSuccessAndDocument() {
        val (_, token) = createUserAndToken("restrictor1@example.com", "restrictor")
        val (target, _) = createUserAndToken("target1@example.com", "target")

        val requestDto = CreateFriendRestrictionRequest(targetUserId = target.id!!)

        mockMvc.perform(
            post("/api/friends/restrictions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-create-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("targetUserId").description("차단할 유저 식별자")
                        ),
                        restrictionResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 유저를 차단하려 하면 404 NOT_FOUND를 반환한다")
    fun restrictUser_Fail_UserNotFound() {
        val (_, token) = createUserAndToken("restrictor2@example.com", "restrictor")
        val requestDto = CreateFriendRestrictionRequest(targetUserId = UUID.randomUUID())

        mockMvc.perform(
            post("/api/friends/restrictions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-create-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("차단 여부 조회를 성공하고 문서화한다")
    fun checkRestrictionStatusSuccess() {
        val (restrictor, token) = createUserAndToken("restrictor-check@example.com", "restrictor-check")
        val (target, _) = createUserAndToken("target-check@example.com", "target-check")
        friendRestrictionRepository.save(
            FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK)
        )

        mockMvc.perform(
            get("/api/friends/restrictions/target/{targetUserId}", target.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-check-status-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("targetUserId").description("차단 여부를 확인할 대상 유저 식별자")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data").description("차단 여부")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("정상적으로 차단을 해제(삭제)하면 200 OK를 반환한다")
    fun deleteRestriction_Success() {
        val (restrictor, token) = createUserAndToken("restrictor3@example.com", "restrictor")
        val (target, _) = createUserAndToken("target3@example.com", "target")

        val restriction = friendRestrictionRepository.save(
            FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK)
        )

        mockMvc.perform(
            delete("/api/friends/restrictions/{id}", restriction.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-delete-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("삭제할 차단 식별자")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 차단 정보를 삭제하려 하면 404 NOT_FOUND를 반환한다")
    fun deleteRestriction_Fail_NotFound() {
        val (_, token) = createUserAndToken("restrictor4@example.com", "restrictor")

        mockMvc.perform(
            delete("/api/friends/restrictions/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-delete-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("본인의 차단 정보가 아닌 것을 삭제하려 하면 403 FORBIDDEN을 반환한다")
    fun deleteRestriction_Fail_OwnerMismatch() {
        val (restrictor, _) = createUserAndToken("restrictor5@example.com", "restrictor")
        val (target, _) = createUserAndToken("target5@example.com", "target")
        val (_, otherToken) = createUserAndToken("other@example.com", "other")

        val restriction = friendRestrictionRepository.save(
            FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK)
        )

        mockMvc.perform(
            delete("/api/friends/restrictions/{id}", restriction.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-delete-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("차단 대상 사용자 식별자로 차단을 해제하고 문서화한다")
    fun unblockSuccess() {
        val (restrictor, token) = createUserAndToken("restrictor-unblock@example.com", "restrictor-unblock")
        val (target, _) = createUserAndToken("target-unblock@example.com", "target-unblock")
        friendRestrictionRepository.save(
            FriendRestriction(restrictor = restrictor, restricted = target, type = FriendRestrictionType.BLOCK)
        )

        mockMvc.perform(
            delete("/api/friends/restrictions/blocked-users/{restrictedId}", target.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-restriction-unblock-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("restrictedId").description("차단 해제할 대상 유저 식별자")
                        )
                    )
                )
            )
    }

}
