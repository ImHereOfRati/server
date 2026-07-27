package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.Friendship
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
@PreAuthorize("hasRole('ADMIN')")
class FriendshipAdminService(
    private val friendshipRepository: FriendshipRepository
) {

    fun findAll(pageable: Pageable): Slice<Friendship> = friendshipRepository.findAll(pageable)

    @Transactional
    fun deleteById(id: UUID) {
        val friendship = friendshipRepository.findById(id)
            ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        friendshipRepository.delete(friendship.ownerId(), friendship.friendId())
    }
}
