package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRequestMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("test")
@DataJpaTest
@Import(
    FriendRequestSavePersistenceAdapter::class,
    FriendRequestMapper::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRequestSavePersistenceAdapterTest @Autowired constructor(
    private val adapter: FriendRequestSavePersistenceAdapter,
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataFriendRequestRepository: SpringDataFriendRequestRepository
) {

    private lateinit var requester: UserJpaEntity
    private lateinit var receiver: UserJpaEntity

    @BeforeEach
    fun setUp() {
        requester = springDataUserRepository.save(
            UserJpaEntity("requester@kakao.com", "요청자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
        receiver = springDataUserRepository.save(
            UserJpaEntity("receiver@kakao.com", "수신자", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        )
    }

    @Test
    @DisplayName("친구 요청을 DB에 잘 저장하고 도메인 엔티티를 반환한다")
    fun save_success() {
        // given
        val message = "친하게 지내요!"

        // when
        val result = adapter.save(requester.email, receiver.id!!, message)

        // then
        assertThat(result).isNotNull
        assertThat(result.message).isEqualTo(message)

        val savedRequests = springDataFriendRequestRepository.findAll()
        assertThat(savedRequests).hasSize(1)
        assertThat(savedRequests[0].requester.id).isEqualTo(requester.id)
        assertThat(savedRequests[0].receiver.id).isEqualTo(receiver.id)
        assertThat(savedRequests[0].message).isEqualTo(message)
    }

    @Test
    @DisplayName("잘못된 수신자 ID(없는 존재)는 오류 발생")
    fun save_receiver_not_exist() {
        // given
        val message = "친하게 지내요!"

        // when, then
        Assertions.assertThatThrownBy {
            adapter.save(requester.email, UUID.randomUUID(), message)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(UserErrorCode.USER_NOT_FOUND.message)
    }
}
