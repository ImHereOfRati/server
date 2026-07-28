package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelation
import java.time.LocalDateTime
import java.util.*

/**
 * 거절·차단 읽기 모델.
 *
 * 제한도 방향이 분명해서 제한을 건 쪽이 restrictor, 당한 쪽이 restricted로 고정된다.
 *
 * 애그리게이트에는 식별자만 있으므로 표시 값은 [members]로 받는다.
 *
 * 프로퍼티 이름은 기존 응답 DTO와 Thymeleaf 템플릿이 그대로 참조하므로 바꾸면 안 된다.
 */
data class FriendRestrictionView(
    val id: UUID?,
    val restrictor: FriendMember,
    val restricted: FriendMember,
    val type: FriendRestrictionType,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val expiredAt: LocalDateTime?
) {
    companion object {
        fun of(relation: FriendRelation, members: Map<UUID, FriendMember>): FriendRestrictionView =
            FriendRestrictionView(
                id = relation.id,
                restrictor = members.getValue(relation.initiator()),
                restricted = members.getValue(relation.target()),
                type = FriendRestrictionType.from(relation.status),
                createdAt = relation.createdAt,
                updatedAt = relation.updatedAt,
                expiredAt = relation.rejectionExpiredAt
            )
    }
}
