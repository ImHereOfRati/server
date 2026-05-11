package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.user.exception.UserError
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRestrictionMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.friend.FriendRequestUserInfo
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("test")
@DataJpaTest
@Import(
    FriendRestrictionSavePersistenceAdapter::class,
    FriendRestrictionMapper::class,
    FriendRestrictionSavePersistenceAdapterTest.TestConfig::class
)
class FriendRestrictionSavePersistenceAdapterTest @Autowired constructor(
    private val em: EntityManager,
    private val userRepository: SpringQueryDSLUserRepository,
    private val adapter: FriendRestrictionSavePersistenceAdapter,
    private val springDataFriendRestrictionRepository: SpringDataFriendRestrictionRepository
) {

    @TestConfiguration
    class TestConfig(private val em: EntityManager) {
        @Bean
        fun jpaQueryFactory() = JPAQueryFactory(em)

        @Bean
        fun springQueryDSLUserRepository(jpaQueryFactory: JPAQueryFactory) =
            SpringQueryDSLUserRepository(jpaQueryFactory)
    }

    companion object {
        fun email(idx: Int) = "test$idx@kakao.com"
        fun nickname(idx: Int) = "?�스??idx"
    }

    private lateinit var requester: UserJpaEntity
    private lateinit var receiver: UserJpaEntity

    @BeforeEach
    fun setUp() {
        requester = createTestUser(1)
        receiver = createTestUser(2)

        em.persist(requester)
        em.persist(receiver)
        em.flush()
        em.clear()
    }

    @Test
    @DisplayName("친구 ?�청 거절 ???�신??actor) ?�장???�한 ?�코?��? ?�확???�?�된??")
    fun save_restriction_success() {
        // given
        val requesterInfo = FriendRequestUserInfo(requester.id!!, requester.email, requester.nickname)
        val receiverInfo = FriendRequestUserInfo(receiver.id!!, receiver.email, receiver.nickname)
        val type = FriendRestrictionType.REJECT

        // when
        val resultDomain = adapter.save(requesterInfo, receiverInfo, type)

        // then
        val savedEntities = springDataFriendRestrictionRepository.findAll()
        assertEquals(1, savedEntities.size)

        val entity = savedEntities[0]
        assertEquals(receiver.email, entity.actor.email)
        assertEquals(requester.email, entity.target.email)
        assertEquals(type, entity.type)
        assertEquals(entity.id, resultDomain.friendRestrictionId)
    }

    @Test
    @DisplayName("존재?��? ?�는 ?��? ?�보�??�????BaseException??발생?�다")
    fun save_fail_user_not_found() {
        // given
        val invalidInfo = FriendRequestUserInfo(UUID.randomUUID(), "none@test.com", "존재?��??�는??")
        val receiverInfo = FriendRequestUserInfo(receiver.id!!, receiver.email, receiver.nickname)

        // when & then
        assertThrows<BaseException> {
            adapter.save(invalidInfo, receiverInfo, FriendRestrictionType.REJECT)
        }.also {
//            assertEquals(UserError.USER_NOT_FOUND, it.errorCode)
        }
    }

    private fun createTestUser(idx: Int) = UserJpaEntity(
        email = email(idx),
        nickname = nickname(idx),
        role = UserRole.NORMAL,
        provider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )
}
