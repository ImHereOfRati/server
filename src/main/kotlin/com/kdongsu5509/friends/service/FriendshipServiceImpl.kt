package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.throwIt
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendshipServiceImpl(
    private val friendshipRepository: FriendshipRepository,
    private val friendRestrictionRepository: FriendRestrictionRepository
) : FriendshipService {

    override fun findAllByOwnerEmail(ownerEmail: String, pageable: Pageable): Slice<Friendship> {
        return friendshipRepository.findByOwnerEmail(ownerEmail, pageable)
    }

    override fun findByOwnerEmailAndFriendId(ownerEmail: String, friendId: UUID): Friendship? {
        return friendshipRepository.findByOwnerEmailAndFriendId(ownerEmail, friendId)
    }

    override fun findByIdAndOwnerEmail(id: UUID, ownerEmail: String): Friendship {
        val friendship = friendshipRepository.findById(id)
            ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()
        if (!friendship.isOwnedBy(ownerEmail)) {
            FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        }

        return friendship
    }

    @Transactional
    override fun updateAliasByIdAndOwnerEmail(id: UUID, ownerEmail: String, alias: String): Friendship {
        val found = findByIdAndOwnerEmail(id, ownerEmail)
        return friendshipRepository.updateAlias(found.updateFriendAlias(alias))
    }

    @Transactional
    override fun deleteByIdAndOwnerEmail(id: UUID, ownerEmail: String) {
        val friendship = findByIdAndOwnerEmail(id, ownerEmail)
        friendshipRepository.delete(friendship.ownerId(), friendship.friendId())
    }

    @Transactional
    override fun blockByIdAndOwnerEmail(id: UUID, ownerEmail: String) {
        val friendship = findByIdAndOwnerEmail(id, ownerEmail)

        friendRestrictionRepository.save(
            FriendRestriction.block(
                restrictor = friendship.owner,
                restricted = friendship.friend
            )
        )
        friendshipRepository.delete(friendship.ownerId(), friendship.friendId())
    }
}
