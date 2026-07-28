package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.*
import com.kdongsu5509.friends.repository.FriendRelationQueryRepository
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.time.LocalDateTime
import java.util.*

class FriendRelationQueryServiceTest {

    private val friendRelationQueryRepository = mock<FriendRelationQueryRepository>()
    private val userLookupContract = mock<UserLookupContract>()

    private lateinit var friendRelationQueryService: FriendRelationQueryService
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
    private val pageable = PageRequest.of(0, 10)

    @BeforeEach
    fun setUp() {
        friendRelationQueryService = FriendRelationQueryService(
            friendRelationQueryRepository,
            FriendMemberLoader(userLookupContract)
        )
    }

    @Test
    @DisplayName("보낸 요청과 받은 요청을 구분해 조회한다")
    fun find_requests_by_view_type() {
        // given
        given(friendRelationQueryRepository.findRequests(requesterId, FriendRequestViewType.SENT, pageable))
            .willReturn(SliceImpl(listOf(storedRequest(initiatedBy = requesterId)), pageable, false))
        given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult, otherResult))

        // when
        val result = friendRelationQueryService.findRequests(requesterId, FriendRequestViewType.SENT, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].requester.id).isEqualTo(requesterId)
        assertThat(result.content[0].receiver.id).isEqualTo(otherId)
    }

    @Test
    @DisplayName("표시 정보를 채울 수 없는 관계는 목록에서 빠진다")
    fun relations_without_member_info_are_dropped() {
        // given: 상대가 탈퇴해 사용자 조회 결과에 없다.
        given(friendRelationQueryRepository.findRequests(any(), any(), any()))
            .willReturn(SliceImpl(listOf(storedRequest(initiatedBy = requesterId)), pageable, false))
        given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult))

        // when
        val result = friendRelationQueryService.findRequests(requesterId, FriendRequestViewType.SENT, pageable)

        // then
        assertThat(result.content).isEmpty()
    }

    @Test
    @DisplayName("요청 상태가 아니면 찾을 수 없다고 본다")
    fun request_not_found_when_absent() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationQueryRepository.findRequest(id)).willReturn(null)

        // when
        val exception = assertThrows<ImHereBaseException> {
            friendRelationQueryService.findRequest(id, requesterId)
        }

        // then
        assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_NOT_FOUND)
    }

    @Test
    @DisplayName("참여자가 아니면 남의 요청을 들여다볼 수 없다")
    fun outsider_cannot_read_request() {
        // given
        val id = UUID.randomUUID()
        val strangerId = UUID.randomUUID()
        given(friendRelationQueryRepository.findRequest(id))
            .willReturn(storedRequest(initiatedBy = requesterId, id = id))

        // when
        val exception = assertThrows<ImHereBaseException> {
            friendRelationQueryService.findRequest(id, strangerId)
        }

        // then
        assertThat(exception.errorCode).isEqualTo(FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH)
    }

    @Test
    @DisplayName("친구 목록은 보는 사람 기준으로 편다")
    fun friend_list_is_viewer_scoped() {
        // given
        given(friendRelationQueryRepository.findFriendships(requesterId, pageable))
            .willReturn(SliceImpl(listOf(storedFriendship()), pageable, false))
        given(userLookupContract.findAllByIds(any())).willReturn(listOf(meResult, otherResult))

        // when
        val result = friendRelationQueryService.findFriends(requesterId, pageable)

        // then: friendAlias는 보는 사람 자리에 저장된 별칭이다. 수락 시점엔 본인 닉네임으로 채워진다.
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].owner.id).isEqualTo(requesterId)
        assertThat(result.content[0].friend.id).isEqualTo(otherId)
        assertThat(result.content[0].friendAlias).isEqualTo(meResult.nickname)
    }

    @Test
    @DisplayName("자기 자신은 친구가 될 수 없으므로 저장소까지 가지 않는다")
    fun self_is_never_a_friend() {
        // when
        val result = friendRelationQueryService.findFriendByTarget(requesterId, requesterId)

        // then
        assertThat(result).isNull()
        then(friendRelationQueryRepository).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("친구 관계가 없으면 찾을 수 없다고 본다")
    fun friendship_not_found_when_absent() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationQueryRepository.findFriendshipById(id)).willReturn(null)

        // when
        val exception = assertThrows<ImHereBaseException> {
            friendRelationQueryService.findFriend(id, requesterId)
        }

        // then
        assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_NOT_FOUND)
    }

    @Test
    @DisplayName("참여자가 아니면 친구 관계를 들여다볼 수 없다")
    fun outsider_cannot_read_friendship() {
        // given
        val id = UUID.randomUUID()
        val strangerId = UUID.randomUUID()
        given(friendRelationQueryRepository.findFriendshipById(id)).willReturn(storedFriendship(id))

        // when
        val exception = assertThrows<ImHereBaseException> {
            friendRelationQueryService.findFriend(id, strangerId)
        }

        // then
        assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
    }

    @Test
    @DisplayName("제한 여부만 묻는 조회는 사용자 표시 정보를 가져오지 않는다")
    fun restriction_check_skips_member_lookup() {
        // given
        given(friendRelationQueryRepository.existsActiveRestriction(eq(requesterId), eq(otherId), any()))
            .willReturn(true)

        // when
        val result = friendRelationQueryService.existsRestriction(requesterId, otherId)

        // then
        assertThat(result).isTrue()
        then(userLookupContract).should(never()).findAllByIds(any())
    }

    @Test
    @DisplayName("자기 자신에 대한 제한 여부는 묻지 않고 없음으로 답한다")
    fun self_is_never_restricted() {
        // given: 준비할 것이 없다. 자기 자신을 묻는 것만으로 걸러져야 한다.

        // when
        val result = friendRelationQueryService.existsRestriction(requesterId, requesterId)

        // then
        assertThat(result).isFalse()
        then(friendRelationQueryRepository).shouldHaveNoInteractions()
    }

    /** 저장된 요청 행을 흉내 낸다. 쌍은 늘 (requesterId, otherId)이고 방향만 [initiatedBy]로 정한다. */
    private fun storedRequest(initiatedBy: UUID, id: UUID = UUID.randomUUID()): FriendRelation =
        FriendRelation(
            id = id,
            pair = FriendPair.of(requesterId, otherId),
            status = FriendRelationStatus.REQUESTED,
            modifierId = initiatedBy,
            message = RequestMessage(message),
            createdAt = now,
            updatedAt = now
        )

    /** 저장된 친구 관계 행. 수락 시점에 양쪽 별칭이 각자 닉네임으로 채워져 있다. */
    private fun storedFriendship(id: UUID = UUID.randomUUID()): FriendRelation =
        storedRequest(initiatedBy = otherId, id = id)
            .accept(lowAlias = meResult.nickname, highAlias = otherResult.nickname)
}
