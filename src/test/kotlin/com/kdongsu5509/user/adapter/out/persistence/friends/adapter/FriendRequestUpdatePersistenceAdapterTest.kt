package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
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
    FriendRequestUpdatePersistenceAdapter::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRequestUpdatePersistenceAdapterTest @Autowired constructor(
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataFriendRequestRepository: SpringDataFriendRequestRepository,
    private val friendRequestUpdatePersistenceAdapter: FriendRequestUpdatePersistenceAdapter,
) {
    private lateinit var requester: UserJpaEntity
    private lateinit var receiver: UserJpaEntity
    private lateinit var friendRequest: FriendRequestJpaEntity

    @BeforeEach
    fun setUp() {
        requester = springDataUserRepository.save(
            UserJpaEntity("requester@kakao.com", "요청자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        receiver = springDataUserRepository.save(
            UserJpaEntity("receiver@kakao.com", "수신자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        friendRequest = springDataFriendRequestRepository.save(
            FriendRequestJpaEntity(
                requester, receiver, "친하게 지내요"
            )
        )
    }

    @Test
    @DisplayName("친구 요청을 잘 지운다")
    fun delete_success() {
        //when
        friendRequestUpdatePersistenceAdapter.delete(friendRequest.id!!)

        //then
        Assertions.assertThat(
            springDataFriendRequestRepository.findById(friendRequest.id!!).isEmpty
        ).isTrue
    }

    @Test
    @DisplayName("요청 시 존재하지 않는 친구 요청 ID를 전달해도 오류가 발생하지 않는다")
    fun delete_success_even_though_not_exist() {
        //given
        val notExistRequestId = 10000L

        //when, then
        assertDoesNotThrow {
            friendRequestUpdatePersistenceAdapter.delete(notExistRequestId)
        }
    }
}
