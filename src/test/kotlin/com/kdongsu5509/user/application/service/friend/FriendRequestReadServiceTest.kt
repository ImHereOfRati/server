package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.ErrorCode
import com.kdongsu5509.user.application.port.out.friend.FriendRequestLoadPort
import com.kdongsu5509.user.domain.friend.FriendRequest
import com.kdongsu5509.user.domain.friend.FriendRequestUserInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestReadServiceTest {

    @Mock
    lateinit var friendRequestLoadPort: FriendRequestLoadPort

    @InjectMocks
    lateinit var friendRequestReadService: FriendRequestReadService

    companion object {
        const val EMAIL = "ds.ko@kakao.com"
        val RECEIVER_ID: UUID? = UUID.randomUUID()
        const val MESSAGE = "친하게 지내요!"

        val receiverInfo = FriendRequestUserInfo(RECEIVER_ID!!, EMAIL, "고동수")
        val requesterInfo1 = FriendRequestUserInfo(UUID.randomUUID(), "request1@kakao.com", "요청자")
        val requesterInfo2 = FriendRequestUserInfo(UUID.randomUUID(), "request2@kakao.com", "요청자")
        val requesterInfo3 = FriendRequestUserInfo(UUID.randomUUID(), "request3@kakao.com", "요청자")

        val friend_req1 = FriendRequest.create(requesterInfo1, receiverInfo, MESSAGE)
        val friend_req2 = FriendRequest.create(requesterInfo2, receiverInfo, MESSAGE)
        val friend_req3 = FriendRequest.create(requesterInfo3, receiverInfo, MESSAGE)
    }

    @Test
    @DisplayName("특정 이메일의 모든 요청을 loadPort 로 잘 전달한다.")
    fun getReceivedAll_success() {
        // given
        given(friendRequestLoadPort.findReceivedRequestsAllByEmail(EMAIL)).willReturn(
            listOf(friend_req1, friend_req2, friend_req3)
        )

        // when
        val result = friendRequestReadService.getReceivedAll(EMAIL)

        // then
        assertThat(result).isNotNull
        verify(friendRequestLoadPort, times(1)).findReceivedRequestsAllByEmail(EMAIL)
    }

    @Test
    @DisplayName("특정 친구 요청 조회 요청을 loadPort 로 잘 전달한다.")
    fun getReceived_success() {
        // given
        val testFriendRequestId = 100L
        given(friendRequestLoadPort.findReceivedRequestByRequestId(testFriendRequestId)).willReturn(
            FriendRequest(
                testFriendRequestId,
                requesterInfo1,
                receiverInfo,
                MESSAGE,
                LocalDateTime.now()
            )
        )

        // when
        friendRequestReadService.getReceivedDetail(testFriendRequestId)

        // then
        verify(friendRequestLoadPort, times(1)).findReceivedRequestByRequestId(testFriendRequestId)
    }

    @Test
    @DisplayName("특정 친구 요청 조회 요청 시 loadPort의 오류를 잘 전파한다.")
    fun getReceived_fail() {
        // given
        val testFriendRequestId = 999L
        given(friendRequestLoadPort.findReceivedRequestByRequestId(testFriendRequestId)).willThrow(
            BusinessException(ErrorCode.FRIENDSHIP_REQUEST_NOT_FOUND)
        )

        // when,then
        Assertions.assertThatThrownBy {
            friendRequestReadService.getReceivedDetail(testFriendRequestId)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.FRIENDSHIP_REQUEST_NOT_FOUND.message)
    }
}