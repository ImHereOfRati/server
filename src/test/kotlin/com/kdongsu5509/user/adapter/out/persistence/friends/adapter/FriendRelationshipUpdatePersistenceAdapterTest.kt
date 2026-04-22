package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRelationshipMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(
    FriendRelationshipUpdatePersistenceAdapter::class,
    FriendRelationshipMapper::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRelationshipUpdatePersistenceAdapterTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val adapter: FriendRelationshipUpdatePersistenceAdapter,
    private val friendRelationshipRepository: SpringDataFriendRelationshipsRepository
) {

    @Test
    @DisplayName("친구 별칭 수정 성공 테스트")
    fun updateAlias() {
        // given
        val owner = persistUser("owner@test.com", "owner")
        val friend = persistUser("friend@test.com", "friend")
        val relationship = persistRelationship(owner, friend, "oldAlias")

        clearPersistenceContext()
        val newAlias = "newAlias"

        // when
        val result = adapter.updateAlias(owner.email, relationship.id!!, newAlias)

        // then
        assertThat(result.friendAlias).isEqualTo(newAlias)
        val updatedEntity = friendRelationshipRepository.findById(relationship.id!!).get()
        assertThat(updatedEntity.friendAlias).isEqualTo(newAlias)
    }

    @Test
    @DisplayName("친구 관계 삭제 시 양방향 관계가 모두 삭제된다")
    fun delete() {
        // given
        val owner = persistUser("owner@test.com", "owner")
        val friend = persistUser("friend@test.com", "friend")

        val relationshipAtoB = persistRelationship(owner, friend, "friend")
        persistRelationship(friend, owner, "owner")

        clearPersistenceContext()

        // when
        adapter.delete(owner.email, relationshipAtoB.id!!)

        // then
        val remainingRelationships = friendRelationshipRepository.findAll()
        assertThat(remainingRelationships).isEmpty()
    }

    @Test
    @DisplayName("삭제 요청자가 관계의 소유자가 아닐 경우 예외가 발생한다")
    fun delete_ShouldThrowException_WhenUserIsNotOwner() {
        // given
        val owner = persistUser("owner@test.com", "owner")
        val friend = persistUser("friend@test.com", "friend")
        val relationship = persistRelationship(owner, friend, "alias")

        clearPersistenceContext()
        val notOwnerEmail = "not_owner@test.com"

        // when & then
        assertThrows(BusinessException::class.java) {
            adapter.delete(notOwnerEmail, relationship.id!!)
        }
    }

    private fun persistUser(email: String, nickname: String): UserJpaEntity {
        return entityManager.persist(
            UserJpaEntity(
                email = email,
                nickname = nickname,
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
        )
    }

    private fun persistRelationship(
        owner: UserJpaEntity,
        friend: UserJpaEntity,
        alias: String
    ): FriendRelationshipsJpaEntity {
        return entityManager.persist(FriendRelationshipsJpaEntity(owner, friend, alias))
    }

    private fun clearPersistenceContext() {
        entityManager.flush()
        entityManager.clear()
    }
}
