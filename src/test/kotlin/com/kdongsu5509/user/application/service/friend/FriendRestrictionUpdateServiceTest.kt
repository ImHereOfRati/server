package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.ErrorCode
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionLoadPort
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionUpdatePort
import com.kdongsu5509.user.domain.friend.FriendRestriction
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FriendRestrictionUpdateServiceTest {

    companion object {
        const val TEST_RESTRICTION_ID = 1L
        const val TEST_ACTOR_EMAIL = "actor@actor.com"
        const val TEST_TARGET_EMAIL = "target@target.com"
        const val TEST_NICKNAME = "테스터"
    }

    @Mock
    private lateinit var friendRestrictionLoadPort: FriendRestrictionLoadPort

    @Mock
    private lateinit var friendRestrictionUpdatePort: FriendRestrictionUpdatePort

    @InjectMocks
    private lateinit var friendRestrictionUpdateService: FriendRestrictionUpdateService

    @Test
    @DisplayName("성공: 친구 제한 삭제 요청 시 데이터를 삭제하고 정보를 반환한다")
    fun deleteRestriction_success() {
        // given
        val fixture = createTestFriendRestriction()
        given(friendRestrictionLoadPort.loadById(TEST_RESTRICTION_ID))
            .willReturn(fixture)
        willDoNothing().given(friendRestrictionUpdatePort).delete(TEST_RESTRICTION_ID)

        // when
        val result = friendRestrictionUpdateService.deleteRestriction(TEST_ACTOR_EMAIL, TEST_RESTRICTION_ID)

        // then
        assertThat(result).isNotNull
        assertThat(result.targetEmail).isEqualTo(TEST_TARGET_EMAIL)
        assertThat(result.actorEmail).isEqualTo(TEST_ACTOR_EMAIL)

        verify(friendRestrictionLoadPort, times(1)).loadById(TEST_RESTRICTION_ID)
        verify(friendRestrictionUpdatePort, times(1)).delete(TEST_RESTRICTION_ID)
    }

    @Test
    @DisplayName("요청자 이메일과 데이터의 소유자 이메일이 다르면 예외가 발생한다")
    fun deleteRestriction_fail_emailMismatch() {
        // given
        val wrongEmail = "wrong@wrong.com"
        val fixture = createTestFriendRestriction()

        given(friendRestrictionLoadPort.loadById(TEST_RESTRICTION_ID))
            .willReturn(fixture)

        // when & then
        // 메시지 대신 에러 코드 자체를 비교 (추천)
        assertThatThrownBy {
            friendRestrictionUpdateService.deleteRestriction(wrongEmail, TEST_RESTRICTION_ID)
        }
            .isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.FRIEND_RESTRICTION_ACTOR_MISS_MATCH.message)
        verify(friendRestrictionUpdatePort, never()).delete(anyLong())
    }

    private fun createTestFriendRestriction() = FriendRestriction(
        TEST_RESTRICTION_ID,
        TEST_ACTOR_EMAIL,
        TEST_TARGET_EMAIL,
        TEST_NICKNAME,
        FriendRestrictionType.BLOCK,
        LocalDateTime.now().minusHours(1)
    )
}