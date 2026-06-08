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
                    identifier = "friend-request-fail-already-requested"
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
                    identifier = "friend-request-fail-already-friend"
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
                    identifier = "friend-request-fail-restricted"
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
            .andDo(MockMvcRestDocumentationWrapper.document("friend-request-accept-success"))
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
            .andDo(MockMvcRestDocumentationWrapper.document("friend-request-accept-fail-forbidden"))
    }
}
