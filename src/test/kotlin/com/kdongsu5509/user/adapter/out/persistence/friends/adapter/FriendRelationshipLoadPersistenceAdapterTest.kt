package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.user.exception.UserError
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRelationshipMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(
    FriendRelationshipMapper::class,
    FriendRelationshipLoadPersistenceAdapter::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRelationshipLoadPersistenceAdapterTest @Autowired constructor(
    private val em: EntityManager,
    private val userRepository: SpringDataUserRepository,
    private val friendRelationshipAdapter: FriendRelationshipLoadPersistenceAdapter,
) {

    private lateinit var testUser: UserJpaEntity

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(createUserEntity(0))
    }

    @Test
    @DisplayName("Emailë¥??µí•´??ì¹œêµ¬ë¥???ì°¾ì•„?¨ë‹¤.")
    fun findFriendsRelationshipsByUserEmail_success() {
        //given
        createTenTestUserAsFriendWithTestUser()
        testUser = userRepository.findByEmail("test0@test.com")!!

        //when
        val testResult = friendRelationshipAdapter.findFriendsRelationshipsByUserEmail(testUser.email)

        //then
        Assertions.assertThat(testResult.size).isEqualTo(10)
    }


    @Test
    @DisplayName("?¬ìš©?ë? Emailë¥??µí•´ ì°¾ì? ëª»í•˜ë©??¤ë¥˜ê°€ ?°ì§„??")
    fun findFriendsRelationshipsByUserEmail_fail_not_found() {
        //given
        val noExistEmail = "nonono@nonono.com"

        //when, then
        Assertions.assertThatThrownBy {
            friendRelationshipAdapter.findFriendsRelationshipsByUserEmail(noExistEmail)
        }.isInstanceOf(BaseException::class.java)
            .hasMessage(UserError.USER_NOT_FOUND.message)
    }

    @Test
    @DisplayName("ì¹œêµ¬ê°€ ?†ìœ¼ë©?ë¹?ë¦¬ìŠ¤?¸ê? ë°˜í™˜?œë‹¤.")
    fun findFriendsByUserEmail_no_friendsRelationships() {
        //given
        val noFriendUser = createUserEntity(999)
        userRepository.save(noFriendUser)

        //when
        val testResult = friendRelationshipAdapter.findFriendsRelationshipsByUserEmail(noFriendUser.email)

        //then
        Assertions.assertThat(testResult.size).isEqualTo(0)
    }

    private fun createTenTestUserAsFriendWithTestUser() {
        (1..10).forEach { idx ->
            val friend = createUserEntity(idx)
            em.persist(friend)

            val relationship = FriendRelationshipsJpaEntity(
                ownerUser = testUser,
                friendUser = friend,
                friendAlias = friend.nickname
            )
            em.persist(relationship)
        }
        em.flush()
        em.clear()
    }

    private fun createUserEntity(idx: Int): UserJpaEntity {
        return UserJpaEntity(
            email = "test$idx@test.com",
            nickname = "?ŒìŠ¤??idx",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
    }
}
