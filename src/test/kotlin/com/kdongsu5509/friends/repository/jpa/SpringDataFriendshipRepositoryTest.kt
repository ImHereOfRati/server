package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
class SpringDataFriendshipRepositoryTest @Autowired constructor(
    private val em: EntityManager,
    private val repository: SpringDataFriendshipRepository
) {

    @Test
    @DisplayName("소유자 Email과 페이징 정보로 친구 관계 슬라이스를 조회한다")
    fun findByOwnerUserEmail_slice_success() {
        // given
        val owner = createUser("owner@test.com", "owner")
        val friend1 = createUser("friend1@test.com", "friend1")
        val friend2 = createUser("friend2@test.com", "friend2")

        em.persist(FriendshipJpaEntity.create(owner, friend1, "alias1"))
        em.persist(FriendshipJpaEntity.create(owner, friend2, "alias2"))
        em.flush()

        val pageable = PageRequest.of(0, 1) // page 0, size 1

        // when
        val result = repository.findByOwnerUserEmail(owner.email, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.hasNext()).isTrue() // 다음 페이지 존재 여부 확인
    }

    @Test
    @DisplayName("소유자 Email과 친구 ID로 친구 관계를 단건 조회한다")
    fun findByOwnerUserEmailAndFriendUserId_success() {
        // given
        val owner = createUser("owner@test.com", "owner")
        val friend = createUser("friend@test.com", "friend")

        val relationship = FriendshipJpaEntity.create(owner, friend, "alias")
        em.persist(relationship)
        em.flush()

        // when
        val result = repository.findByOwnerUserEmailAndFriendUserId(owner.email, friend.id!!)

        // then
        assertThat(result).isPresent
        assertThat(result.get().friendAlias).isEqualTo("alias")
    }

    @Test
    @DisplayName("소유자 Email과 친구 Email로 친구 관계를 단건 조회한다")
    fun findByOwnerUserEmailAndFriendUserEmail_success() {
        // given
        val owner = createUser("owner@test.com", "owner")
        val friend = createUser("friend@test.com", "friend")

        val relationship = FriendshipJpaEntity.create(owner, friend, "alias")
        em.persist(relationship)
        em.flush()

        // when
        val result = repository.findByOwnerUserEmailAndFriendUserEmail(owner.email, friend.email)

        // then
        assertThat(result).isPresent
        assertThat(result.get().friendAlias).isEqualTo("alias")
    }

    @Test
    @DisplayName("소유자 Email과 친구 Email로 친구 관계를 삭제한다")
    fun deleteByOwnerUserEmailAndFriendUserEmail_success() {
        // given
        val owner = createUser("owner@test.com", "owner")
        val friend = createUser("friend@test.com", "friend")

        val relationship = FriendshipJpaEntity.create(owner, friend, "alias")
        em.persist(relationship)
        em.flush()

        // when
        repository.deleteByOwnerUserEmailAndFriendUserEmail(owner.email, friend.email)
        em.flush()
        em.clear()

        // then
        val result = repository.findByOwnerUserEmailAndFriendUserEmail(owner.email, friend.email)
        assertThat(result).isEmpty
    }

    @Test
    @DisplayName("양방향 친구 관계를 동시에 모두 삭제한다")
    fun deleteBoth_success() {
        // given
        val owner = createUser("owner@test.com", "owner")
        val friend = createUser("friend@test.com", "friend")

        em.persist(FriendshipJpaEntity.create(owner, friend, "friend1"))
        em.persist(FriendshipJpaEntity.create(friend, owner, "friend2"))
        em.flush()

        // when
        repository.deleteBoth(owner.id!!, friend.id!!)
        em.flush()
        em.clear()

        // then
        val check1 = repository.findByOwnerUserEmailAndFriendUserId(owner.email, friend.id!!)
        val check2 = repository.findByOwnerUserEmailAndFriendUserId(friend.email, owner.id!!)
        assertThat(check1).isEmpty
        assertThat(check2).isEmpty
    }

    @Nested
    inner class ExistTest {
        @Test
        @DisplayName("소유자 아이디와 친구 아이디를 기반으로 존재 하면 true을 반환한다")
        fun existsByIds() {
            // given
            val owner = createUser("owner@test.com", "owner")
            val friend = createUser("friend@test.com", "friend")
            em.persist(FriendshipJpaEntity.create(owner, friend, "friend1"))
            em.persist(FriendshipJpaEntity.create(friend, owner, "friend2"))
            em.flush()

            //when
            val result = repository.existsByOwnerUserIdAndFriendUserId(owner.id!!, friend.id!!)

            //then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("소유자 아이디와 친구 아이디를 기반으로 존재 하지 않으면 false을 반환한다")
        fun existsByIds_false() {
            // given
            val owner = createUser("owner@test.com", "owner")
            val notExistUserId = UUID.randomUUID()
            em.flush()

            //when
            val result = repository.existsByOwnerUserIdAndFriendUserId(owner.id!!, notExistUserId)

            //then
            assertThat(result).isFalse()
        }
    }

    private fun createUser(email: String, nickname: String): UserJpaEntity {
        val user = UserJpaEntity(
            email = email,
            nickname = nickname,
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        em.persist(user)
        return user
    }
}
