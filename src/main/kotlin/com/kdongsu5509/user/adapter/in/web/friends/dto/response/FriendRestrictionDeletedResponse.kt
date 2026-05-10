package com.kdongsu5509.user.adapter.`in`.web.friends.dto.response

import com.kdongsu5509.user.domain.friend.FriendRestriction

data class FriendRestrictionDeletedResponse(
    val targetEmail: String,
) {
    companion object {
        fun fromDomain(domain: FriendRestriction): FriendRestrictionDeletedResponse {
            return FriendRestrictionDeletedResponse(
                domain.targetEmail
            )
        }
    }

}
