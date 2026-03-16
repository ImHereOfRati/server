package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.application.port.`in`.friend.UpdateFriendsUseCase
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipLoadPort
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipUpdatePort
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionSavePort
import com.kdongsu5509.user.application.port.out.user.UserLoadPort
import com.kdongsu5509.user.domain.friend.FriendRelationship
import com.kdongsu5509.user.domain.friend.FriendRequestUserInfo
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional
class FriendRelationshipUpdateService(
    private val friendRelationshipUpdatePort: FriendRelationshipUpdatePort,
    private val friendRelationshipLoadPort: FriendRelationshipLoadPort,
    private val friendRestrictionSavePort: FriendRestrictionSavePort,
    private val userLoadPort: UserLoadPort,
) : UpdateFriendsUseCase {
    override fun changeFriendAlias(
        userEmail: String,
        friendRelationshipId: UUID,
        newFriendAlias: String
    ): FriendRelationship {
        return friendRelationshipUpdatePort.updateAlias(
            userEmail, friendRelationshipId, newFriendAlias
        )
    }

    override fun block(userEmail: String, friendRelationshipId: UUID) {
        val relationship = friendRelationshipLoadPort.findFriendRelationshipByRelationshipId(friendRelationshipId)

        val owner = userLoadPort.findActiveUserByEmailOrNull(userEmail)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        val target = userLoadPort.findActiveUserByEmailOrNull(relationship.friendEmail)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        friendRestrictionSavePort.save(
            FriendRequestUserInfo(
                id = target.id!!,
                email = target.email,
                nickname = target.nickname
            ),
            FriendRequestUserInfo(
                id = owner.id!!,
                email = owner.email,
                nickname = owner.nickname
            ),
            FriendRestrictionType.BLOCK
        )

        friendRelationshipUpdatePort.delete(userEmail, friendRelationshipId)
    }

    override fun deleteRelationship(userEmail: String, friendRelationshipId: UUID) {
        friendRelationshipUpdatePort.delete(userEmail, friendRelationshipId)
    }
}