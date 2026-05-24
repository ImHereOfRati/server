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
class SpringDataFriendRequestRepositoryTest @Autowired constructor(
    private val em: EntityManager,
    private val repository: SpringDataFriendRequestRepository
) {

    @Test
    @DisplayName("수신자 이메일과 페이징 정보로 친구 요청 슬라이스를 조회한다")
    fun findAllByReceiverEmail_slice_success() {
        // given
        val requester = createUser("requester@test.com", "requester")
        val receiver = createUser("receiver@test.com", "receiver")
        val request1 = FriendRequestJpaEntity(requester, receiver, "message1")
        val request2 = FriendRequestJpaEntity(requester, receiver, "message2")
        em.persist(request1)
        em.persist(request2)
        em.flush()

        val pageable = PageRequest.of(0, 1)

        // when
        val result = repository.findAllByReceiverEmail(receiver.email, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    @DisplayName("송신자 이메일과 페이징 정보로 친구 요청 슬라이스를 조회한다")
    fun findAllByRequesterEmail_slice_success() {
        // given
        val requester = createUser("requester@test.com", "requester")
        val receiver = createUser("receiver@test.com", "receiver")
        val request = FriendRequestJpaEntity(requester, receiver, "message")
        em.persist(request)
        em.flush()

        val pageable = PageRequest.of(0, 10)

        // when
        val result = repository.findAllByRequesterEmail(requester.email, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.hasNext()).isFalse()
    }

    @Test
    @DisplayName("송신자와 수신자 엔티티로 양방향 친구 요청 삭제가 동작하는지 확인한다")
    fun deleteByRequesterAndReceiverEntity_success() {
        // given
        val user1 = createUser("user1@test.com", "user1")
        val user2 = createUser("user2@test.com", "user2")
        em.persist(FriendRequestJpaEntity(user1, user2, "from 1 to 2"))
        em.persist(FriendRequestJpaEntity(user2, user1, "from 2 to 1"))
        em.flush()

        // when
        repository.deleteByRequesterAndReceiver(user1, user2)
        repository.deleteByRequesterAndReceiver(user2, user1)
        em.flush()
        em.clear()

        // then
        val check1 = repository.findAllByReceiverEmail(user2.email, PageRequest.of(0, 10))
        val check2 = repository.findAllByReceiverEmail(user1.email, PageRequest.of(0, 10))
        assertThat(check1.content).isEmpty()
        assertThat(check2.content).isEmpty()
    }

    @Nested
    inner class existsByRequesterEmailAndReceiverId {
        @Test
        @DisplayName("송신자 이메일과 수신자 id 를 기반으로 요청이 이미 존재하면 true을 반환한다")
        fun existsByRequesterEmailAndReceiverId_success() {
            //given
            val user1 = createUser("user1@test.com", "user1")
            val user2 = createUser("user2@test.com", "user2")
            em.persist(FriendRequestJpaEntity(user1, user2, "from 1 to 2"))
            em.persist(FriendRequestJpaEntity(user2, user1, "from 2 to 1"))
            em.flush()

            //when
            val exists = repository.existsByRequesterIdAndReceiverId(user1.id!!, user2.id!!)

            //then
            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("송신자 이메일과 수신자 id 를 기반으로 요청이 존재하지 않으면 false을 반환한다")
        fun existsByRequesterEmailAndReceiverId_success_not_exist() {
            //given
            val notExistUserId = UUID.randomUUID()
            val user1 = createUser("user1@test.com", "user1")
            em.flush()

            //when
            val exists = repository.existsByRequesterIdAndReceiverId(user1.id!!, notExistUserId)

            //then
            assertThat(exists).isFalse()
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
