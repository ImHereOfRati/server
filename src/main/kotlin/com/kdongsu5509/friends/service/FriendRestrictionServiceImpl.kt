package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.service.UserService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendRestrictionServiceImpl(
    private val friendRestrictionRepository: FriendRestrictionRepository,
    private val userService: UserService,
    private val friendshipRepository: FriendshipRepository,
    private val friendRequestRepository: FriendRequestRepository
) : FriendRestrictionService {

    override fun findAllByRestrictorEmail(email: String, pageable: Pageable): Slice<FriendRestriction> {
        return friendRestrictionRepository.findAllByEmail(email, pageable)
    }

    @Transactional
    override fun deleteByIdAndRestrictorEmail(id: UUID, restrictorEmail: String) {
        val found = friendRestrictionRepository.findById(id) ?: FriendException.FRI_RESTRICTION_NOT_FOUND.throwIt()
        if (!found.isOwnedBy(restrictorEmail)) FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        friendRestrictionRepository.deleteById(id)
    }

    @Transactional
    override fun unblockByRestrictorEmailAndRestrictedId(restrictorEmail: String, restrictedId: UUID) {
        friendRestrictionRepository.deleteBlockByRestrictorEmailAndRestrictedId(restrictorEmail, restrictedId)
    }

    @Transactional
    override fun restrictUser(restrictorEmail: String, targetUserId: UUID): FriendRestriction {
        val restrictor = findUserOr(FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH) {
            userService.findByEmail(restrictorEmail).toDomain()
        }
        val restricted = findUserOr(FriendException.FRIEND_RELATIONSHIP_NOT_FOUND) {
            userService.findById(targetUserId).toDomain()
        }

        val restrictorId = restrictor.id!!
        val restrictedId = restricted.id!!

        // 1. Delete Friendship
        friendshipRepository.delete(restrictorId, restrictedId)

        // 2. Delete Friend Requests between them
        friendRequestRepository.deleteBetween(restrictorId, restrictedId)

        // 3. Create Restriction
        return friendRestrictionRepository.save(
            FriendRestriction.block(
                restrictor = restrictor,
                restricted = restricted
            )
        )
    }

    override fun existRestricted(restrictorEmail: String, targetUserId: UUID): Boolean {
        val targetEmail = try {
            userService.findById(targetUserId).email
        } catch (e: ImHereBaseException) {
            return false
        }
        return friendRestrictionRepository.existsRestriction(restrictorEmail, targetEmail)
    }

    // UserService는 부재 시 USER_NOT_FOUND를 던지지만, 이 유스케이스의 기존 에러 계약은 friends 코드다.
    private inline fun findUserOr(onAbsent: FriendException, finder: () -> User): User = try {
        finder()
    } catch (e: ImHereBaseException) {
        onAbsent.throwIt()
    }
}
