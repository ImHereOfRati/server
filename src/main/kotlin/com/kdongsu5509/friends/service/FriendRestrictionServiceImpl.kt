package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.repository.UserDao
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendRestrictionServiceImpl(
    private val friendRestrictionRepository: FriendRestrictionRepository,
    private val userDao: UserDao,
    private val friendshipRepository: FriendshipRepository,
    private val friendRequestRepository: FriendRequestRepository
) : FriendRestrictionService {

    override fun findAllByRestrictorEmail(email: String, pageable: Pageable): Slice<FriendRestriction> {
        return friendRestrictionRepository.findAllByEmail(email, pageable)
    }

    @PreAuthorize("hasRole('ADMIN')")
    override fun findAll(pageable: Pageable): Slice<FriendRestriction> {
        return friendRestrictionRepository.findAll(pageable)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    override fun deleteById(id: UUID) {
        friendRestrictionRepository.deleteById(id)
    }

    @Transactional
    override fun deleteByIdAndRestrictorEmail(id: UUID, restrictorEmail: String) {
        val found = friendRestrictionRepository.findById(id) ?: FriendException.FRI_RESTRICTION_NOT_FOUND.throwIt()
        if (found.restrictor.email != restrictorEmail) FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        friendRestrictionRepository.deleteById(id)
    }

    @Transactional
    override fun unblockByRestrictorEmailAndRestrictedId(restrictorEmail: String, restrictedId: UUID) {
        friendRestrictionRepository.deleteBlockByRestrictorEmailAndRestrictedId(restrictorEmail, restrictedId)
    }

    @Transactional
    override fun restrictUser(restrictorEmail: String, targetUserId: UUID): FriendRestriction {
        val restrictor =
            userDao.findByEmail(restrictorEmail) ?: FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        val restricted = userDao.findById(targetUserId) ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        val restrictorId = restrictor.id!!
        val restrictedId = restricted.id!!

        // 1. Delete Friendship
        friendshipRepository.delete(restrictorId, restrictedId)

        // 2. Delete Friend Requests between them
        friendRequestRepository.deleteBetween(restrictorId, restrictedId)

        // 3. Create Restriction
        return friendRestrictionRepository.save(
            FriendRestriction(
                restrictor = restrictor,
                restricted = restricted,
                type = FriendRestrictionType.BLOCK
            )
        )
    }

    override fun existRestricted(restrictorEmail: String, targetUserId: UUID): Boolean {
        val targetUser = userDao.findById(targetUserId) ?: return false
        return friendRestrictionRepository.existsRestriction(restrictorEmail, targetUser.email)
    }
}
