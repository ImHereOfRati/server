package com.kdongsu5509.imhereuserservice.adapter.`in`.web.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FriendsRequestIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: SpringDataUserRepository,
    private val friendRequestRepository: SpringDataFriendRequestRepository
) {

    private lateinit var requester: UserJpaEntity
    private lateinit var receiver: UserJpaEntity

    @BeforeEach
    fun setUp() {
        requester = userRepository.save(
            UserJpaEntity("requester@kakao.com", "요청자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        receiver = userRepository.save(
            UserJpaEntity("receiver@kakao.com", "수신자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
    }

    @Test
    @DisplayName("친구 요청 API 호출 시 DB 저장까지 성공한다")
    fun requestFriendship_IntegrationSuccess() {
        // given
        val message = "친하게 지내요!"
        val requestDto = mapOf(
            "receiverId" to receiver.id.toString(),
            "message" to message
        )

        // when
        val resultActions = mockMvc.perform(
            post("/api/v1/user/friends/requests")
                .with(user(requester.email))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.friendRequestId").exists())

        val savedRequests = friendRequestRepository.findAll()
        assertThat(savedRequests).hasSize(1)

        val savedRequest = savedRequests[0]
        assertThat(savedRequest.requester.id).isEqualTo(requester.id)
        assertThat(savedRequest.receiver.id).isEqualTo(receiver.id)
        assertThat(savedRequest.message).isEqualTo(message)
    }
}