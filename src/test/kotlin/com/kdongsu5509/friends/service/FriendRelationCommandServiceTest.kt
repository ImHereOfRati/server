package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendPair
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.domain.RequestMessage
import com.kdongsu5509.friends.repository.FriendRelationRepository
import com.kdongsu5509.shared.notification.NotificationPort
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

class FriendRelationCommandServiceTest {

    private val friendRelationRepository = mock<FriendRelationRepository>()
    private val userLookupContract = mock<UserLookupContract>()
    private val notificationPort = mock<NotificationPort>()

    private lateinit var friendRelationCommandService: FriendRelationCommandService

    /** 쌍은 (low, high)로 정규화되므로 어느 쪽이 low인지 고정해야 별칭 자리를 단언할 수 있다. */
    private val requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val otherId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val meResult = UserResult(
        id = requesterId,
        email = "me@example.com",
        nickname = "me",
        oauthProvider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = UserStatus.ACTIVE
    )
    private val otherResult = UserResult(
        id = otherId,
        email = "other@example.com",
        nickname = "other",
        oauthProvider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = UserStatus.ACTIVE
    )

    private val message = "친구가 되고 싶습니다"
    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)

    @BeforeEach
    fun setUp() {
        friendRelationCommandService = FriendRelationCommandService(
            friendRelationRepository,
            FriendMemberLoader(userLookupContract),
            userLookupContract,
            notificationPort
        )
    }

    @Nested
    @DisplayName("요청")
    inner class Request {

        @Test
        @DisplayName("관계가 없으면 REQUESTED 관계를 저장하고 알림을 보낸다")
        fun request_saves_and_notifies() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId)).willReturn(null)
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            val result = friendRelationCommandService.sendRequest(requesterId, otherId, message)

            // then
            assertThat(result.requester.id).isEqualTo(requesterId)
            assertThat(result.receiver.id).isEqualTo(otherId)
            assertThat(result.message).isEqualTo(message)
            then(notificationPort).should().send(any())
        }

        @Test
        @DisplayName("이미 친구면 저장하지 않고 예외를 던진다")
        fun request_rejected_when_already_friend() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(FriendRelation(requesterId, otherId, message).accept(lowAlias = "me", highAlias = "other"))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.sendRequest(requesterId, otherId, message)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.ALREADY_FRIEND)
            then(friendRelationRepository).should(never()).save(any())
            then(notificationPort).shouldHaveNoInteractions()
        }

        @Test
        @DisplayName("내가 막은 상대에게는 요청할 수 없다")
        fun request_rejected_when_blocked_by_me() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(FriendRelation.blockWithoutRelation(requesterId, otherId))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.sendRequest(requesterId, otherId, message)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_REQUEST_UNPROCESSABLE)
        }

        @Test
        @DisplayName("나를 막은 상대에게는 요청할 수 없다")
        fun request_rejected_when_blocked_by_target() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(FriendRelation.blockWithoutRelation(otherId, requesterId))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.sendRequest(requesterId, otherId, message)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_REQUEST_UNPROCESSABLE)
        }

        @Test
        @DisplayName("이미 보낸 요청이 있으면 다시 보낼 수 없다")
        fun request_rejected_when_already_sent() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(FriendRelation(requesterId, otherId, message))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.sendRequest(requesterId, otherId, message)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_REQUEST_ALREADY_SENT)
        }

        @Test
        @DisplayName("거절 기록이 남아 있으면 만료 여부와 무관하게 요청할 수 없다")
        fun request_rejected_while_rejection_row_remains() {
            // given: 두 달 전 거절이라 이미 만료됐지만 정리 스케줄러가 아직 행을 지우지 않았다.
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(FriendRelation(requesterId, otherId, message).reject(now.minusMonths(2)))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.sendRequest(requesterId, otherId, message)
            }

            // then: 행이 사라져야 다시 보낼 수 있다. 그 삭제는 FriendRestrictionScheduler가 한다.
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_REQUEST_UNPROCESSABLE)
        }

        @Test
        @DisplayName("정리 스케줄러가 거절 행을 지운 뒤에는 다시 요청할 수 있다")
        fun request_allowed_after_rejection_row_removed() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId)).willReturn(null)
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            val result = friendRelationCommandService.sendRequest(requesterId, otherId, message)

            // then
            assertThat(result.requester.id).isEqualTo(requesterId)
        }
    }

    @Nested
    @DisplayName("수락과 거절")
    inner class AcceptAndReject {

        @Test
        @DisplayName("받은 요청을 수락하면 ACCEPTED로 저장하고 알림을 보낸다")
        fun accept_received_request() {
            // given
            val id = UUID.randomUUID()
            given(friendRelationRepository.findById(id))
                .willReturn(storedRequest(id, requesterId = otherId, receiverId = requesterId))
            given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult, otherResult))
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            val result = friendRelationCommandService.acceptRequest(id, requesterId)

            // then
            assertThat(result.owner.id).isEqualTo(requesterId)
            assertThat(result.friend.id).isEqualTo(otherId)
            then(notificationPort).should().send(any())
        }

        @Test
        @DisplayName("수락하면 양쪽 별칭이 각 자리의 닉네임으로 채워진다")
        fun accept_fills_aliases_with_nicknames() {
            // given
            val id = UUID.randomUUID()
            given(friendRelationRepository.findById(id))
                .willReturn(storedRequest(id, requesterId = otherId, receiverId = requesterId))
            given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult, otherResult))
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            friendRelationCommandService.acceptRequest(id, requesterId)

            // then: meId가 low라 low 자리에 me의 닉네임이 들어간다.
            val saved = argumentCaptor<FriendRelation>()
            then(friendRelationRepository).should().save(saved.capture())
            assertThat(saved.firstValue.lowAlias?.value).isEqualTo(meResult.nickname)
            assertThat(saved.firstValue.highAlias?.value).isEqualTo(otherResult.nickname)
        }

        @Test
        @DisplayName("내가 보낸 요청은 내가 수락할 수 없다")
        fun cannot_accept_own_request() {
            // given
            val id = UUID.randomUUID()
            given(friendRelationRepository.findById(id))
                .willReturn(storedRequest(id, requesterId = requesterId, receiverId = otherId))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.acceptRequest(id, requesterId)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH)
        }

        @Test
        @DisplayName("거절하면 거절한 쪽이 제한 주체가 된다")
        fun reject_makes_receiver_the_restrictor() {
            // given
            val id = UUID.randomUUID()
            given(friendRelationRepository.findById(id))
                .willReturn(storedRequest(id, requesterId = otherId, receiverId = requesterId))
            given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult, otherResult))
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            val result = friendRelationCommandService.rejectRequest(id, requesterId)

            // then
            assertThat(result.restrictor.id).isEqualTo(requesterId)
            assertThat(result.restricted.id).isEqualTo(otherId)
        }

        @Test
        @DisplayName("요청 상태가 아니면 찾을 수 없다고 본다")
        fun non_requested_relation_is_not_found() {
            // given
            val id = UUID.randomUUID()
            given(friendRelationRepository.findById(id)).willReturn(
                storedRequest(id, requesterId = otherId, receiverId = requesterId)
                    .accept(lowAlias = "me", highAlias = "other")
            )

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.acceptRequest(id, requesterId)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("친구")
    inner class Friendship {

        @Test
        @DisplayName("별칭 변경은 내 자리 별칭만 바꾼다")
        fun rename_changes_only_my_alias() {
            // given
            val id = UUID.randomUUID()
            given(friendRelationRepository.findById(id))
                .willReturn(storedFriendship(id))
            given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult, otherResult))
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            val result = friendRelationCommandService.updateAlias(id, requesterId, "단짝")

            // then
            assertThat(result.friendAlias).isEqualTo("단짝")
        }

        @Test
        @DisplayName("참여자가 아니면 친구 관계를 다룰 수 없다")
        fun outsider_cannot_touch_friendship() {
            // given
            val id = UUID.randomUUID()
            val strangerId = UUID.randomUUID()
            given(friendRelationRepository.findById(id)).willReturn(storedFriendship(id))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.deleteFriendship(id, strangerId)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }
    }

    /**
     * 관계를 끊는 방법은 거절과 차단 둘뿐이다.
     *
     * 차단은 친구든 아니든 하나의 연산으로 처리한다. 예전에는 "친구 차단"과 "사용자 차단"이
     * 나뉘어 있었지만 결과 상태가 같아서 합쳤다.
     */
    @Nested
    @DisplayName("차단")
    inner class Block {

        @Test
        @DisplayName("관계가 없던 상대를 차단하면 차단 관계를 새로 만든다")
        fun block_stranger_creates_relation() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId)).willReturn(null)
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            val result = friendRelationCommandService.block(requesterId, otherId)

            // then
            assertThat(result.restrictor.id).isEqualTo(requesterId)
            assertThat(result.restricted.id).isEqualTo(otherId)
        }

        @Test
        @DisplayName("기존 친구를 차단하면 같은 관계를 BLOCKED로 전이시킨다")
        fun block_existing_friend_transitions() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(storedFriendship(UUID.randomUUID()))
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            friendRelationCommandService.block(requesterId, otherId)

            // then
            val saved = argumentCaptor<FriendRelation>()
            then(friendRelationRepository).should().save(saved.capture())
            assertThat(saved.firstValue.status).isEqualTo(FriendRelationStatus.BLOCKED)
            assertThat(saved.firstValue.isInitiatedBy(requesterId)).isTrue()
        }

        @Test
        @DisplayName("오가던 요청이 있어도 같은 관계를 BLOCKED로 전이시킨다")
        fun block_pending_request_transitions() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId)).willReturn(otherResult)
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(storedRequest(UUID.randomUUID(), requesterId = otherId, receiverId = requesterId))
            given(friendRelationRepository.save(any())).willAnswer { it.arguments[0] as FriendRelation }

            // when
            friendRelationCommandService.block(requesterId, otherId)

            // then
            val saved = argumentCaptor<FriendRelation>()
            then(friendRelationRepository).should().save(saved.capture())
            assertThat(saved.firstValue.status).isEqualTo(FriendRelationStatus.BLOCKED)
            assertThat(saved.firstValue.message).isNull()
        }

        @Test
        @DisplayName("자기 자신은 차단할 수 없다")
        fun cannot_block_self() {
            // given: 같은 쌍은 저장될 수 없어 저장소는 없다고 답하고, 판정은 애그리게이트가 한다.
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(friendRelationRepository.findByPair(requesterId, requesterId)).willReturn(null)

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.block(requesterId, requesterId)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.SELF_BLOCK)
            then(friendRelationRepository).should(never()).save(any())
        }

        @Test
        @DisplayName("차단 대상이 없으면 대상을 찾을 수 없다고 알린다")
        fun block_target_not_found() {
            // given
            given(userLookupContract.findById(requesterId)).willReturn(meResult)
            given(userLookupContract.findById(otherId))
                .willThrow(ImHereBaseException(FriendException.FRIEND_RELATIONSHIP_NOT_FOUND))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.block(requesterId, otherId)
            }

            // then: 관계가 아니라 사용자가 없다는 사실이 그대로 드러나야 한다.
            assertThat(exception.errorCode).isEqualTo(FriendException.BLOCK_TARGET_NOT_FOUND)
            assertThat(exception.contextData).containsEntry("targetUserId", otherId)
        }

        @Test
        @DisplayName("차단 해제는 내가 건 차단만 지운다")
        fun unblock_only_my_block() {
            // given: 상대가 나를 차단한 관계다.
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(
                    FriendRelation.blockWithoutRelation(otherId, requesterId)
                        .block(otherId)
                )

            // when, then
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.unblock(requesterId, otherId)
            }

            // then: 관계가 아니라 사용자가 없다는 사실이 그대로 드러나야 한다.
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }

        @Test
        @DisplayName("차단 해제는 거절 기록을 건드리지 않는다")
        fun unblock_leaves_rejection_alone() {
            // given: 거절은 차단이 아니다. 한 달 뒤 만료되어 정리 스케줄러가 지우는 기록이다.
            given(friendRelationRepository.findByPair(requesterId, otherId))
                .willReturn(FriendRelation(otherId, requesterId, message).reject(now))

            // when
            val exception = assertThrows<ImHereBaseException> {
                friendRelationCommandService.unblock(requesterId, otherId)
            }

            // then
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIENDSHIP_UNBLOCKED)
            then(friendRelationRepository).should(never()).deleteById(any())
        }
    }

    /** 저장된 요청 행을 흉내 낸다. 식별자가 있어야 유스케이스가 삭제·전이를 걸 수 있다. */
    private fun storedRequest(id: UUID, requesterId: UUID, receiverId: UUID): FriendRelation =
        FriendRelation(
            id = id,
            pair = FriendPair.of(requesterId, receiverId),
            status = FriendRelationStatus.REQUESTED,
            modifierId = requesterId,
            message = RequestMessage(message),
            createdAt = now,
            updatedAt = now
        )

    /** 저장된 친구 관계 행. 수락 시점에 양쪽 별칭이 각자 닉네임으로 채워져 있다. */
    private fun storedFriendship(id: UUID): FriendRelation =
        storedRequest(id, requesterId = otherId, receiverId = requesterId)
            .accept(lowAlias = meResult.nickname, highAlias = otherResult.nickname)
}
