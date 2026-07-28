package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelation
import java.time.LocalDateTime
import java.util.*

/**
 * 친구 관계를 "보는 사람" 기준으로 펼친 읽기 모델.
 *
 * 애그리게이트는 두 사람을 대칭으로 들고 있어 누가 owner인지 스스로 알 수 없다.
 * 응답과 관리자 화면은 owner/friend라는 비대칭 표현을 요구하므로 여기서 관점을 붙인다.
 *
 * 애그리게이트에는 식별자만 있으므로 표시 값은 [members]로 받는다. 두 참여자가 모두 들어 있어야
 * 하고, 탈퇴 등으로 빠진 사람이 있으면 부르는 쪽이 미리 걸러야 한다.
 *
 * 프로퍼티 이름은 기존 응답 DTO와 Thymeleaf 템플릿이 그대로 참조하므로 바꾸면 안 된다.
 */
data class FriendshipView(
    val id: UUID?,
    val owner: FriendMember,
    val friend: FriendMember,
    val friendAlias: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(
            relation: FriendRelation,
            viewerId: UUID,
            members: Map<UUID, FriendMember>
        ): FriendshipView = FriendshipView(
            id = relation.id,
            owner = members.getValue(relation.pair.memberOf(viewerId)),
            friend = members.getValue(relation.getCounterpart(viewerId)),
            friendAlias = relation.getAlias(viewerId)?.value.orEmpty(),
            createdAt = relation.createdAt,
            updatedAt = relation.updatedAt
        )

        /** 관리자 목록은 특정 관점이 없다. 쌍의 정렬 순서를 그대로 쓴다. */
        fun ofAny(relation: FriendRelation, members: Map<UUID, FriendMember>): FriendshipView =
            of(relation, relation.pair.low, members)
    }
}
