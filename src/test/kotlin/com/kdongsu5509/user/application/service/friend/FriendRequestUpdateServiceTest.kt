package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.ErrorCode
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipSavePort
import com.kdongsu5509.user.application.port.out.friend.FriendRequestLoadPort
import com.kdongsu5509.user.application.port.out.friend.FriendRequestUpdatePort
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionSavePort
import com.kdongsu5509.user.domain.friend.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestUpdateServiceTest {

    @Mock
    lateinit var friendRequestLoadPort: FriendRequestLoadPort

    @Mock
    lateinit var friendRequestUpdatePort: FriendRequestUpdatePort


    @Mock
    lateinit var friendRelationshipSavePort: FriendRelationshipSavePort

    @Mock
    lateinit var friendRestrictionSavePort: FriendRestrictionSavePort

    @InjectMocks
    private lateinit var service: FriendRequestUpdateService

    private val userEmail = "receiver@test.com"
    private val requestId = 100L

    @BeforeEach
    fun setUp() {
        service = FriendRequestUpdateService(
            friendRequestLoadPort,
            friendRequestUpdatePort,
            friendRelationshipSavePort,
            friendRestrictionSavePort
        )
    }

    private fun createFriendRequest() = FriendRequest(
        friendRequestId = requestId,
        requester = FriendRequestUserInfo(UUID.randomUUID(), "sender@test.com", "발신자"),
        receiver = FriendRequestUserInfo(UUID.randomUUID(), userEmail, "수신자"),
        message = "친하게 지내요"
    )

    @Test
    @DisplayName("친구 요청 수락 시 관계가 저장되고 기존 요청은 삭제된다")
    fun acceptFriendRequest_success() {
        // given
        val friendRequest = createFriendRequest()
        val expectedRelationship =
            FriendRelationship(
                UUID.randomUUID(),
                friendRequest.requester.email,
                friendRequest.requester.nickname,
                LocalDateTime.now().minusDays(1)
            )

        `when`(friendRequestLoadPort.findReceivedRequestByRequestId(requestId)).thenReturn(friendRequest)
        `when`(friendRelationshipSavePort.save(friendRequest.requester, friendRequest.receiver)).thenReturn(
            expectedRelationship
        )

        // when
        val result = service.acceptFriendRequest(userEmail, requestId)

        // then
        assertEquals(expectedRelationship, result)
        verify(friendRelationshipSavePort, times(1)).save(friendRequest.requester, friendRequest.receiver)
        verify(friendRequestUpdatePort, times(1)).delete(requestId)
    }

    @Test
    @DisplayName("수신자 이메일이 일치하지 않으면 BusinessException이 발생한다")
    fun acceptFriendRequest_receiver_mismatch() {
        // given
        val friendRequest = createFriendRequest()
        val wrongEmail = "wrong@test.com"

        `when`(friendRequestLoadPort.findReceivedRequestByRequestId(requestId)).thenReturn(friendRequest)

        // when / then
        Assertions.assertThatThrownBy {
            service.acceptFriendRequest(wrongEmail, requestId)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.message)
    }

    @Test
    @DisplayName("친구 요청 거절 시 제한 기록이 생성되고 요청은 삭제된다")
    fun rejectFriendRequest_success() {
        // given
        val friendRequest = createFriendRequest()
        val expectedRestriction = FriendRestriction(
            100L,
            friendRequest.receiver.email,
            friendRequest.requester.email,
            friendRequest.requester.nickname,
            FriendRestrictionType.REJECT,
            LocalDateTime.now().minusHours(1)
        )

        `when`(friendRequestLoadPort.findReceivedRequestByRequestId(requestId)).thenReturn(friendRequest)
        `when`(
            friendRestrictionSavePort.save(
                friendRequest.requester,
                friendRequest.receiver,
                FriendRestrictionType.REJECT
            )
        )
            .thenReturn(expectedRestriction)

        // when
        val result = service.rejectFriendRequest(userEmail, requestId)

        // then
        assertEquals(expectedRestriction, result)
        verify(friendRestrictionSavePort).save(
            friendRequest.requester,
            friendRequest.receiver,
            FriendRestrictionType.REJECT
        )
        verify(friendRequestUpdatePort).delete(requestId)
    }
}