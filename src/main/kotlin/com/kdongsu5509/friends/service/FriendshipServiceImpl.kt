package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.throwIt
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.security.access.prepost.PreAuthorize
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

    @PreAuthorize("hasRole('ADMIN')")
    override fun findAll(pageable: Pageable): Slice<Friendship> {
        return friendshipRepository.findAll(pageable)
    }

    override fun findByOwnerEmailAndFriendId(ownerEmail: String, friendId: UUID): Friendship? {
        return friendshipRepository.findByOwnerEmailAndFriendId(ownerEmail, friendId)
    }

    override fun findByIdAndOwnerEmail(id: UUID, ownerEmail: String): Friendship {
        val friendship = friendshipRepository.findById(id)
            ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()
        if (friendship.owner.email != ownerEmail) {
            FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        }

        return friendship
    }

    @Transactional
    override fun updateAliasByIdAndOwnerEmail(id: UUID, ownerEmail: String, alias: String): Friendship {
        val found = findByIdAndOwnerEmail(id, ownerEmail)
        return friendshipRepository.updateAlias(found.updateFriendAlias(alias))
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    override fun deleteById(id: UUID) {
        val friendship = friendshipRepository.findById(id)
            ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        friendshipRepository.delete(friendship.owner.id!!, friendship.friend.id!!)
    }

    @Transactional
    override fun deleteByIdAndOwnerEmail(id: UUID, ownerEmail: String) {
        val friendship = findByIdAndOwnerEmail(id, ownerEmail)
        friendshipRepository.delete(friendship.owner.id!!, friendship.friend.id!!)
    }

    @Transactional
    override fun blockByIdAndOwnerEmail(id: UUID, ownerEmail: String) {
        val friendship = findByIdAndOwnerEmail(id, ownerEmail)

        friendRestrictionRepository.save(
            FriendRestriction(
                restrictor = friendship.owner,
                restricted = friendship.friend,
                type = FriendRestrictionType.BLOCK
            )
        )
        friendshipRepository.delete(friendship.owner.id!!, friendship.friend.id!!)
    }
}
