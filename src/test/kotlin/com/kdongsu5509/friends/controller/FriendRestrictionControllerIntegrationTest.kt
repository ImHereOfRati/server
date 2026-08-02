package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.repository.jpa.FriendRelationJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRelationRepository
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*

class FriendRestrictionControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Autowired
    private lateinit var relationRepository: SpringDataFriendRelationRepository

    @Test
    @DisplayName("관계가 없는 상대도 차단할 수 있고 만료되지 않는다")
    fun blockWithoutRelationCreatesPermanentBlock() {
        // given
        val me = saveUser("me@example.com", "me")
        val target = saveUser("target@example.com", "target")

        // when & then
        mockMvc.perform(
            post("/api/friends/restrictions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetUserId" to target.id.toString()))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.type").value("BLOCKED"))
            .andExpect(jsonPath("$.data.restrictor.email").value(me.email))
            .andExpect(jsonPath("$.data.restricted.email").value(target.email))

        val blocked = relations().single()
        assertThat(blocked.status).isEqualTo(FriendRelationStatus.BLOCKED)
        assertThat(blocked.initiatedUserId).isEqualTo(me.id)
        assertThat(blocked.expiredAt).isEqualTo(FriendRelation.PERMANENT)
    }

    @Test
    @DisplayName("친구를 차단하면 같은 행이 BLOCKED로 바뀌고 별칭이 지워진다")
    fun blockFriendFlipsRelationAndClearsAliases() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val relation = saveRelation(
            me, friend, FriendRelationStatus.ACCEPTED,
            initiatorAlias = "내별칭", targetAlias = "상대별칭"
        )

        // when & then
        mockMvc.perform(
            post("/api/friends/restrictions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetUserId" to friend.id.toString()))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.type").value("BLOCKED"))

        val blocked = relations().single()
        assertThat(blocked.id).isEqualTo(relation.id)
        assertThat(blocked.status).isEqualTo(FriendRelationStatus.BLOCKED)
        assertThat(blocked.initiatedUserId).isEqualTo(me.id)
        assertThat(blocked.lowAlias).isNull()
        assertThat(blocked.highAlias).isNull()
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    fun blockSelfFails() {
        // given
        val me = saveUser("me@example.com", "me")

        // when & then
        mockMvc.perform(
            post("/api/friends/restrictions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetUserId" to me.id.toString()))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-007"))

        assertThat(relations()).isEmpty()
    }

    @Test
    @DisplayName("없는 사용자를 차단하면 404로 거절된다")
    fun blockUnknownTargetFails() {
        // given
        val me = saveUser("me@example.com", "me")

        // when & then
        mockMvc.perform(
            post("/api/friends/restrictions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetUserId" to UUID.randomUUID().toString()))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-301"))
    }

    @Test
    @DisplayName("제한 목록에는 내가 건 차단과 거절만 담긴다")
    fun findAllReturnsOnlyRestrictionsIInitiated() {
        // given
        val me = saveUser("me@example.com", "me")
        val blockedByMe = saveUser("blocked@example.com", "blocked")
        val rejectedByMe = saveUser("rejected@example.com", "rejected")
        val blockerOfMe = saveUser("blocker@example.com", "blocker")
        val friend = saveUser("friend@example.com", "friend")

        saveRelation(me, blockedByMe, FriendRelationStatus.BLOCKED, expiredAt = FriendRelation.PERMANENT)
        saveRelation(me, rejectedByMe, FriendRelationStatus.REJECTED, expiredAt = LocalDateTime.now().plusMonths(1))
        saveRelation(blockerOfMe, me, FriendRelationStatus.BLOCKED, expiredAt = FriendRelation.PERMANENT)
        saveRelation(me, friend, FriendRelationStatus.ACCEPTED)

        // when & then
        mockMvc.perform(get("/api/friends/restrictions").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[*].restrictor.email").value(containsInAnyOrder(me.email, me.email)))
            .andExpect(
                jsonPath("$.data.content[*].restricted.email")
                    .value(containsInAnyOrder(blockedByMe.email, rejectedByMe.email))
            )
    }

    @Test
    @DisplayName("제한 여부 조회는 내가 건 만료 전 제한만 true로 본다")
    fun checkRestrictionStatusIgnoresExpiredAndInboundRestrictions() {
        // given
        val me = saveUser("me@example.com", "me")
        val blockedByMe = saveUser("blocked@example.com", "blocked")
        val expiredTarget = saveUser("expired@example.com", "expired")
        val blockerOfMe = saveUser("blocker@example.com", "blocker")

        saveRelation(me, blockedByMe, FriendRelationStatus.BLOCKED, expiredAt = FriendRelation.PERMANENT)
        saveRelation(me, expiredTarget, FriendRelationStatus.REJECTED, expiredAt = LocalDateTime.now().minusDays(1))
        saveRelation(blockerOfMe, me, FriendRelationStatus.BLOCKED, expiredAt = FriendRelation.PERMANENT)

        // when & then
        mockMvc.perform(
            get("/api/friends/restrictions/target/${blockedByMe.id}").with(user(principalOf(me)))
        ).andExpect(status().isOk).andExpect(jsonPath("$.data").value(true))

        mockMvc.perform(
            get("/api/friends/restrictions/target/${expiredTarget.id}").with(user(principalOf(me)))
        ).andExpect(status().isOk).andExpect(jsonPath("$.data").value(false))

        // 내가 당한 차단은 내가 건 제한이 아니므로 false다.
        mockMvc.perform(
            get("/api/friends/restrictions/target/${blockerOfMe.id}").with(user(principalOf(me)))
        ).andExpect(status().isOk).andExpect(jsonPath("$.data").value(false))

        mockMvc.perform(
            get("/api/friends/restrictions/target/${me.id}").with(user(principalOf(me)))
        ).andExpect(status().isOk).andExpect(jsonPath("$.data").value(false))
    }

    @Test
    @DisplayName("차단을 해제하면 관계 행이 사라진다")
    fun unblockRemovesRelation() {
        // given
        val me = saveUser("me@example.com", "me")
        val target = saveUser("target@example.com", "target")
        saveRelation(me, target, FriendRelationStatus.BLOCKED, expiredAt = FriendRelation.PERMANENT)

        // when & then
        mockMvc.perform(
            delete("/api/friends/restrictions/blocked-users/${target.id}")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)

        assertThat(relations()).isEmpty()
    }

    @Test
    @DisplayName("차단을 건 쪽이 아니면 해제할 수 없다")
    fun unblockByNonBlockerForbidden() {
        // given
        val me = saveUser("me@example.com", "me")
        val blocker = saveUser("blocker@example.com", "blocker")
        saveRelation(blocker, me, FriendRelationStatus.BLOCKED, expiredAt = FriendRelation.PERMANENT)

        // when & then
        mockMvc.perform(
            delete("/api/friends/restrictions/blocked-users/${blocker.id}")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-200"))

        assertThat(relations()).hasSize(1)
    }

    @Test
    @DisplayName("차단 상태가 아닌 관계는 해제할 수 없다")
    fun unblockNonBlockedRelationFails() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        saveRelation(me, friend, FriendRelationStatus.ACCEPTED)

        // when & then
        mockMvc.perform(
            delete("/api/friends/restrictions/blocked-users/${friend.id}")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-008"))

        assertThat(relations()).hasSize(1)
    }

    @Test
    @DisplayName("관계가 없는 상대의 차단 해제는 아무 일도 하지 않는다")
    fun unblockWithoutRelationIsNoop() {
        // given
        val me = saveUser("me@example.com", "me")
        val stranger = saveUser("stranger@example.com", "stranger")

        // when & then
        mockMvc.perform(
            delete("/api/friends/restrictions/blocked-users/${stranger.id}")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)

        assertThat(relations()).isEmpty()
    }

    private fun hasOnly(vararg values: String) =
        org.hamcrest.Matchers.containsInAnyOrder(*values.map { org.hamcrest.Matchers.equalTo(it) }.toTypedArray())

    private fun relations(): List<FriendRelationJpaEntity> {
        relationRepository.flush()
        return relationRepository.findAll()
    }

    private fun json(vararg pairs: Pair<String, Any?>): String = jsonMapper.writeValueAsString(mapOf(*pairs))

    private fun saveUser(email: String, nickname: String): UserJpaEntity =
        userRepository.save(
            UserJpaEntity(
                email = email,
                nickname = nickname,
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
        )

    private fun saveRelation(
        initiator: UserJpaEntity,
        target: UserJpaEntity,
        status: FriendRelationStatus,
        initiatorAlias: String? = null,
        targetAlias: String? = null,
        expiredAt: LocalDateTime? = null
    ): FriendRelationJpaEntity {
        val initiatorId = initiator.id!!
        val targetId = target.id!!
        val initiatorIsLow = initiatorId < targetId

        return relationRepository.save(
            FriendRelationJpaEntity(
                lowUserId = if (initiatorIsLow) initiatorId else targetId,
                highUserId = if (initiatorIsLow) targetId else initiatorId,
                status = status,
                initiatedUserId = initiatorId,
                lowAlias = if (initiatorIsLow) initiatorAlias else targetAlias,
                highAlias = if (initiatorIsLow) targetAlias else initiatorAlias,
                expiredAt = expiredAt
            )
        )
    }

    private fun principalOf(entity: UserJpaEntity) = ImHereUserDetails(
        email = entity.email,
        nickname = entity.nickname,
        role = entity.role.name,
        status = entity.status.name,
        userId = entity.id
    )
}
