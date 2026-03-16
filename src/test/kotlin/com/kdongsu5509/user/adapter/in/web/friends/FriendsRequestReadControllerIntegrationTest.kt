package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(SpringQueryDSLUserRepository::class, QueryDslConfig::class)
class FriendsRequestReadControllerIntegrationTest {

    private lateinit var requester1: UserJpaEntity
    private lateinit var requester2: UserJpaEntity
    private lateinit var receiver: UserJpaEntity

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: SpringDataUserRepository

    @Autowired
    lateinit var friendRequestRepository: SpringDataFriendRequestRepository

    @BeforeEach
    fun setUp() {
        requester1 = userRepository.save(
            UserJpaEntity("requester1@kakao.com", "요청자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        requester2 = userRepository.save(
            UserJpaEntity("requester2@kakao.com", "요청자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )

        receiver = userRepository.save(
            UserJpaEntity("receiver@kakao.com", "수신자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
    }

    @Test
    @DisplayName("받은 친구 요청 전부를 잘 찾는다.")
    fun getReceivedRequestAll_IntegrationSuccess() {
        //given
        val savedFriendRequest1 = friendRequestRepository.save(
            FriendRequestJpaEntity(
                requester1, receiver, "친하게 지내요"
            )
        )
        val savedFriendRequest2 = friendRequestRepository.save(
            FriendRequestJpaEntity(
                requester2, receiver, "친하게 지내요"
            )
        )

        // when
        val resultActions = mockMvc.perform(
            get("/api/v1/user/friends/request")
                .with(user(receiver.email))
                .with(csrf())
        )

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].friendRequestId").value(savedFriendRequest1.id.toString()))
            .andExpect(jsonPath("$.data[0].requesterEmail").value(savedFriendRequest1.requester.email))
            .andExpect(jsonPath("$.data[0].requesterNickname").value(savedFriendRequest1.requester.nickname))
            .andExpect(jsonPath("$.data[1].friendRequestId").value(savedFriendRequest2.id.toString()))
            .andExpect(jsonPath("$.data[1].requesterEmail").value(savedFriendRequest2.requester.email))
            .andExpect(jsonPath("$.data[1].requesterNickname").value(savedFriendRequest2.requester.nickname))
    }

    @Test
    @DisplayName("받은 친구 요청의 상세 정보를 잘 찾아 반환한다.")
    fun getReceivedRequestDetail_IntegrationSuccess() {
        //given
        val savedFriendRequest1 = friendRequestRepository.save(
            FriendRequestJpaEntity(
                requester1, receiver, "친하게 지내요"
            )
        )

        // when
        val resultActions = mockMvc.perform(
            get("/api/v1/user/friends/request/${savedFriendRequest1.id}")
                .with(csrf())
        )

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.friendRequestId").value(savedFriendRequest1.id.toString()))
            .andExpect(jsonPath("$.data.requesterEmail").value(savedFriendRequest1.requester.email))
            .andExpect(jsonPath("$.data.requesterNickname").value(savedFriendRequest1.requester.nickname))
            .andExpect(jsonPath("$.data.message").exists())
    }
}