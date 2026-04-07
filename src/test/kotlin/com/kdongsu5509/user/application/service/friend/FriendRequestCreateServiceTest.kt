package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.out.friend.FriendRequestSavePort
import com.kdongsu5509.user.application.port.out.noti.FriendAlertPort
import com.kdongsu5509.user.domain.friend.FriendRequest
import com.kdongsu5509.user.domain.friend.FriendRequestUserInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestCreateServiceTest {

    companion object {
        const val SENDER_EMAIL = "ds.ko@kakao.com"
        const val RECEIVER_EMAIL = "ds.ko2@kakao.com"
        const val TEST_NICKNAME = "rati"
        const val TEST_MSG = "친하게 지내요!"
    }

    @Mock
    lateinit var friendRequestSavePort: FriendRequestSavePort

    @Mock
    lateinit var friendAlertPort: FriendAlertPort

    @InjectMocks
    lateinit var friendRequestCreateService: FriendRequestCreateService

    @Test
    @DisplayName("request 요청을 savePort에 잘 전달하고 반환값을 확인한다")
    fun request_success() {
        // given
        val testEmail = "ds.ko@kakao.com"
        val testReceiverId = UUID.randomUUID()
        val testMsg = "친하게 지내요!"

        val requesterInfo = FriendRequestUserInfo(UUID.randomUUID(), testEmail, "고동수")
        val receiverInfo = FriendRequestUserInfo(testReceiverId, "other@kakao.com", "상대방")

        val expectedFriendRequest = FriendRequest.create(
            requester = requesterInfo,
            receiver = receiverInfo,
            message = testMsg
        )

        given(friendRequestSavePort.save(testEmail, testReceiverId, testMsg))
            .willReturn(expectedFriendRequest)

        // when
        val result = friendRequestCreateService.request(
            myEmail = SENDER_EMAIL,
            myNickname = TEST_NICKNAME,
            receiverId = testReceiverId,
            receiverEmail = RECEIVER_EMAIL,
            message = TEST_MSG
        )

        // then
        assertThat(result).isNotNull
        assertThat(result.requester.email).isEqualTo(testEmail)
        assertThat(result.message).isEqualTo(testMsg)

        then(friendRequestSavePort).should().save(testEmail, testReceiverId, testMsg)
    }

    @Test
    @DisplayName("savePort에서 오류가 발생하면 잘 전파한다")
    fun request_fail() {
        // given
        val testEmail = "ds.ko@kakao.com"
        val testReceiverId = UUID.randomUUID()
        val testMsg = "친하게 지내요!"

        given(friendRequestSavePort.save(testEmail, testReceiverId, testMsg))
            .willThrow(BusinessException(UserErrorCode.USER_NOT_FOUND))

        // when
        Assertions.assertThatThrownBy {
            friendRequestCreateService.request(
                myEmail = SENDER_EMAIL,
                myNickname = TEST_NICKNAME,
                receiverId = testReceiverId,
                receiverEmail = RECEIVER_EMAIL,
                message = TEST_MSG
            )
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(UserErrorCode.USER_NOT_FOUND.message)

        then(friendRequestSavePort).should().save(testEmail, testReceiverId, testMsg)
    }

    @Test
    @DisplayName("다른 로직이 정상적으로 종료되면, 최종적으로 MQ에 메시지를 발급한다")
    fun publish_message_success() {
        // given
        val testReceiverId = UUID.randomUUID()
        val requesterInfo = FriendRequestUserInfo(UUID.randomUUID(), SENDER_EMAIL, TEST_NICKNAME)
        val receiverInfo = FriendRequestUserInfo(testReceiverId, RECEIVER_EMAIL, "상대방")

        val expectedFriendRequest = FriendRequest.create(
            requester = requesterInfo,
            receiver = receiverInfo,
            message = TEST_MSG
        )
        val alertInformation = AlertInformation(
            senderNickname = TEST_NICKNAME,
            body = "$TEST_NICKNAME 님이 친구 요청을 보냈습니다.",
            receiverEmail = RECEIVER_EMAIL
        )

        given(friendRequestSavePort.save(SENDER_EMAIL, testReceiverId, TEST_MSG))
            .willReturn(expectedFriendRequest)
        willDoNothing().given(friendAlertPort).sendAlert(alertInformation)


        // when
        friendRequestCreateService.request(
            myEmail = SENDER_EMAIL,
            myNickname = TEST_NICKNAME,
            receiverId = testReceiverId,
            receiverEmail = RECEIVER_EMAIL,
            message = TEST_MSG
        )

        then(friendAlertPort).should(times(1)).sendAlert(alertInformation)
    }
}
