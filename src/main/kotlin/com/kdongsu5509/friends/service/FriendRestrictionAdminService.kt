package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
class FriendRestrictionAdminService(
    private val friendRestrictionRepository: FriendRestrictionRepository
) {

    fun findAll(pageable: Pageable): Slice<FriendRestriction> = friendRestrictionRepository.findAll(pageable)

    @Transactional
    fun deleteById(id: UUID) {
        friendRestrictionRepository.deleteById(id)
    }
}
