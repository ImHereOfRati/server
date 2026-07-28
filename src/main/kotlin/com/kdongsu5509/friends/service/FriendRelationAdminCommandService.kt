package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.repository.FriendRelationRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
class FriendRelationAdminCommandService(
    private val friendRelationRepository: FriendRelationRepository
) {
    fun deleteById(id: UUID) {
        friendRelationRepository.deleteById(id)
    }
}
