package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
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
    FriendRestrictionUpdatePersistenceAdapter::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRestrictionUpdatePersistenceAdapterTest @Autowired constructor(
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataFriendRestrictionRepository: SpringDataFriendRestrictionRepository,
    private val friendRestrictionUpdatePersistenceAdapter: FriendRestrictionUpdatePersistenceAdapter,
) {

    private lateinit var actor: UserJpaEntity
    private lateinit var target: UserJpaEntity
    private lateinit var friendRestriction: FriendRestrictionJpaEntity

    @BeforeEach
    fun setUp() {
        actor = springDataUserRepository.save(
            UserJpaEntity("requester@kakao.com", "?�청??", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        target = springDataUserRepository.save(
            UserJpaEntity("receiver@kakao.com", "?�신??", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        friendRestriction = springDataFriendRestrictionRepository.save(
            FriendRestrictionJpaEntity(
                actor, target, FriendRestrictionType.BLOCK
            )
        )
    }

    @Test
    @DisplayName("친구 차단/거절 기록????지?�다")
    fun delete_ById_success() {
        //when
        friendRestrictionUpdatePersistenceAdapter.deleteById(friendRestriction.id!!)

        //then
        Assertions.assertThat(
            springDataFriendRestrictionRepository.findById(friendRestriction.id!!).isEmpty
        ).isTrue
    }

    @Test
    @DisplayName("?�청 ??존재?��? ?�는 차단/거절 ID�??�달?�도 ?�류가 발생?��? ?�는??")
    fun delete_success_even_though_not_exist() {
        //given
        val notExistRequestId = 10000L

        //when, then
        assertDoesNotThrow {
            friendRestrictionUpdatePersistenceAdapter.deleteById(notExistRequestId)
        }
    }
}
