package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@Import(
    AdminFriendPersistenceAdapter::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class AdminFriendPersistenceAdapterTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val adapter: AdminFriendPersistenceAdapter,
    private val relationshipsRepository: SpringDataFriendRelationshipsRepository,
    private val requestRepository: SpringDataFriendRequestRepository
) {

    @Test
    @DisplayName("관리자가 두 유저의 친구 관계를 양방향 모두 강제 삭제한다")
    fun forceClearFriendRelationship_deletesBothDirections() {
        // given
        val userA = persistUser("userA@kakao.com", "유저A")
        val userB = persistUser("userB@kakao.com", "유저B")
        persistRelationship(userA, userB, "B의별명")
        persistRelationship(userB, userA, "A의별명")
        flush()

        // when
        adapter.forceClearFriendRelationship(userA.email, userB.email)

        // then
        assertThat(relationshipsRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("친구 관계가 없어도 예외 없이 정상 종료된다")
    fun forceClearFriendRelationship_noRelationship_noException() {
        // given
        val userA = persistUser("userA@kakao.com", "유저A")
        val userB = persistUser("userB@kakao.com", "유저B")
        flush()

        // when & then (no exception)
        adapter.forceClearFriendRelationship(userA.email, userB.email)
    }

    @Test
    @DisplayName("관리자가 특정 친구 요청을 강제 삭제한다")
    fun forceClearFriendRequest_deletesRequest() {
        // given
        val requester = persistUser("requester@kakao.com", "요청자")
        val receiver = persistUser("receiver@kakao.com", "수신자")
        persistRequest(requester, receiver)
        flush()

        // when
        adapter.forceClearFriendRequest(requester.email, receiver.email)

        // then
        assertThat(requestRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("친구 요청이 없어도 예외 없이 정상 종료된다")
    fun forceClearFriendRequest_noRequest_noException() {
        // given
        val requester = persistUser("requester@kakao.com", "요청자")
        val receiver = persistUser("receiver@kakao.com", "수신자")
        flush()

        // when & then (no exception)
        adapter.forceClearFriendRequest(requester.email, receiver.email)
    }

    private fun persistUser(email: String, nickname: String): UserJpaEntity =
        entityManager.persist(
            UserJpaEntity(
                email = email,
                nickname = nickname,
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
        )

    private fun persistRelationship(owner: UserJpaEntity, friend: UserJpaEntity, alias: String) =
        entityManager.persist(FriendRelationshipsJpaEntity(owner, friend, alias))

    private fun persistRequest(requester: UserJpaEntity, receiver: UserJpaEntity) =
        entityManager.persist(FriendRequestJpaEntity(requester, receiver, "친구해요"))

    private fun flush() {
        entityManager.flush()
        entityManager.clear()
    }
}
