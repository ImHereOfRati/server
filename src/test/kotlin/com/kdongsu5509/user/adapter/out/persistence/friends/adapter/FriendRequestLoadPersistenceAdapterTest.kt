package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
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

@ActiveProfiles("test")
@DataJpaTest
@Import(
    FriendRequestLoadPersistenceAdapter::class,
    FriendRequestSavePersistenceAdapter::class,
    FriendRequestMapper::class,
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class
)
class FriendRequestLoadPersistenceAdapterTest @Autowired constructor(
    private val saveAdapter: FriendRequestSavePersistenceAdapter,
    private val loadAdapter: FriendRequestLoadPersistenceAdapter,
    private val springDataUserRepository: SpringDataUserRepository,
) {

    companion object {
        const val REQUESTER_EMAIL1 = "requester1@kakao.com"
        const val REQUESTER_EMAIL2 = "requester2@kakao.com"
        const val REQUESTER_EMAIL3 = "requester3@kakao.com"

        const val RECEIVER_EMAIL = "receiver@kakao.com"

        const val REQUESTER_NICKNAME = "요청자"
        const val RECEIVER_NICKNAME = "수신자"
    }

    private lateinit var requester1: UserJpaEntity
    private lateinit var requester2: UserJpaEntity
    private lateinit var requester3: UserJpaEntity
    private lateinit var receiver: UserJpaEntity

    @BeforeEach
    fun setUp() {
        requester1 = saveUserInDatabase(REQUESTER_EMAIL1, REQUESTER_NICKNAME)
        requester2 = saveUserInDatabase(REQUESTER_EMAIL2, REQUESTER_NICKNAME)
        requester3 = saveUserInDatabase(REQUESTER_EMAIL3, REQUESTER_NICKNAME)
        receiver = saveUserInDatabase(RECEIVER_EMAIL, RECEIVER_NICKNAME)
    }

    @Test
    @DisplayName("특정 이메일에 온 친구 요청들을 잘 찾는다.")
    fun findReceivedRequestByRequestIdRequestsAll_ByEmail_success() {
        // given
        val message = "친하게 지내요!"

        saveAdapter.save(requester1.email, receiver.id!!, message)
        saveAdapter.save(requester2.email, receiver.id!!, message)
        saveAdapter.save(requester3.email, receiver.id!!, message)

        // when
        val result = loadAdapter.findReceivedRequestsAllByEmail(receiver.email)

        // then
        assertThat(result).isNotEmpty
        assertThat(result).hasSize(3)

        val requestsEmail = listOf("requester1@kakao.com", "requester2@kakao.com", "requester3@kakao.com")

        assertThat(requestsEmail.contains(result[0].requester.email)).isTrue
        assertThat(requestsEmail.contains(result[1].requester.email)).isTrue
        assertThat(requestsEmail.contains(result[2].requester.email)).isTrue
    }

    @Test
    @DisplayName("특정 이메일에 온 친구 요청이 없으면 빈 리스트를 반환한다.")
    fun findReceivedRequestByRequestIdRequestsAll_ByEmail_success_empty() {
        // when
        val result = loadAdapter.findReceivedRequestsAllByEmail("none@kakao.com")

        // then
        assertThat(result).isEmpty()
        assertThat(result).hasSize(0)
    }

    @Test
    @DisplayName("특정 친구 요청을 잘 찾는다.")
    fun findReceived_RequestByRequestId_success() {
        // given
        val message = "친하게 지내요!"

        val savedFriendRequest = saveAdapter.save(requester1.email, receiver.id!!, message)

        // when
        val result = loadAdapter.findReceivedRequestByRequestId(savedFriendRequest.friendRequestId!!)

        // then
        assertThat(result.requester.email).isEqualTo(requester1.email)
        assertThat(result.requester.nickname).isEqualTo(requester1.nickname)
        assertThat(result.receiver.email).isEqualTo(receiver.email)
        assertThat(result.receiver.nickname).isEqualTo(receiver.nickname)
    }

    @Test
    @DisplayName("특정 친구 요청의 ID 가 없으면 오류가 발생한다")
    fun findReceived_RequestByRequestId_fail_id_not_exist() {
        // when, then
        Assertions.assertThatThrownBy {
            loadAdapter.findReceivedRequestByRequestId(1L)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(FriendErrorCode.FRIENDSHIP_REQUEST_NOT_FOUND.message)
    }

    private fun saveUserInDatabase(email: String, nickname: String): UserJpaEntity = springDataUserRepository.save(
        UserJpaEntity(
            email,
            nickname,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            UserStatus.ACTIVE
        )
    )
}
