package com.kdongsu5509.imhereuserservice.application.service.friend

import com.kdongsu5509.imhereuserservice.application.port.out.friend.FriendRequestSavePort
import com.kdongsu5509.imhereuserservice.domain.friend.FriendRequest
import com.kdongsu5509.imhereuserservice.domain.friend.FriendRequestUserInfo
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestCreateServiceTest {

    @Mock
    lateinit var friendRequestSavePort: FriendRequestSavePort

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

        given(friendRequestSavePort.createFriendshipRequest(testEmail, testReceiverId, testMsg))
            .willReturn(expectedFriendRequest)

        // when
        val result = friendRequestCreateService.request(testEmail, testReceiverId, testMsg)

        // then
        assertThat(result).isNotNull
        assertThat(result.requester.email).isEqualTo(testEmail)
        assertThat(result.message).isEqualTo(testMsg)

        then(friendRequestSavePort).should().createFriendshipRequest(testEmail, testReceiverId, testMsg)
    }

    @Test
    @DisplayName("savePort에서 오류가 발생하면 잘 전파한다")
    fun request_fail() {
        // given
        val testEmail = "ds.ko@kakao.com"
        val testReceiverId = UUID.randomUUID()
        val testMsg = "친하게 지내요!"

        given(friendRequestSavePort.createFriendshipRequest(testEmail, testReceiverId, testMsg))
            .willThrow(BusinessException(ErrorCode.USER_NOT_FOUND))

        // when
        Assertions.assertThatThrownBy {
            friendRequestCreateService.request(testEmail, testReceiverId, testMsg)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.USER_NOT_FOUND.message)

        then(friendRequestSavePort).should().createFriendshipRequest(testEmail, testReceiverId, testMsg)
    }
}