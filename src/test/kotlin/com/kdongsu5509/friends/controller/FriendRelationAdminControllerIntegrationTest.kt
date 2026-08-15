package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.ResourceSnippetParameters
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
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class FriendRelationAdminControllerIntegrationTest : WebIntegrationTestSupport() {

    private fun documentation(identifier: String) =
        MockMvcRestDocumentationWrapper.document(
            identifier,
            ResourceSnippetParameters.builder().description("관리자 친구 관계 성공·실패 케이스")
        )

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Autowired
    private lateinit var relationRepository: SpringDataFriendRelationRepository

    private val admin = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = UserRole.ADMIN.name,
        status = UserStatus.ACTIVE.name
    )

    @Test
    @DisplayName("요청 목록에는 REQUESTED 관계만 담긴다")
    fun findAllRequestsReturnsOnlyRequested() {
        // given
        val fixture = saveAllStatuses()

        // when & then
        mockMvc.perform(get("/api/admin/friend-requests").with(user(admin)))
            .andExpect(status().isOk)
            .andDo(documentation("admin-friend-requests-success"))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].requester.email").value(fixture.requester.email))
            .andExpect(jsonPath("$.data.content[0].receiver.email").value(fixture.receiver.email))
            .andExpect(jsonPath("$.data.content[0].message").value("친구가 되고 싶습니다"))
    }

    @Test
    @DisplayName("친구 목록에는 ACCEPTED 관계만 담긴다")
    fun findAllFriendshipsReturnsOnlyAccepted() {
        // given
        val fixture = saveAllStatuses()

        // when & then: 관리자 목록은 특정 관점이 없어 쌍의 정렬 순서를 그대로 쓴다.
        mockMvc.perform(get("/api/admin/friendships").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(
                jsonPath("$.data.content[0].owner.email")
                    .value(anyOf(equalTo(fixture.friendOne.email), equalTo(fixture.friendTwo.email)))
            )
            .andExpect(
                jsonPath("$.data.content[0].friend.email")
                    .value(anyOf(equalTo(fixture.friendOne.email), equalTo(fixture.friendTwo.email)))
            )
            .andExpect(jsonPath("$.data.content[0].friendAlias").value("베프"))
    }

    @Test
    @DisplayName("제한 목록에는 REJECTED와 BLOCKED 관계가 담긴다")
    fun findAllRestrictionsReturnsRejectedAndBlocked() {
        // given
        saveAllStatuses()

        // when & then
        mockMvc.perform(get("/api/admin/friend-restrictions").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[*].type").value(containsInAnyOrder("REJECTED", "BLOCKED")))
    }

    @Test
    @DisplayName("관리자는 요청·친구·제한을 식별자로 지운다")
    fun deleteByIdRemovesRelationRegardlessOfStatus() {
        // given
        val fixture = saveAllStatuses()

        // when & then: 세 경로 모두 같은 삭제 명령을 쓰지만 화면 계약은 따로 유지된다.
        mockMvc.perform(
            delete("/api/admin/friend-requests/${fixture.requested.id}").with(user(admin)).with(csrf())
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            delete("/api/admin/friendships/${fixture.accepted.id}").with(user(admin)).with(csrf())
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            delete("/api/admin/friend-restrictions/${fixture.blocked.id}").with(user(admin)).with(csrf())
        ).andExpect(status().isNoContent)

        relationRepository.flush()
        assertThat(relationRepository.findAll().map { it.id })
            .containsExactly(fixture.rejected.id)
    }

    @Test
    @DisplayName("일반 사용자는 관리자 목록에 접근할 수 없다")
    fun normalUserCannotReadAdminList() {
        // given
        val normal = saveUser("normal@example.com", "normal")

        // when & then
        mockMvc.perform(get("/api/admin/friend-requests").with(user(principalOf(normal))))
            .andExpect(status().isForbidden)
            .andDo(documentation("admin-friend-requests-forbidden"))
    }

    @Test
    @DisplayName("인증되지 않은 요청은 관리자 목록에 접근할 수 없다")
    fun anonymousCannotReadAdminList() {
        mockMvc.perform(get("/api/admin/friend-requests"))
            .andExpect(status().isUnauthorized)
    }

    private fun saveAllStatuses(): Fixture {
        val requester = saveUser("requester@example.com", "requester")
        val receiver = saveUser("receiver@example.com", "receiver")
        val friendOne = saveUser("friend-one@example.com", "friendOne")
        val friendTwo = saveUser("friend-two@example.com", "friendTwo")
        val rejecter = saveUser("rejecter@example.com", "rejecter")
        val rejected = saveUser("rejected@example.com", "rejected")
        val blocker = saveUser("blocker@example.com", "blocker")
        val blocked = saveUser("blocked@example.com", "blocked")

        return Fixture(
            requester = requester,
            receiver = receiver,
            friendOne = friendOne,
            friendTwo = friendTwo,
            requested = saveRelation(
                requester, receiver, FriendRelationStatus.REQUESTED,
                message = "친구가 되고 싶습니다"
            ),
            // 관리자 목록은 쌍의 정렬 순서를 쓰므로 양쪽 별칭을 같게 둬 정렬에 의존하지 않는다.
            accepted = saveRelation(
                friendOne, friendTwo, FriendRelationStatus.ACCEPTED,
                initiatorAlias = "베프", targetAlias = "베프"
            ),
            rejected = saveRelation(
                rejecter, rejected, FriendRelationStatus.REJECTED,
                expiredAt = LocalDateTime.now().plusMonths(1)
            ),
            blocked = saveRelation(
                blocker, blocked, FriendRelationStatus.BLOCKED,
                expiredAt = FriendRelation.PERMANENT
            )
        )
    }

    private data class Fixture(
        val requester: UserJpaEntity,
        val receiver: UserJpaEntity,
        val friendOne: UserJpaEntity,
        val friendTwo: UserJpaEntity,
        val requested: FriendRelationJpaEntity,
        val accepted: FriendRelationJpaEntity,
        val rejected: FriendRelationJpaEntity,
        val blocked: FriendRelationJpaEntity
    )

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
                message = message,
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
