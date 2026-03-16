package com.kdongsu5509.user.adapter.`in`.web.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(SpringQueryDSLUserRepository::class, QueryDslConfig::class)
class FriendsRequestCommandControllerIntegrationTest {

    companion object {
        const val BASE_URL = "/api/v1/user/friends/request"
        const val FRIEND_REQUEST_MESSAGE = "안녕하세요 저는 라티입니다! 친하게 지내요!"
        const val REQ_EMAIL = "requester1@kakao.com"
    }

    private lateinit var requester1: UserJpaEntity
    private lateinit var receiver: UserJpaEntity

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: SpringDataUserRepository

    @Autowired
    lateinit var friendRequestRepository: SpringDataFriendRequestRepository

    @BeforeEach
    fun setUp() {
        requester1 = createUser(REQ_EMAIL, "요청자")
        receiver = createUser("receiver@kakao.com", "수신자")
    }

    @Test
    @WithMockUser(REQ_EMAIL)
    @DisplayName("친구 요청 API 호출 시 DB 저장까지 성공한다")
    fun requestFriendship_IntegrationSuccess() {
        val requestDto = mapOf(
            "receiverId" to receiver.id,
            "message" to FRIEND_REQUEST_MESSAGE
        )

        performPost(BASE_URL, requester1.email, requestDto)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.friendRequestId").exists())

        val savedRequests = friendRequestRepository.findAll()
        assertThat(savedRequests).hasSize(1)
            .first()
            .extracting("requester.id", "receiver.id")
            .containsExactly(requester1.id, receiver.id)
    }

    @Test
    @DisplayName("친구 요청 수락 API 호출 시 친구 요청은 DB 삭제, 친구는 DB 저장까지 성공한다")
    fun acceptToFriendRequest() {
        val requestId = createFriendRequest(requester1, receiver)

        performPost("$BASE_URL/accept/$requestId", receiver.email)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendEmail").value(requester1.email))
    }

    @Test
    @DisplayName("친구 요청 거절 API 호출 시 친구 요청은 DB 삭제, 친구는 거절 DB에 저장까지 성공한다")
    fun rejectToFriendRequest() {
        val requestId = createFriendRequest(requester1, receiver)

        performPost("$BASE_URL/reject/$requestId", receiver.email)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.targetEmail").value(requester1.email))
    }

    private fun createUser(email: String, nickname: String) = userRepository.save(
        UserJpaEntity(email, nickname, UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
    )

    private fun createFriendRequest(from: UserJpaEntity, to: UserJpaEntity): Long? {
        return friendRequestRepository.save(FriendRequestJpaEntity(from, to, FRIEND_REQUEST_MESSAGE)).id
    }

    private fun performPost(url: String, userEmail: String, content: Any? = null) = mockMvc.perform(
        post(url)
            .with(user(userEmail))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .apply { if (content != null) content(objectMapper.writeValueAsString(content)) }
    )
}