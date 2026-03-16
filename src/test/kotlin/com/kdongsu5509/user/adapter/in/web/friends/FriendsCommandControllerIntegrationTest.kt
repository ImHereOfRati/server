package com.kdongsu5509.user.adapter.`in`.web.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(SpringQueryDSLUserRepository::class, QueryDslConfig::class)
class FriendsCommandControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: SpringDataUserRepository

    @Autowired
    lateinit var friendRelationshipsRepository: SpringDataFriendRelationshipsRepository

    @Autowired
    lateinit var friendRestrictionRepository: SpringDataFriendRestrictionRepository

    private lateinit var userA: UserJpaEntity
    private lateinit var userB: UserJpaEntity
    private lateinit var relationshipAtoB: FriendRelationshipsJpaEntity
    private lateinit var relationshipBtoA: FriendRelationshipsJpaEntity

    @BeforeEach
    fun setUp() {
        userA = createUser("userA@test.com", "UserA")
        userB = createUser("userB@test.com", "UserB")

        relationshipAtoB = friendRelationshipsRepository.save(FriendRelationshipsJpaEntity.create(userA, userB))
        relationshipBtoA = friendRelationshipsRepository.save(FriendRelationshipsJpaEntity.create(userB, userA))
    }

    @Test
    @DisplayName("친구 삭제 API 호출 시 양방향 관계가 모두 삭제된다")
    fun deleteFriend_Success() {
        mockMvc.perform(
            delete("/api/v1/user/friends/{friendRelationshipId}", relationshipAtoB.id)
                .with(user(userA.email))
                .with(csrf())
        )
            .andExpect(status().isOk)

        assertThat(friendRelationshipsRepository.findById(relationshipAtoB.id!!)).isEmpty
        assertThat(friendRelationshipsRepository.findById(relationshipBtoA.id!!)).isEmpty
    }

    @Test
    @DisplayName("친구 차단 API 호출 시 관계 삭제 및 차단 목록에 추가된다")
    fun blockFriend_Success() {
        mockMvc.perform(
            post("/api/v1/user/friends/block/{friendRelationshipId}", relationshipAtoB.id)
                .with(user(userA.email))
                .with(csrf())
        )
            .andExpect(status().isOk)

        assertThat(friendRelationshipsRepository.findById(relationshipAtoB.id!!)).isEmpty
        assertThat(friendRelationshipsRepository.findById(relationshipBtoA.id!!)).isEmpty

        val restrictions = friendRestrictionRepository.findByActorId(userA.id!!)
        assertThat(restrictions).hasSize(1)
        assertThat(restrictions[0].target.id).isEqualTo(userB.id)
        assertThat(restrictions[0].type).isEqualTo(FriendRestrictionType.BLOCK)
    }

    @Test
    @DisplayName("친구 별명 변경 API 호출 시 별명이 업데이트된다")
    fun updateFriendAlias_Success() {
        val request = mapOf(
            "friendRelationshipId" to relationshipAtoB.id.toString(),
            "newFriendAlias" to "Bestie"
        )

        mockMvc.perform(
            post("/api/v1/user/friends/alias")
                .with(user(userA.email))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendAlias").value("Bestie"))

        val updated = friendRelationshipsRepository.findById(relationshipAtoB.id!!).get()
        assertThat(updated.friendAlias).isEqualTo("Bestie")
    }

    private fun createUser(email: String, nickname: String) = userRepository.save(
        UserJpaEntity(email, nickname, UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
    )
}
