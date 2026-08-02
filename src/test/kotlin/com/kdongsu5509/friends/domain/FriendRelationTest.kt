package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.support.exception.ImHereBaseException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class FriendRelationTest {

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)

        private val ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val BOB = UUID.fromString("00000000-0000-0000-0000-000000000002")

        private const val MESSAGE = "우리지금여기서친구를해보아요"

        private fun requestedByAlice() = FriendRelation(ALICE, BOB, MESSAGE)

        private fun FriendRelation.acceptWithNicknames() = accept(lowNickname = "alice", highNickname = "bob")
    }

    @Nested
    @DisplayName("요청")
    inner class Request {

        @Test
        @DisplayName("요청을 만들면 REQUESTED 상태이고 요청자가 주체로 남는다")
        fun request_starts_as_requested() {
            val relation = requestedByAlice()

            assertThat(relation.status).isEqualTo(FriendRelationStatus.REQUESTED)
            assertThat(relation.isInitiatedBy(ALICE)).isTrue()
            assertThat(relation.initiator()).isEqualTo(ALICE)
            assertThat(relation.target()).isEqualTo(BOB)
            assertThat(relation.message?.value).isEqualTo(MESSAGE)
        }

        @Test
        @DisplayName("누가 먼저 요청하든 쌍의 정렬 순서는 같다")
        fun pair_is_normalized_regardless_of_direction() {
            val byAlice = FriendRelation(ALICE, BOB, MESSAGE)
            val byBob = FriendRelation(BOB, ALICE, MESSAGE)

            assertThat(byAlice.pair).isEqualTo(byBob.pair)
            assertThat(byAlice.pair.low).isEqualTo(ALICE)
            assertThat(byBob.pair.low).isEqualTo(ALICE)
        }

        @Test
        @DisplayName("자기 자신에게는 요청할 수 없다")
        fun cannot_request_self() {
            val exception = assertThrows<ImHereBaseException> {
                FriendRelation(ALICE, ALICE, MESSAGE)
            }
            assertThat(exception.errorCode).isEqualTo(FriendException.SELF_FRIENDSHIP)
        }

        @Test
        @DisplayName("보낸 사람에게는 SENT, 받은 사람에게는 RECEIVED로 보인다")
        fun view_type_depends_on_viewer() {
            val relation = requestedByAlice()

            assertThat(relation.viewTypeFor(ALICE)).isEqualTo(FriendRequestViewType.SENT)
            assertThat(relation.viewTypeFor(BOB)).isEqualTo(FriendRequestViewType.RECEIVED)
        }

        // "이미 관계가 있으면 새 요청을 보낼 수 없다"는 애그리게이트가 지키는 규칙이 아니다.
        // 같은 쌍의 행을 찾아보고 거부하는 것은 유스케이스의 일이라, 그 규칙은
        // FriendRelationCommandServiceTest의 request_rejected_when_* 들이 확인한다.
    }

    @Nested
    @DisplayName("수락")
    inner class Accept {

        @Test
        @DisplayName("수락하면 ACCEPTED가 되고 각자 상대 닉네임을 별칭으로 갖는다")
        fun accept_sets_both_aliases() {
            val accepted = requestedByAlice().acceptWithNicknames()

            assertThat(accepted.status).isEqualTo(FriendRelationStatus.ACCEPTED)
            // 별칭 칸은 주인이 상대를 부르는 이름이므로 low 칸에는 high의 닉네임이 들어간다.
            assertThat(accepted.lowAlias?.value).isEqualTo("bob")
            assertThat(accepted.highAlias?.value).isEqualTo("alice")
            assertThat(accepted.getAlias(ALICE)?.value).isEqualTo("bob")
            assertThat(accepted.getAlias(BOB)?.value).isEqualTo("alice")
            assertThat(accepted.message).isNull()
        }

        @Test
        @DisplayName("이미 처리된 요청은 다시 수락할 수 없다")
        fun cannot_accept_twice() {
            val accepted = requestedByAlice().acceptWithNicknames()

            val exception = assertThrows<ImHereBaseException> { accepted.acceptWithNicknames() }
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_REQUEST_ALREADY_HANDLED)
        }
    }

    @Nested
    @DisplayName("거절")
    inner class Reject {

        @Test
        @DisplayName("거절하면 거절한 쪽이 주체가 되도록 방향이 뒤집힌다")
        fun reject_flips_initiator() {
            val rejected = requestedByAlice().reject(NOW)

            assertThat(rejected.status).isEqualTo(FriendRelationStatus.REJECTED)
            assertThat(rejected.isInitiatedBy(BOB)).isTrue()
            assertThat(rejected.initiator()).isEqualTo(BOB)
            assertThat(rejected.target()).isEqualTo(ALICE)
        }

        @Test
        @DisplayName("거절은 한 달 뒤 만료된다")
        fun reject_expires_in_a_month() {
            val rejected = requestedByAlice().reject(NOW)

            assertThat(rejected.rejectionExpiredAt).isEqualTo(NOW.plusMonths(FriendRelation.REJECT_BLOCKED_MONTH))
        }

        // "만료된 거절은 새 요청을 막지 않는다"는 애그리게이트가 판정하지 않는다. 만료된 행은 정리
        // 스케줄러가 지우고, 행이 사라지면 요청이 그냥 통과한다. FriendRestrictionSchedulerTest 참조.

        @Test
        @DisplayName("이미 처리된 요청은 거절할 수 없다")
        fun cannot_reject_handled_request() {
            val rejected = requestedByAlice().reject(NOW)

            val exception = assertThrows<ImHereBaseException> { rejected.reject(NOW) }
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_REQUEST_ALREADY_HANDLED)
        }
    }

    @Nested
    @DisplayName("차단")
    inner class Block {

        @Test
        @DisplayName("친구를 차단하면 BLOCKED가 되고 별칭이 사라진다")
        fun block_clears_aliases() {
            val blocked = requestedByAlice().acceptWithNicknames().block(BOB)

            assertThat(blocked.status).isEqualTo(FriendRelationStatus.BLOCKED)
            assertThat(blocked.isInitiatedBy(BOB)).isTrue()
            assertThat(blocked.lowAlias).isNull()
            assertThat(blocked.highAlias).isNull()
        }

        @Test
        @DisplayName("차단은 만료되지 않는다")
        fun block_never_expires() {
            val blocked = requestedByAlice().acceptWithNicknames().block(BOB)

            assertThat(blocked.rejectionExpiredAt).isEqualTo(FriendRelation.PERMANENT)
        }

        @Test
        @DisplayName("관계가 없던 상대도 바로 차단할 수 있다")
        fun can_block_stranger() {
            val blocked = FriendRelation.blockWithoutRelation(ALICE, BOB)

            assertThat(blocked.status).isEqualTo(FriendRelationStatus.BLOCKED)
            assertThat(blocked.initiator()).isEqualTo(ALICE)
        }

        @Test
        @DisplayName("관계에 없는 사람은 차단할 수 없다")
        fun outsider_cannot_block() {
            val outsider = UUID.randomUUID()

            val exception = assertThrows<ImHereBaseException> { requestedByAlice().block(outsider) }
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }
    }

    @Test
    @DisplayName("취소 신청을 하면 상태만 다른 상태로 결과를 반환한다")
    fun cancel_success_return_changed_relation_with_cancel_status() {
        // given
        val requestedByAlice = requestedByAlice()
        val originHashCode = requestedByAlice.hashCode()

        // when
        val canceldRelation = requestedByAlice.cancel(ALICE)

        // then
        assertThat(canceldRelation).isNotEqualTo(originHashCode)
        assertThat(canceldRelation.status).isEqualTo(FriendRelationStatus.CANCEL)
    }

    @Nested
    @DisplayName("별칭")
    inner class Rename {

        @Test
        @DisplayName("내 별칭만 바뀌고 상대 별칭은 그대로다")
        fun rename_touches_only_my_alias() {
            val accepted = requestedByAlice().acceptWithNicknames()

            val renamed = accepted.rename(ALICE, "밥친구")

            assertThat(renamed.getAlias(ALICE)?.value).isEqualTo("밥친구")
            assertThat(renamed.getAlias(BOB)).isEqualTo(accepted.getAlias(BOB))
        }

        @Test
        @DisplayName("아직 친구가 아니면 별칭을 붙일 수 없다")
        fun cannot_rename_before_accept() {
            val exception = assertThrows<ImHereBaseException> { requestedByAlice().rename(ALICE, "밥친구") }
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIENDSHIP_NOT_ACCEPTED)
        }

        @Test
        @DisplayName("관계에 없는 사람은 별칭을 바꿀 수 없다")
        fun outsider_cannot_rename() {
            val accepted = requestedByAlice().acceptWithNicknames()

            val exception = assertThrows<ImHereBaseException> { accepted.rename(UUID.randomUUID(), "밥친구") }
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }
    }

    @Nested
    @DisplayName("관점")
    inner class Perspective {

        @Test
        @DisplayName("각자에게 상대가 누구인지 답한다")
        fun counterpart_depends_on_viewer() {
            val relation = requestedByAlice()

            assertThat(relation.getCounterpart(ALICE)).isEqualTo(BOB)
            assertThat(relation.getCounterpart(BOB)).isEqualTo(ALICE)
        }

        @Test
        @DisplayName("관계에 없는 사람으로는 상대를 물을 수 없다")
        fun outsider_has_no_counterpart() {
            val exception = assertThrows<ImHereBaseException> {
                requestedByAlice().getCounterpart(UUID.randomUUID())
            }
            assertThat(exception.errorCode).isEqualTo(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }
    }
}
