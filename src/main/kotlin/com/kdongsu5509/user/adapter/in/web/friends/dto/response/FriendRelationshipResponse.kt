package com.kdongsu5509.user.adapter.`in`.web.friends.dto.response

import com.kdongsu5509.user.domain.friend.FriendRelationship
import java.util.*

data class FriendRelationshipResponse(
    val friendRelationshipId: UUID,
    val friendEmail: String,
    val friendAlias: String
) {
    companion object {
        fun fromDomain(domain: FriendRelationship): FriendRelationshipResponse {
            return FriendRelationshipResponse(
                domain.friendRelationshipId,
                domain.friendEmail,
                domain.friendAlias
            )
        }
    }
}
