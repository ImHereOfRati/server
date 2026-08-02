package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.repository.jpa.FriendRelationJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRelationRepository
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class FriendRequestControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Autowired
    private lateinit var relationRepository: SpringDataFriendRelationRepository

    private val message = "친구가 되고 싶습니다"

    @Test
    @DisplayName("친구 요청을 보내면 REQUESTED 관계가 한 행 남는다")
    fun sendRequestPersistsRequestedRelation() {
        // given
        val me = saveUser("me@example.com", "me")
        val target = saveUser("target@example.com", "target")

        // when & then
        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetId" to target.id.toString(), "message" to message))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendRequestId").exists())

        // then: 방향은 initiatedUserId가 들고 있다.
        val saved = relations().single()
        assertThat(saved.status).isEqualTo(FriendRelationStatus.REQUESTED)
        assertThat(saved.initiatedUserId).isEqualTo(me.id)
        assertThat(saved.message).isEqualTo(message)
        assertThat(setOf(saved.lowUserId, saved.highUserId)).isEqualTo(setOf(me.id, target.id))
        assertThat(saved.expiredAt).isNull()
    }

    @Test
    @DisplayName("자기 자신에게 보낸 요청은 400으로 거절된다")
    fun sendRequestToSelfFails() {
        // given
        val me = saveUser("me@example.com", "me")

        // when & then
        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetId" to me.id.toString(), "message" to message))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-000"))

        assertThat(relations()).isEmpty()
    }

    @Test
    @DisplayName("이미 보낸 요청이 있으면 409로 거절된다")
    fun sendRequestTwiceConflicts() {
        // given
        val me = saveUser("me@example.com", "me")
        val target = saveUser("target@example.com", "target")
        saveRelation(me, target, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetId" to target.id.toString(), "message" to message))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-501"))

        assertThat(relations()).hasSize(1)
    }

    @Test
    @DisplayName("이미 친구인 상대에게는 409로 거절된다")
    fun sendRequestToFriendConflicts() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        saveRelation(me, friend, FriendRelationStatus.ACCEPTED)

        // when & then
        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetId" to friend.id.toString(), "message" to message))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-500"))
    }

    @Test
    @DisplayName("차단된 관계에는 422로 거절된다")
    fun sendRequestToBlockedIsUnprocessable() {
        // given: 상대가 나를 차단한 상태여도 요청을 걸 수 없다.
        val me = saveUser("me@example.com", "me")
        val blocker = saveUser("blocker@example.com", "blocker")
        saveRelation(blocker, me, FriendRelationStatus.BLOCKED)

        // when & then
        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("targetId" to blocker.id.toString(), "message" to message))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-700"))
    }

    @Test
    @DisplayName("보낸 요청과 받은 요청은 type으로 갈린다")
    fun findRequestsSplitsByType() {
        // given
        val me = saveUser("me@example.com", "me")
        val receiver = saveUser("receiver@example.com", "receiver")
        val sender = saveUser("sender@example.com", "sender")
        saveRelation(me, receiver, FriendRelationStatus.REQUESTED, message = message)
        saveRelation(sender, me, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(get("/api/friends/requests?type=SENT").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].requester.email").value(me.email))
            .andExpect(jsonPath("$.data.content[0].receiver.email").value(receiver.email))

        mockMvc.perform(get("/api/friends/requests?type=RECEIVED").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].requester.email").value(sender.email))
            .andExpect(jsonPath("$.data.content[0].receiver.email").value(me.email))
    }

    @Test
    @DisplayName("남의 요청을 단건 조회하면 400으로 거절된다")
    fun readOthersRequestFails() {
        // given
        val me = saveUser("me@example.com", "me")
        val one = saveUser("one@example.com", "one")
        val other = saveUser("other@example.com", "other")
        val relation = saveRelation(one, other, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(get("/api/friends/requests/${relation.id}").with(user(principalOf(me))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-002"))
    }

    @Test
    @DisplayName("요청을 수락하면 같은 행이 ACCEPTED로 바뀌고 양쪽 별칭이 채워진다")
    fun acceptTurnsRelationIntoFriendship() {
        // given
        val me = saveUser("me@example.com", "me")
        val sender = saveUser("sender@example.com", "sender")
        val relation = saveRelation(sender, me, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            post("/api/friends/requests/${relation.id}/accept")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.owner.email").value(me.email))
            .andExpect(jsonPath("$.data.friend.email").value(sender.email))
            // 별칭은 상대 닉네임으로 채워진다. 내 칸에는 상대 닉네임이 들어간다.
            .andExpect(jsonPath("$.data.friendAlias").value(sender.nickname))

        // then: 관계는 새로 생기지 않고 같은 행의 상태만 바뀐다.
        val accepted = relations().single()
        assertThat(accepted.id).isEqualTo(relation.id)
        assertThat(accepted.status).isEqualTo(FriendRelationStatus.ACCEPTED)
        assertThat(accepted.message).isNull()
        assertThat(aliasOf(accepted, me)).isEqualTo(sender.nickname)
        assertThat(aliasOf(accepted, sender)).isEqualTo(me.nickname)
    }

    @Test
    @DisplayName("요청을 보낸 쪽이 자기 요청을 수락하면 400으로 거절된다")
    fun acceptOwnRequestFails() {
        // given
        val me = saveUser("me@example.com", "me")
        val receiver = saveUser("receiver@example.com", "receiver")
        val relation = saveRelation(me, receiver, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            post("/api/friends/requests/${relation.id}/accept")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-002"))

        assertThat(relations().single().status).isEqualTo(FriendRelationStatus.REQUESTED)
    }

    @Test
    @DisplayName("요청을 거절하면 제한의 주체가 거절한 쪽으로 뒤집히고 만료 시각이 생긴다")
    fun rejectFlipsDirectionAndSetsExpiry() {
        // given
        val me = saveUser("me@example.com", "me")
        val sender = saveUser("sender@example.com", "sender")
        val relation = saveRelation(sender, me, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            post("/api/friends/requests/${relation.id}/reject")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.restrictor.email").value(me.email))
            .andExpect(jsonPath("$.data.restricted.email").value(sender.email))
            .andExpect(jsonPath("$.data.type").value("REJECTED"))

        val rejected = relations().single()
        assertThat(rejected.status).isEqualTo(FriendRelationStatus.REJECTED)
        assertThat(rejected.initiatedUserId).isEqualTo(me.id)
        assertThat(rejected.message).isNull()
        assertThat(rejected.expiredAt).isAfter(LocalDateTime.now())
    }

    @Test
    @DisplayName("받은 요청을 삭제하면 제한 기록 없이 행이 사라진다")
    fun deleteReceivedRequestRemovesRow() {
        // given
        val me = saveUser("me@example.com", "me")
        val sender = saveUser("sender@example.com", "sender")
        val relation = saveRelation(sender, me, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            delete("/api/friends/requests/${relation.id}")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)

        assertThat(relations()).isEmpty()
    }

    @Test
    @DisplayName("보낸 요청을 취소하면 행이 사라진다")
    fun cancelSentRequestRemovesRow() {
        // given
        val me = saveUser("me@example.com", "me")
        val receiver = saveUser("receiver@example.com", "receiver")
        val relation = saveRelation(me, receiver, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            delete("/api/friends/requests/${relation.id}/sent")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)

        assertThat(relations()).isEmpty()
    }

    @Test
    @DisplayName("받은 쪽이 보낸 요청 취소를 호출하면 403으로 거절된다")
    fun cancelSentRequestByReceiverForbidden() {
        // given
        val me = saveUser("me@example.com", "me")
        val sender = saveUser("sender@example.com", "sender")
        val relation = saveRelation(sender, me, FriendRelationStatus.REQUESTED, message = message)

        // when & then
        mockMvc.perform(
            delete("/api/friends/requests/${relation.id}/sent")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-200"))

        assertThat(relations()).hasSize(1)
    }

    private fun aliasOf(relation: FriendRelationJpaEntity, user: UserJpaEntity): String? =
        if (relation.lowUserId == user.id) relation.lowAlias else relation.highAlias

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
        message: String? = null,
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
                message = message,
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
