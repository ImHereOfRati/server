package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.ErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRestrictionMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(
    FriendRestrictionMapper::class,
    FriendRestrictionLoadPersistenceAdapter::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRestrictionLoadPersistenceAdapterTest @Autowired constructor(
    private val em: EntityManager,
    private val userRepository: SpringDataUserRepository,
    private val friendRestrictionLoadPersistenceAdapter: FriendRestrictionLoadPersistenceAdapter,
) {
    private lateinit var testUser: UserJpaEntity

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(createUserEntity(0))
    }

    @Test
    @DisplayName("Email를 통해서 차단/거절한 친구를 잘 찾아온다.")
    fun loadAll_success() {
        //given
        createNTestUserAsFriendWithTestUser(10)
        testUser = userRepository.findByEmail("test0@test.com")!!

        //when
        val testResult = friendRestrictionLoadPersistenceAdapter.loadAll(testUser.email)

        //then
        Assertions.assertThat(testResult.size).isEqualTo(10)
        val rejectCount = testResult.count { it.restrictionType == FriendRestrictionType.REJECT }
        val blockCount = testResult.count { it.restrictionType == FriendRestrictionType.BLOCK }
        Assertions.assertThat(rejectCount).isEqualTo(5)
        Assertions.assertThat(blockCount).isEqualTo(5)
    }


    @Test
    @DisplayName("사용자를 Email를 통해 찾지 못하면 오류가 터진다.")
    fun loadAll_fail_not_found() {
        //given
        val noExistEmail = "nonono@nonono.com"

        //when, then
        Assertions.assertThatThrownBy {
            friendRestrictionLoadPersistenceAdapter.loadAll(noExistEmail)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.USER_NOT_FOUND.message)
    }

    @Test
    @DisplayName("차단한 친구가 없으면 빈 리스트가 반환된다.")
    fun loadAll_no_friends() {
        //given
        val noFriendUser = createUserEntity(999)
        userRepository.save(noFriendUser)

        //when
        val testResult = friendRestrictionLoadPersistenceAdapter.loadAll(noFriendUser.email)

        //then
        Assertions.assertThat(testResult.size).isEqualTo(0)
    }

    @Test
    @DisplayName("id를 통해 friendRestriction을 잘 찾아온다.")
    fun loadById_success() {
        //given
        val friend = createUserEntity(1)
        val friendRestrictionJpaEntity = createFriendRestrictionJpaEntity(1, friend)
        em.persist(friend)
        em.persist(friendRestrictionJpaEntity)
        em.flush()
        em.clear()

        //when
        val testResult = friendRestrictionLoadPersistenceAdapter.loadById(friendRestrictionJpaEntity.id!!)

        //then
        Assertions.assertThat(testResult.actorEmail).isEqualTo(testUser.email)
        Assertions.assertThat(testResult.targetEmail).isEqualTo(friend.email)
        Assertions.assertThat(testResult.targetNickname).isEqualTo(friend.nickname)
    }

    @Test
    @DisplayName("id가 없으면 오류가 발생한다.- ErrorCode.FRIEND_RESTRICTION_NOT_FOUND")
    fun loadById_fail_not_exist() {
        //when, then
        Assertions.assertThatThrownBy {
            friendRestrictionLoadPersistenceAdapter.loadById(999L)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.FRIEND_RESTRICTION_NOT_FOUND.message)
    }

    private fun createNTestUserAsFriendWithTestUser(n: Int) {
        (1..n).forEach { idx ->
            val friend = createUserEntity(idx)
            em.persist(friend)

            val restriction = createFriendRestrictionJpaEntity(idx, friend)
            em.persist(restriction)
        }
        em.flush()
        em.clear()
    }

    private fun createFriendRestrictionJpaEntity(idx: Int, friend: UserJpaEntity): FriendRestrictionJpaEntity {
        return FriendRestrictionJpaEntity(
            actor = testUser,
            target = friend,
            type = if (idx % 2 == 0) FriendRestrictionType.REJECT else FriendRestrictionType.BLOCK
        )
    }

    private fun createUserEntity(idx: Int): UserJpaEntity {
        return UserJpaEntity(
            email = "test$idx@test.com",
            nickname = "테스터$idx",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
    }
}