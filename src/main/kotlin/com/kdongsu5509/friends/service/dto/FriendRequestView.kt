package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelation
import java.time.LocalDateTime
import java.util.*

/**
 * 친구 요청 읽기 모델.
 *
 * 요청은 방향이 분명해서 보는 사람과 무관하게 requester/receiver가 정해진다.
 * 방향은 애그리게이트의 initiatedUserId가 들고 있다.
 *
 * 애그리게이트에는 식별자만 있으므로 표시 값은 [members]로 받는다.
 *
 * 프로퍼티 이름은 기존 응답 DTO와 Thymeleaf 템플릿이 그대로 참조하므로 바꾸면 안 된다.
 */
data class FriendRequestView(
    val id: UUID?,
    val requester: FriendMember,
    val receiver: FriendMember,
    val message: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(relation: FriendRelation, members: Map<UUID, FriendMember>): FriendRequestView =
            FriendRequestView(
                id = relation.id,
                requester = members.getValue(relation.initiator()),
                receiver = members.getValue(relation.target()),
                message = relation.message?.value.orEmpty(),
                createdAt = relation.createdAt,
                updatedAt = relation.updatedAt
            )
    }
}
