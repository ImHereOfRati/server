package com.kdongsu5509.friends.repository

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.*

class FriendRelationRepositoryTest : PersistenceTestSupport() {

    @Autowired
    private lateinit var friendRelationRepository: FriendRelationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val message = "친구가 되고 싶습니다"

    private fun persistedUserId(email: String, nickname: String): UUID =
        userRepository.save(User(email, nickname, OAuth2Provider.KAKAO).activate()).id!!

    @Test
    @DisplayName("저장한 관계를 그대로 되돌린다")
    fun save_and_restore() {
        val requester = persistedUserId("req@example.com", "requester")
        val receiver = persistedUserId("rec@example.com", "receiver")

        val saved = friendRelationRepository.save(FriendRelation(requester, receiver, message))

        val found = friendRelationRepository.findById(saved.id!!)

        assertThat(found).isNotNull
        assertThat(found!!.status).isEqualTo(FriendRelationStatus.REQUESTED)
        assertThat(found.message?.value).isEqualTo(message)
        assertThat(found.initiator()).isEqualTo(requester)
        assertThat(found.target()).isEqualTo(receiver)
    }

    @Test
    @DisplayName("어느 순서로 조회해도 같은 관계를 찾는다")
    fun find_by_pair_is_order_independent() {
        val one = persistedUserId("one@example.com", "one")
        val other = persistedUserId("other@example.com", "other")
        friendRelationRepository.save(FriendRelation(one, other, message))

        val forward = friendRelationRepository.findByPair(one, other)
        val backward = friendRelationRepository.findByPair(other, one)

        assertThat(forward).isNotNull
        assertThat(backward).isNotNull
        assertThat(forward!!.id).isEqualTo(backward!!.id)
    }

    @Test
    @DisplayName("같은 쌍을 다시 저장하면 행이 늘지 않고 덮어쓴다")
    fun save_overwrites_same_pair() {
        val one = persistedUserId("dup1@example.com", "dup1")
        val other = persistedUserId("dup2@example.com", "dup2")
        val requested = friendRelationRepository.save(FriendRelation(one, other, message))

        // 식별자가 없는 새 관계라도 같은 쌍이면 기존 행을 덮어써야 한다.
        val reBlocked = friendRelationRepository.save(FriendRelation.blockWithoutRelation(one, other))

        assertThat(reBlocked.id).isEqualTo(requested.id)
        assertThat(friendRelationRepository.findByPair(one, other)!!.status)
            .isEqualTo(FriendRelationStatus.BLOCKED)
    }

    @Test
    @DisplayName("만료된 관계만 지운다")
    fun delete_expired_only() {
        val one = persistedUserId("exp1@example.com", "exp1")
        val other = persistedUserId("exp2@example.com", "exp2")
        val third = persistedUserId("exp3@example.com", "exp3")

        val now = LocalDateTime.now()
        // 거절은 한 달 뒤 만료된다. 두 달 전에 거절한 관계는 이미 만료된 상태다.
        friendRelationRepository.save(FriendRelation(one, other, message).reject(now.minusMonths(2)))
        val blocked = friendRelationRepository.save(FriendRelation.blockWithoutRelation(one, third))

        friendRelationRepository.deleteExpired(now)

        assertThat(friendRelationRepository.findByPair(one, other)).isNull()
        assertThat(friendRelationRepository.findById(blocked.id!!)).isNotNull
    }
}
