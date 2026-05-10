package com.kdongsu5509.user.adapter.`in`.web.friends.dto.response

import com.kdongsu5509.user.domain.friend.FriendRestriction
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import java.time.LocalDateTime

data class FriendRestrictionResponse(
    val friendRestrictionId: Long,
    val targetEmail: String,
    val targetNickname: String,
    val restrictionType: FriendRestrictionType,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun fromDomain(domain: FriendRestriction): FriendRestrictionResponse {
            return FriendRestrictionResponse(
                domain.friendRestrictionId,
                domain.targetEmail,
                domain.targetNickname,
                domain.restrictionType,
                domain.createdAt
            )
        }
    }

}
