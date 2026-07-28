package com.kdongsu5509.friends.repository

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRequestViewType
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

class FriendRelationQueryRepositoryTest : PersistenceTestSupport() {

    @Autowired
    private lateinit var friendRelationQueryRepository: FriendRelationQueryRepository

    @Autowired
    private lateinit var friendRelationRepository: FriendRelationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val pageable = PageRequest.of(0, 10)
    private val message = "친구가 되고 싶습니다"

    private fun persistedUserId(email: String, nickname: String): UUID =
        userRepository.save(User(email, nickname, OAuth2Provider.KAKAO).activate()).id!!

    private fun acceptedBetween(one: UUID, other: UUID): FriendRelation =
        friendRelationRepository.save(
            FriendRelation(one, other, message).accept(lowAlias = "low", highAlias = "high")
        )

    @Test
    @DisplayName("친구 목록은 내가 low든 high든 모두 찾는다")
    fun friendships_cover_both_columns() {
        val me = persistedUserId("me@example.com", "me")
        val a = persistedUserId("a@example.com", "a")
        val b = persistedUserId("b@example.com", "b")
        acceptedBetween(me, a)
        acceptedBetween(b, me)

        val friends = friendRelationQueryRepository.findFriendships(me, pageable)

        assertThat(friends.content).hasSize(2)
        assertThat(friends.content.map { it.getCounterpart(me) }).containsExactlyInAnyOrder(a, b)
    }

    @Test
    @DisplayName("보낸 요청과 받은 요청을 방향으로 가른다")
    fun requests_split_by_direction() {
        val me = persistedUserId("dir-me@example.com", "me")
        val target = persistedUserId("dir-target@example.com", "target")
        val sender = persistedUserId("dir-sender@example.com", "sender")
        friendRelationRepository.save(FriendRelation(me, target, message))
        friendRelationRepository.save(FriendRelation(sender, me, message))

        val sent = friendRelationQueryRepository.findRequests(me, FriendRequestViewType.SENT, pageable)
        val received = friendRelationQueryRepository.findRequests(me, FriendRequestViewType.RECEIVED, pageable)

        assertThat(sent.content.map { it.target() }).containsExactly(target)
        assertThat(received.content.map { it.initiator() }).containsExactly(sender)
    }

    @Test
    @DisplayName("요청 상태의 관계는 친구 조회에 잡히지 않는다")
    fun friendship_lookup_filters_by_status() {
        val one = persistedUserId("st-one@example.com", "one")
        val other = persistedUserId("st-other@example.com", "other")
        val saved = friendRelationRepository.save(FriendRelation(one, other, message))

        assertThat(friendRelationQueryRepository.findFriendshipById(saved.id!!)).isNull()
        assertThat(friendRelationQueryRepository.findFriendshipByPair(one, other)).isNull()
    }

    @Test
    @DisplayName("내가 건 차단만 제한으로 인정한다")
    fun active_restriction_is_directional() {
        val me = persistedUserId("res-me@example.com", "me")
        val blockedByMe = persistedUserId("res-target@example.com", "target")
        val blockerOfMe = persistedUserId("res-blocker@example.com", "blocker")
        friendRelationRepository.save(FriendRelation.blockWithoutRelation(me, blockedByMe))
        friendRelationRepository.save(FriendRelation.blockWithoutRelation(blockerOfMe, me))

        assertThat(friendRelationQueryRepository.existsActiveRestriction(me, blockedByMe)).isTrue()
        assertThat(friendRelationQueryRepository.existsActiveRestriction(me, blockerOfMe)).isFalse()
    }

    @Test
    @DisplayName("만료된 거절은 제한으로 치지 않는다")
    fun expired_restriction_is_not_active() {
        val requester = persistedUserId("exp-req@example.com", "requester")
        val rejecter = persistedUserId("exp-rej@example.com", "rejecter")
        val stillRejecting = persistedUserId("exp-live@example.com", "live")
        val now = LocalDateTime.now()

        // 거절은 한 달 뒤 만료된다. 두 달 전에 거절한 관계는 이미 만료된 상태다.
        friendRelationRepository.save(FriendRelation(requester, rejecter, message).reject(now.minusMonths(2)))
        friendRelationRepository.save(FriendRelation(requester, stillRejecting, message).reject(now))

        assertThat(friendRelationQueryRepository.existsActiveRestriction(rejecter, requester, now)).isFalse()
        assertThat(friendRelationQueryRepository.existsActiveRestriction(stillRejecting, requester, now)).isTrue()
    }

    @Test
    @DisplayName("관리자 제한 목록은 거절과 차단을 함께 준다")
    fun admin_restriction_list_includes_both() {
        val one = persistedUserId("adm-one@example.com", "one")
        val two = persistedUserId("adm-two@example.com", "two")
        val three = persistedUserId("adm-three@example.com", "three")
        friendRelationRepository.save(FriendRelation.blockWithoutRelation(one, two))
        friendRelationRepository.save(FriendRelation(three, one, message).reject(LocalDateTime.now()))

        val restrictions = friendRelationQueryRepository.findAllRestrictions(pageable)

        assertThat(restrictions.content).hasSize(2)
        // 거절은 방향이 뒤집혀 거절한 one이 주체가 되므로, 둘 다 one이 건 제한이다.
        assertThat(restrictions.content.map { it.initiator() }).containsOnly(one)
    }
}
