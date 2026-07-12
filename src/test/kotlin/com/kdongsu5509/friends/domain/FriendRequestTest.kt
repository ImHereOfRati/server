package com.kdongsu5509.friends.domain

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class FriendRequestTest {

    private fun user(nickname: String, email: String = "$nickname@test.com"): User = User(
        id = UUID.randomUUID(),
        email = email,
        nickname = nickname,
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Test
    @DisplayName("accept()는 요청자/수신자를 서로 owner로 스왑하고 alias를 상대 닉네임으로 채운 Friendship 두 건을 반환한다")
    fun accept_swapsOwnerAndUsesCounterpartNickname() {
        val requester = user("requester")
        val receiver = user("receiver")
        val friendRequest = FriendRequest(requester = requester, receiver = receiver, message = "hi")

        val (requesterFriendship, receiverFriendship) = friendRequest.accept()

        assertThat(requesterFriendship.owner).isEqualTo(requester)
        assertThat(requesterFriendship.friend).isEqualTo(receiver)
        assertThat(requesterFriendship.friendAlias.value).isEqualTo(receiver.nickname)

        assertThat(receiverFriendship.owner).isEqualTo(receiver)
        assertThat(receiverFriendship.friend).isEqualTo(requester)
        assertThat(receiverFriendship.friendAlias.value).isEqualTo(requester.nickname)
    }

    @Test
    @DisplayName("accept()는 20자를 넘는 상대 닉네임을 잘라서 alias로 사용한다")
    fun accept_truncatesLongNickname() {
        val requester = user("requester")
        val longNicknameReceiver = user("a".repeat(30))
        val friendRequest = FriendRequest(requester = requester, receiver = longNicknameReceiver, message = "hi")

        val (requesterFriendship, _) = friendRequest.accept()

        assertThat(requesterFriendship.friendAlias.value).isEqualTo("a".repeat(20))
    }

    @Test
    @DisplayName("reject()는 수신자가 요청자를 30일간 거절 제한하는 FriendRestriction을 반환한다")
    fun reject_createsRestrictionFromReceiverToRequester() {
        val requester = user("requester")
        val receiver = user("receiver")
        val friendRequest = FriendRequest(requester = requester, receiver = receiver, message = "hi")

        val restriction = friendRequest.reject()

        assertThat(restriction.restrictor).isEqualTo(receiver)
        assertThat(restriction.restricted).isEqualTo(requester)
        assertThat(restriction.type).isEqualTo(FriendRestrictionType.REJECT)
        assertThat(restriction.expiredAt).isNotNull()
    }

    @Test
    @DisplayName("isReceivedBy/isRequestedBy/involves는 이메일로 참여자 여부를 답한다")
    fun participantQueries() {
        val requester = user("requester")
        val receiver = user("receiver")
        val friendRequest = FriendRequest(requester = requester, receiver = receiver, message = "hi")

        assertThat(friendRequest.isReceivedBy(receiver.email)).isTrue()
        assertThat(friendRequest.isReceivedBy(requester.email)).isFalse()

        assertThat(friendRequest.isRequestedBy(requester.email)).isTrue()
        assertThat(friendRequest.isRequestedBy(receiver.email)).isFalse()

        assertThat(friendRequest.involves(requester.email)).isTrue()
        assertThat(friendRequest.involves(receiver.email)).isTrue()
        assertThat(friendRequest.involves("stranger@test.com")).isFalse()
    }
}
