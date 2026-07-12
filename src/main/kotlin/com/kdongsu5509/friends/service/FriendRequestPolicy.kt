package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.service.dto.UserResult

class FriendRequestPolicy(
    private val friendRequestRepository: FriendRequestRepository,
    private val friendRestrictionRepository: FriendRestrictionRepository,
    private val friendshipRepository: FriendshipRepository,
) {

    fun verifyRequestable(me: UserResult, target: UserResult) {
        verifyNotRestricted(me.email, target.email)
        verifyNotAlreadyRequested(me, target)
        verifyNotAlreadyFriend(me, target)
    }

    private fun verifyNotAlreadyFriend(me: UserResult, target: UserResult) {
        if (friendshipRepository.existsByOwnerUserIdAndFriendUserId(me.id, target.id))
            FriendException.ALREADY_FRIEND.throwIt()
    }

    private fun verifyNotAlreadyRequested(me: UserResult, target: UserResult) {
        if (friendRequestRepository.existsByRequesterIdAndReceiverId(me.id, target.id))
            FriendException.FRIEND_REQUEST_ALREADY_SENT.throwIt()
    }

    private fun verifyNotRestricted(requesterEmail: String, targetEmail: String) {
        if (friendRestrictionRepository.existsRestriction(requesterEmail, targetEmail)) {
            FriendException.FRIEND_REQUEST_UNPROCESSABLE_BY_ME.throwIt()
        }

        if (friendRestrictionRepository.existsRestriction(targetEmail, requesterEmail)) {
            FriendException.FRIEND_REQUEST_UNPROCESSABLE_BY_TARGET.throwIt()
        }
    }
}
