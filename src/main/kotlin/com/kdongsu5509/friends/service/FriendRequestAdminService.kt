package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.FriendRequestRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
class FriendRequestAdminService(
    private val friendRequestRepository: FriendRequestRepository
) {

    fun findAll(pageable: Pageable): Slice<FriendRequest> = friendRequestRepository.findAll(pageable)

    @Transactional
    fun deleteById(id: UUID) {
        friendRequestRepository.deleteById(id)
    }
}
