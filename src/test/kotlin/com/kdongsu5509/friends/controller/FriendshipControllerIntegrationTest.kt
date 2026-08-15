package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.ResourceSnippetParameters
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

class FriendshipControllerIntegrationTest : WebIntegrationTestSupport() {

    private fun documentation(identifier: String) =
        MockMvcRestDocumentationWrapper.document(
            identifier,
            ResourceSnippetParameters.builder().description("친구 관계 성공·실패 케이스")
        )

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Autowired
    private lateinit var relationRepository: SpringDataFriendRelationRepository

    @Test
    @DisplayName("친구 목록에는 ACCEPTED 관계만 담긴다")
    fun readAllReturnsOnlyAcceptedRelations() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val requester = saveUser("requester@example.com", "requester")
        saveRelation(me, friend, FriendRelationStatus.ACCEPTED, myAlias = "친구", counterpartAlias = "나")
        saveRelation(requester, me, FriendRelationStatus.REQUESTED, message = "친구가 되고 싶습니다")

        // when & then
        mockMvc.perform(get("/api/friendships").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andDo(documentation("friendships-read-success"))
            .andExpect(jsonPath("$.data.content[0].owner.email").value(me.email))
            .andExpect(jsonPath("$.data.content[0].friend.email").value(friend.email))
            .andExpect(jsonPath("$.data.content[0].friendAlias").value("친구"))
    }

    @Test
    @DisplayName("친구 여부 조회는 ACCEPTED일 때만 true다")
    fun checkFriendStatusReflectsAcceptedOnly() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val requester = saveUser("requester@example.com", "requester")
        saveRelation(me, friend, FriendRelationStatus.ACCEPTED)
        saveRelation(requester, me, FriendRelationStatus.REQUESTED, message = "친구가 되고 싶습니다")

        // when & then
        mockMvc.perform(get("/api/friendships/target/${friend.id}").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))

        mockMvc.perform(get("/api/friendships/target/${requester.id}").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(false))

        // 자기 자신은 친구가 될 수 없으므로 관계를 찾지 않고 false를 준다.
        mockMvc.perform(get("/api/friendships/target/${me.id}").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(false))
    }

    @Test
    @DisplayName("친구 단건 조회는 보는 사람을 owner로 세운다")
    fun readByIdPutsViewerAsOwner() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val relation = saveRelation(
            me, friend, FriendRelationStatus.ACCEPTED,
            myAlias = "내별칭", counterpartAlias = "상대별칭"
        )

        // when & then: 같은 행을 상대가 조회하면 owner/friend가 뒤집힌다.
        mockMvc.perform(get("/api/friendships/${relation.id}").with(user(principalOf(me))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.owner.email").value(me.email))
            .andExpect(jsonPath("$.data.friend.email").value(friend.email))
            .andExpect(jsonPath("$.data.friendAlias").value("내별칭"))

        mockMvc.perform(get("/api/friendships/${relation.id}").with(user(principalOf(friend))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.owner.email").value(friend.email))
            .andExpect(jsonPath("$.data.friend.email").value(me.email))
            .andExpect(jsonPath("$.data.friendAlias").value("상대별칭"))
    }

    @Test
    @DisplayName("남의 친구 관계를 조회하면 403으로 거절된다")
    fun readOthersFriendshipForbidden() {
        // given
        val me = saveUser("me@example.com", "me")
        val one = saveUser("one@example.com", "one")
        val other = saveUser("other@example.com", "other")
        val relation = saveRelation(one, other, FriendRelationStatus.ACCEPTED)

        // when & then
        mockMvc.perform(get("/api/friendships/${relation.id}").with(user(principalOf(me))))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-200"))
            .andDo(documentation("friendship-detail-forbidden"))
    }

    @Test
    @DisplayName("별칭 수정은 내 칸만 바꾼다")
    fun updateAliasTouchesOnlyMySlot() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val relation = saveRelation(
            me, friend, FriendRelationStatus.ACCEPTED,
            myAlias = "내별칭", counterpartAlias = "상대별칭"
        )

        // when & then
        mockMvc.perform(
            patch("/api/friendships/${relation.id}/alias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("alias" to "새별칭"))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendAlias").value("새별칭"))

        val updated = reload(relation)
        assertThat(aliasOf(updated, me)).isEqualTo("새별칭")
        assertThat(aliasOf(updated, friend)).isEqualTo("상대별칭")
    }

    @Test
    @DisplayName("10자를 넘는 별칭은 400으로 거절된다")
    fun updateAliasTooLongFails() {
        // given: 요청 DTO는 20자까지 허용하지만 도메인 규칙은 10자다.
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val relation = saveRelation(me, friend, FriendRelationStatus.ACCEPTED, myAlias = "내별칭")

        // when & then
        mockMvc.perform(
            patch("/api/friendships/${relation.id}/alias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("alias" to "0123456789A"))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-001"))
            .andDo(documentation("friendship-alias-bad-request"))

        assertThat(aliasOf(reload(relation), me)).isEqualTo("내별칭")
    }

    @Test
    @DisplayName("아직 친구가 아닌 관계의 별칭을 바꾸면 404로 거절된다")
    fun updateAliasOnNonAcceptedRelationFails() {
        // given
        val me = saveUser("me@example.com", "me")
        val requester = saveUser("requester@example.com", "requester")
        val relation = saveRelation(
            requester, me, FriendRelationStatus.REQUESTED,
            message = "친구가 되고 싶습니다"
        )

        // when & then
        mockMvc.perform(
            patch("/api/friendships/${relation.id}/alias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("alias" to "새별칭"))
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.imhereResponseCode").value("FRIEND-300"))
    }

    @Test
    @DisplayName("친구 삭제는 204를 주고 행을 지운다")
    fun deleteFriendshipRemovesRow() {
        // given
        val me = saveUser("me@example.com", "me")
        val friend = saveUser("friend@example.com", "friend")
        val relation = saveRelation(me, friend, FriendRelationStatus.ACCEPTED)

        // when & then
        mockMvc.perform(
            delete("/api/friendships/${relation.id}")
                .with(user(principalOf(me)))
                .with(csrf())
        ).andExpect(status().isNoContent)
            .andDo(documentation("friendship-delete-success"))

        relationRepository.flush()
        assertThat(relationRepository.findAll()).isEmpty()
    }

    private fun reload(relation: FriendRelationJpaEntity): FriendRelationJpaEntity {
        relationRepository.flush()
        return relationRepository.findById(relation.id!!).orElseThrow()
    }

    private fun aliasOf(relation: FriendRelationJpaEntity, user: UserJpaEntity): String? =
        if (relation.lowUserId == user.id) relation.lowAlias else relation.highAlias

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
        myAlias: String? = null,
        counterpartAlias: String? = null
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
                lowAlias = if (initiatorIsLow) myAlias else counterpartAlias,
                highAlias = if (initiatorIsLow) counterpartAlias else myAlias
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
