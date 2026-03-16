package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRestrictionJpaEntity
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(SpringQueryDSLUserRepository::class, QueryDslConfig::class)
class FriendsRestrictionControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: SpringDataUserRepository,
    private val friendRestrictionRepository: SpringDataFriendRestrictionRepository
) {
    companion object {
        const val TEST_OWNER_EMAIL = "test0@test.com"
        const val START = 1
        const val END = 5
    }

    @BeforeEach
    fun setUp() {
        val owner = userRepository.save(
            UserJpaEntity(
                TEST_OWNER_EMAIL,
                "나",
                UserRole.NORMAL,
                OAuth2Provider.KAKAO,
                UserStatus.ACTIVE
            )
        )

        createTenFriendsAndRestrictions(owner)
    }

    @Test
    @DisplayName("인증된 사용자의 차단/거절 목록을 가져온다")
    @WithMockUser(username = "test0@test.com")
    fun getMyRestrictedFriends_success() {
        mockMvc.perform(
            get("/api/v1/user/friends/restriction")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(END))
            .andExpect(jsonPath("$.data[0].targetEmail").exists())
            .andExpect(jsonPath("$.data[0].restrictionType").value("BLOCK"))
    }

    private fun createTenFriendsAndRestrictions(owner: UserJpaEntity) {
        (START..END).forEach { idx ->
            val friend = userRepository.save(
                createTestFriend(idx)
            )

            friendRestrictionRepository.save(
                createdTestFriendRestriction(owner, friend, idx)
            )
        }
    }

    @Test
    @DisplayName("친구 제한을 삭제하면 삭제된 정보와 함께 201 응답을 받는다")
    @WithMockUser(username = TEST_OWNER_EMAIL)
    fun unrestrict_success() {
        // given
        val restriction = friendRestrictionRepository.findAll()
            .first { it.actor.email == TEST_OWNER_EMAIL }
        val targetId = restriction.id!!
        val targetEmail = restriction.target.email

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/api/v1/user/friends/restriction/$targetId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.data.targetEmail").value(targetEmail))

        val isExists = friendRestrictionRepository.findById(targetId).isPresent
        assertThat(isExists).isFalse()
    }

    @Test
    @DisplayName("본인의 것이 아닌 제한 내역을 삭제하려 하면 예외가 발생한다")
    @WithMockUser(username = "other@test.com")
    fun unrestrict_fail_forbidden() {
        // given -> TEST_OWNER_EMAIL 소유의 데이터
        val restriction = friendRestrictionRepository.findAll()
            .first { it.actor.email == TEST_OWNER_EMAIL }
        val targetId = restriction.id!!

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/api/v1/user/friends/restriction/$targetId")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest) // BusinessException (ErrorCode 확인)
            .andExpect(jsonPath("$.message").exists())
    }

    private fun createTestFriend(idx: Int): UserJpaEntity = UserJpaEntity(
        "friend$idx@test.com",
        "친구$idx",
        UserRole.NORMAL,
        OAuth2Provider.KAKAO,
        UserStatus.ACTIVE
    )

    private fun createdTestFriendRestriction(
        owner: UserJpaEntity,
        friend: UserJpaEntity,
        idx: Int
    ): FriendRestrictionJpaEntity = FriendRestrictionJpaEntity(
        actor = owner,
        target = friend,
        type = if (idx % 2 == 0) FriendRestrictionType.REJECT else FriendRestrictionType.BLOCK
    )
}