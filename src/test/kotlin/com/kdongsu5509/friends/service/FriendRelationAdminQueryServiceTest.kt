package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendPair
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.domain.RequestMessage
import com.kdongsu5509.friends.repository.FriendRelationQueryRepository
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.time.LocalDateTime
import java.util.*

class FriendRelationAdminQueryServiceTest {

    private val friendRelationQueryRepository = mock<FriendRelationQueryRepository>()
    private val userLookupContract = mock<UserLookupContract>()

    private lateinit var friendRelationAdminQueryService: FriendRelationAdminQueryService

    private val lowId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val highId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val lowResult = UserResult(
        id = lowId,
        email = "low@example.com",
        nickname = "low",
        oauthProvider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = UserStatus.ACTIVE
    )
    private val highResult = UserResult(
        id = highId,
        email = "high@example.com",
        nickname = "high",
        oauthProvider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = UserStatus.ACTIVE
    )

    private val message = "친구가 되고 싶습니다"
    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)
    private val pageable = PageRequest.of(0, 10)

    @BeforeEach
    fun setUp() {
        friendRelationAdminQueryService = FriendRelationAdminQueryService(
            friendRelationQueryRepository,
            FriendMemberLoader(userLookupContract)
        )
    }

    @Test
    @DisplayName("요청 목록은 요청 전용 조회로 간다")
    fun findAllRequests_delegates() {
        // given
        given(friendRelationQueryRepository.findAllRequests(pageable))
            .willReturn(SliceImpl(listOf(storedRequest()), pageable, false))
        given(userLookupContract.findAllByIds(any())).willReturn(listOf(lowResult, highResult))

        // when
        val result = friendRelationAdminQueryService.findAllRequests(pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].requester.id).isEqualTo(lowId)
        assertThat(result.content[0].receiver.id).isEqualTo(highId)
    }

    @Test
    @DisplayName("친구 목록은 쌍의 정렬 순서를 관점으로 쓴다")
    fun findAllFriendships_delegates() {
        // given
        given(friendRelationQueryRepository.findAllFriendships(pageable))
            .willReturn(
                SliceImpl(
                    listOf(storedRequest().accept(lowAlias = "low", highAlias = "high")),
                    pageable,
                    false
                )
            )
        given(userLookupContract.findAllByIds(any())).willReturn(listOf(lowResult, highResult))

        // when
        val result = friendRelationAdminQueryService.findAllFriendships(pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].owner.id).isEqualTo(lowId)
        assertThat(result.content[0].friend.id).isEqualTo(highId)
    }

    @Test
    @DisplayName("제한 목록은 거절과 차단을 함께 조회한다")
    fun findAllRestrictions_delegates() {
        // given
        given(friendRelationQueryRepository.findAllRestrictions(pageable))
            .willReturn(SliceImpl(listOf(FriendRelation.blockWithoutRelation(lowId, highId)), pageable, false))
        given(userLookupContract.findAllByIds(any())).willReturn(listOf(lowResult, highResult))

        // when
        val result = friendRelationAdminQueryService.findAllRestrictions(pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].restrictor.id).isEqualTo(lowId)
        assertThat(result.content[0].restricted.id).isEqualTo(highId)
    }

    /** 저장된 요청 행을 흉내 낸다. 식별자와 감사 시각이 있어야 View 변환이 성립한다. */
    private fun storedRequest(): FriendRelation = FriendRelation(
        id = UUID.randomUUID(),
        pair = FriendPair.of(lowId, highId),
        status = FriendRelationStatus.REQUESTED,
        modifierId = lowId,
        message = RequestMessage(message),
        createdAt = now,
        updatedAt = now
    )
}
