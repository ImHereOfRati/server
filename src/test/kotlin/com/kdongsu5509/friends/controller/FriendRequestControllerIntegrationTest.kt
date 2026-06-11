package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.friends.controller.dto.NewFriendRequest
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
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

class FriendRequestControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var friendRequestRepository: FriendRequestRepository
    @Autowired private lateinit var friendshipRepository: FriendshipRepository
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
        val token = tokenProviderPort.issue(
            JwtTokenClaims(
                uid = saved.id!!,
                email = saved.email,
                nickname = saved.nickname,
                role = "ADMIN",
                status = saved.statusName()
            )
        ).accessToken
        return Pair(saved, token)
    }

    private fun errorResponseFields() = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드"),
        fieldWithPath("message").description("에러 메시지"),
        fieldWithPath("data").description("없음").optional()
    )

    private fun friendRequestResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.id").description("친구 요청 식별자"),
        fieldWithPath("data.message").description("친구 요청 메시지"),
        subsectionWithPath("data.requester").description("요청자 정보"),
        subsectionWithPath("data.receiver").description("수신자 정보"),
        fieldWithPath("data.createdAt").description("생성일시").optional(),
        fieldWithPath("data.updatedAt").description("수정일시").optional()
    )

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

    private fun friendshipResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.id").description("친구 관계 식별자"),
        fieldWithPath("data.friendAlias").description("친구 별칭").optional(),
        subsectionWithPath("data.owner").description("친구 관계의 주체 정보"),
        subsectionWithPath("data.friend").description("친구 정보"),
        fieldWithPath("data.createdAt").description("생성일시").optional(),
        fieldWithPath("data.updatedAt").description("수정일시").optional()
    )

    private fun restrictionResponseFields() = relaxedResponseFields(
        fieldWithPath("imhereResponseCode").description("응답 코드"),
        fieldWithPath("message").description("응답 메시지"),
        fieldWithPath("data.id").description("제한 식별자"),
        fieldWithPath("data.type").description("제한 타입"),
        subsectionWithPath("data.restrictor").description("제한 주체 정보"),
        subsectionWithPath("data.restricted").description("제한 대상 정보"),
        fieldWithPath("data.createdAt").description("생성일시").optional(),
        fieldWithPath("data.updatedAt").description("수정일시").optional(),
        fieldWithPath("data.expiredAt").description("만료일시").optional()
    )

    @Test
    @DisplayName("정상적으로 친구 요청을 수행하고 200 OK를 반환하며 문서화한다")
    fun requestFriendSuccessAndDocument() {
        val (requester, token) = createUserAndToken("req1@example.com", "req1")
        val (receiver, _) = createUserAndToken("rec1@example.com", "rec1")

        val requestDto = NewFriendRequest(
            targetId = receiver.id!!,
            message = "안녕하세요. 친하게 지내요!"
        )

        mockMvc.perform(
            post("/api/friends/requests")
                .header("Authorization", "Bearer $token")
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

    @Test
    @DisplayName("자기 자신에게 친구 요청을 보내면 400 BAD_REQUEST를 반환한다")
    fun requestFriendFailSelfFriendship() {
        val (requester, token) = createUserAndToken("req-self@example.com", "req-self")
        val requestDto = NewFriendRequest(targetId = requester.id!!, message = "이것은 10자 이상의 자기 요청입니다")

        mockMvc.perform(
            post("/api/friends/requests")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-fail-self-friendship",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 유저에게 친구 요청 시 404 NOT_FOUND를 반환한다")
    fun requestFriend_Fail_UserNotFound() {
        val (_, token) = createUserAndToken("req2@example.com", "req2")

        val requestDto = NewFriendRequest(targetId = UUID.randomUUID(), message = "이것은 10자 이상의 메시지입니다!")

        mockMvc.perform(
            post("/api/friends/requests")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-fail-user-not-found",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드"),
                            fieldWithPath("message").description("에러 메시지"),
                            fieldWithPath("data").description("null").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("이미 친구 요청을 보낸 대상에게 다시 요청 시 409 CONFLICT를 반환한다")
    fun requestFriend_Fail_AlreadyRequested() {
        val (requester, token) = createUserAndToken("req3@example.com", "req3")
        val (receiver, _) = createUserAndToken("rec3@example.com", "rec3")

        friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "첫번째 요청"))

        val requestDto = NewFriendRequest(targetId = receiver.id!!, message = "이것은 두번째 보내는 10자 이상의 요청입니다!")

        mockMvc.perform(
            post("/api/friends/requests")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isConflict)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-fail-already-requested",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("이미 친구 관계인 유저에게 친구 요청 시 409 CONFLICT를 반환한다")
    fun requestFriend_Fail_AlreadyFriend() {
        val (requester, token) = createUserAndToken("req4@example.com", "req4")
        val (receiver, _) = createUserAndToken("rec4@example.com", "rec4")

        friendshipRepository.save(Friendship(owner = requester, friend = receiver, friendAlias = "친구"))

        val requestDto = NewFriendRequest(targetId = receiver.id!!, message = "우리 다시 친하게 지내자 제발!")

        mockMvc.perform(
            post("/api/friends/requests")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isConflict)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-fail-already-friend",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("차단된 상태에서 친구 요청 시 403 FORBIDDEN을 반환한다")
    fun requestFriend_Fail_Restricted() {
        val (requester, token) = createUserAndToken("req5@example.com", "req5")
        val (receiver, _) = createUserAndToken("rec5@example.com", "rec5")

        friendRestrictionRepository.save(FriendRestriction(restrictor = receiver, restricted = requester, type = FriendRestrictionType.BLOCK))

        val requestDto = NewFriendRequest(targetId = receiver.id!!, message = "차단 좀 풀어줄래? 진짜 미안해.")

        mockMvc.perform(
            post("/api/friends/requests")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isUnprocessableEntity)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-fail-restricted",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("관리자는 전체 친구 요청 목록을 조회하고 문서화한다")
    fun findAllAdminSuccess() {
        val (requester, _) = createUserAndToken("req-admin-list@example.com", "req-admin-list")
        val (receiver, _) = createUserAndToken("rec-admin-list@example.com", "rec-admin-list")
        val (_, adminToken) = createAdminUserAndToken("friend-request-admin@example.com", "friend-request-admin")
        friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "관리자 조회용 요청입니다"))

        mockMvc.perform(
            get("/api/friends/requests/admin")
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
            get("/api/friends/requests/admin")
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
    @DisplayName("내가 보낸 친구 요청 목록을 조회하고 문서화한다")
    fun findSentRequestsSuccess() {
        val (requester, token) = createUserAndToken("req-sent@example.com", "req-sent")
        val (receiver, _) = createUserAndToken("rec-sent@example.com", "rec-sent")
        friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "보낸 요청 목록용 메시지"))

        mockMvc.perform(
            get("/api/friends/requests")
                .header("Authorization", "Bearer $token")
                .param("type", "SENT")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].message").value("보낸 요청 목록용 메시지"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-read-sent-success",
                    snippets = arrayOf(
                        queryParameters(
                            parameterWithName("type").description("조회 타입 (SENT 또는 RECEIVED)"),
                            parameterWithName("page").description("페이지 번호").optional(),
                            parameterWithName("size").description("페이지 크기").optional()
                        ),
                        friendRequestSliceResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("친구 요청 단건 조회에 성공하고 문서화한다")
    fun readByIdSuccess() {
        val (requester, token) = createUserAndToken("req-read@example.com", "req-read")
        val (receiver, _) = createUserAndToken("rec-read@example.com", "rec-read")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "단건 조회용 메시지"))

        mockMvc.perform(
            get("/api/friends/requests/{id}", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.message").value("단건 조회용 메시지"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-read-by-id-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("조회할 친구 요청 식별자")
                        ),
                        friendRequestResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 요청을 조회하면 404 NOT_FOUND를 반환한다")
    fun readByIdFailNotFound() {
        val (_, token) = createUserAndToken("req-read-missing@example.com", "req-read-missing")

        mockMvc.perform(
            get("/api/friends/requests/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-read-by-id-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("참여자가 아닌 사용자가 친구 요청을 조회하면 400 BAD_REQUEST를 반환한다")
    fun readByIdFailBadRequest() {
        val (requester, _) = createUserAndToken("req-read-bad@example.com", "req-read-bad")
        val (receiver, _) = createUserAndToken("rec-read-bad@example.com", "rec-read-bad")
        val (_, otherToken) = createUserAndToken("other-read-bad@example.com", "other-read-bad")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "타인 조회 차단 메시지"))

        mockMvc.perform(
            get("/api/friends/requests/{id}", request.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-read-by-id-fail-bad-request",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("친구 요청을 수락하면 200 OK를 반환한다")
    fun acceptRequest_Success() {
        val (requester, _) = createUserAndToken("req6@example.com", "req6")
        val (receiver, token) = createUserAndToken("rec6@example.com", "rec6")

        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "안녕"))

        mockMvc.perform(
            post("/api/friends/requests/{id}/accept", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-accept-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("수락할 친구 요청 식별자")
                        ),
                        friendshipResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("본인이 받은 요청이 아닌 것을 수락하려 하면 403 FORBIDDEN을 반환한다")
    fun acceptRequest_Fail_NotReceiver() {
        val (requester, token) = createUserAndToken("req7@example.com", "req7")
        val (receiver, _) = createUserAndToken("rec7@example.com", "rec7")

        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "안녕"))

        mockMvc.perform(
            post("/api/friends/requests/{id}/accept", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-accept-fail-bad-request",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 요청을 수락하면 404 NOT_FOUND를 반환한다")
    fun acceptRequestFailNotFound() {
        val (_, token) = createUserAndToken("rec-accept-missing@example.com", "rec-accept-missing")

        mockMvc.perform(
            post("/api/friends/requests/{id}/accept", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-accept-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("친구 요청을 거절하면 200 OK를 반환하고 문서화한다")
    fun rejectRequestSuccess() {
        val (requester, _) = createUserAndToken("req-reject@example.com", "req-reject")
        val (receiver, token) = createUserAndToken("rec-reject@example.com", "rec-reject")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "거절해주세요"))

        mockMvc.perform(
            post("/api/friends/requests/{id}/reject", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-reject-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("거절할 친구 요청 식별자")
                        ),
                        restrictionResponseFields()
                    )
                )
            )
    }

    @Test
    @DisplayName("본인이 받은 요청이 아닌 것을 거절하면 400 BAD_REQUEST를 반환한다")
    fun rejectRequestFailBadRequest() {
        val (requester, token) = createUserAndToken("req-reject-bad@example.com", "req-reject-bad")
        val (receiver, _) = createUserAndToken("rec-reject-bad@example.com", "rec-reject-bad")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "거절 실패 테스트"))

        mockMvc.perform(
            post("/api/friends/requests/{id}/reject", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-reject-fail-bad-request",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 친구 요청을 거절하면 404 NOT_FOUND를 반환한다")
    fun rejectRequestFailNotFound() {
        val (_, token) = createUserAndToken("rec-reject-missing@example.com", "rec-reject-missing")

        mockMvc.perform(
            post("/api/friends/requests/{id}/reject", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-reject-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("받은 친구 요청을 삭제하고 문서화한다")
    fun deleteReceivedRequestSuccess() {
        val (requester, _) = createUserAndToken("req-delete-received@example.com", "req-delete-received")
        val (receiver, token) = createUserAndToken("rec-delete-received@example.com", "rec-delete-received")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "받은 요청 삭제"))

        mockMvc.perform(
            delete("/api/friends/requests/{id}", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-delete-received-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("삭제할 받은 친구 요청 식별자")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 받은 친구 요청을 삭제하면 404 NOT_FOUND를 반환한다")
    fun deleteReceivedRequestFailNotFound() {
        val (_, token) = createUserAndToken("rec-delete-missing@example.com", "rec-delete-missing")

        mockMvc.perform(
            delete("/api/friends/requests/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-delete-received-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("타인의 받은 친구 요청을 삭제하면 403 FORBIDDEN을 반환한다")
    fun deleteReceivedRequestFailForbidden() {
        val (requester, _) = createUserAndToken("req-delete-forbidden@example.com", "req-delete-forbidden")
        val (receiver, _) = createUserAndToken("rec-delete-forbidden@example.com", "rec-delete-forbidden")
        val (_, otherToken) = createUserAndToken("other-delete-forbidden@example.com", "other-delete-forbidden")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "권한 없는 삭제"))

        mockMvc.perform(
            delete("/api/friends/requests/{id}", request.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-delete-received-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("보낸 친구 요청을 취소하고 문서화한다")
    fun cancelSentRequestSuccess() {
        val (requester, token) = createUserAndToken("req-cancel@example.com", "req-cancel")
        val (receiver, _) = createUserAndToken("rec-cancel@example.com", "rec-cancel")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "보낸 요청 취소"))

        mockMvc.perform(
            delete("/api/friends/requests/{id}/sent", request.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-cancel-sent-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("취소할 보낸 친구 요청 식별자")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 보낸 친구 요청을 취소하면 404 NOT_FOUND를 반환한다")
    fun cancelSentRequestFailNotFound() {
        val (_, token) = createUserAndToken("req-cancel-missing@example.com", "req-cancel-missing")

        mockMvc.perform(
            delete("/api/friends/requests/{id}/sent", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-cancel-sent-fail-not-found",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }

    @Test
    @DisplayName("타인의 보낸 친구 요청을 취소하면 403 FORBIDDEN을 반환한다")
    fun cancelSentRequestFailForbidden() {
        val (requester, _) = createUserAndToken("req-cancel-forbidden@example.com", "req-cancel-forbidden")
        val (receiver, _) = createUserAndToken("rec-cancel-forbidden@example.com", "rec-cancel-forbidden")
        val (_, otherToken) = createUserAndToken("other-cancel-forbidden@example.com", "other-cancel-forbidden")
        val request = friendRequestRepository.save(FriendRequest.createWithNullId(requester, receiver, "타인 요청 취소"))

        mockMvc.perform(
            delete("/api/friends/requests/{id}/sent", request.id)
                .header("Authorization", "Bearer $otherToken")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-cancel-sent-fail-forbidden",
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
            delete("/api/friends/requests/admin/{id}", request.id)
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-delete-admin-success",
                    snippets = arrayOf(
                        pathParameters(
                            parameterWithName("id").description("관리자가 삭제할 친구 요청 식별자")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("일반 사용자가 관리자 친구 요청 삭제를 시도하면 403 FORBIDDEN을 반환한다")
    fun deleteByIdAdminFailForbidden() {
        val (_, token) = createUserAndToken("user-delete-friend-request-admin@example.com", "user")

        mockMvc.perform(
            delete("/api/friends/requests/admin/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "friend-request-delete-admin-fail-forbidden",
                    snippets = arrayOf(errorResponseFields())
                )
            )
    }
}
