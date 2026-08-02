package com.kdongsu5509.notifications.adapter.out.friend

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.repository.FriendRelationRepository
import com.kdongsu5509.notifications.application.port.out.SenderAliasPort
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class FriendSenderAliasAdapterIntegrationTest : PersistenceTestSupport() {

    @Autowired
    private lateinit var senderAliasPort: SenderAliasPort

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var friendRelationRepository: FriendRelationRepository

    private fun persistUser(email: String, nickname: String): UUID =
        userRepository.save(User(email, nickname, OAuth2Provider.KAKAO).activate()).id!!

    private fun befriend(
        one: UUID,
        oneNickname: String,
        other: UUID,
        otherNickname: String,
        oneCallsOther: String? = null,
        otherCallsOne: String? = null,
    ) {
        val (low, high) = com.kdongsu5509.friends.domain.FriendPair.ordered(one, other)
        val lowNickname = if (low == one) oneNickname else otherNickname
        val highNickname = if (high == one) oneNickname else otherNickname

        var relation = FriendRelation(one, other, "친구가 되고 싶습니다")
            .accept(lowNickname = lowNickname, highNickname = highNickname)
        oneCallsOther?.let { relation = relation.rename(one, it) }
        otherCallsOne?.let { relation = relation.rename(other, it) }
        friendRelationRepository.save(relation)
    }

    @Test
    @DisplayName("친구가 붙여 둔 별칭을 식별자 한 쌍으로 찾아낸다")
    fun findAlias_success() {
        // given
        val sender = persistUser("sender@imhere.com", "홍길동")
        val receiver = persistUser("receiver@imhere.com", "김철수")
        befriend(
            one = receiver, oneNickname = "김철수",
            other = sender, otherNickname = "홍길동",
            oneCallsOther = "길동이",
        )

        // when
        val alias = senderAliasPort.findAlias(receiver, sender)

        // then
        assertThat(alias).isEqualTo("길동이")
    }

    @Test
    @DisplayName("별칭은 방향이 있다 - 부르는 쪽이 바뀌면 답도 바뀐다")
    fun findAlias_success_is_directional() {
        // given
        val a = persistUser("a@imhere.com", "에이")
        val b = persistUser("b@imhere.com", "비")
        befriend(
            one = a, oneNickname = "에이",
            other = b, otherNickname = "비",
            oneCallsOther = "비형",
            otherCallsOne = "에이형",
        )

        // when & then
        assertThat(senderAliasPort.findAlias(a, b)).isEqualTo("비형")
        assertThat(senderAliasPort.findAlias(b, a)).isEqualTo("에이형")
    }

    @Test
    @DisplayName("수락한 순간에는 상대 닉네임이 곧 별칭이다")
    fun findAlias_success_defaults_to_counterpart_nickname() {
        // given
        val sender = persistUser("nick-sender@imhere.com", "홍길동")
        val receiver = persistUser("nick-receiver@imhere.com", "김철수")
        befriend(
            one = receiver, oneNickname = "김철수",
            other = sender, otherNickname = "홍길동",
        )

        // when & then
        assertThat(senderAliasPort.findAlias(receiver, sender)).isEqualTo("홍길동")
    }

    @Test
    @DisplayName("친구가 아니면 부르는 이름이 없다")
    fun findAlias_returnsNull_when_not_friends() {
        val a = persistUser("lonely-a@imhere.com", "에이")
        val b = persistUser("lonely-b@imhere.com", "비")

        assertThat(senderAliasPort.findAlias(a, b)).isNull()
    }

    @Test
    @DisplayName("아직 수락되지 않은 요청 상태에서는 부르는 이름이 없다")
    fun findAlias_returnsNull_when_only_requested() {
        // given
        val a = persistUser("req-a@imhere.com", "에이")
        val b = persistUser("req-b@imhere.com", "비")
        friendRelationRepository.save(FriendRelation(a, b, "친구가 되고 싶습니다"))

        // when & then
        assertThat(senderAliasPort.findAlias(a, b)).isNull()
    }

    @Test
    @DisplayName("존재하지 않는 사용자 식별자는 조용히 비운다")
    fun findAlias_returnsNull_when_user_is_unknown() {
        val sender = persistUser("unknown-sender@imhere.com", "홍길동")

        assertThat(senderAliasPort.findAlias(UUID.randomUUID(), sender)).isNull()
        assertThat(senderAliasPort.findAlias(sender, UUID.randomUUID())).isNull()
    }
}
