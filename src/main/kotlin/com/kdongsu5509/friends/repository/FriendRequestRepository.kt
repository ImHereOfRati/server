package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.FriendRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendRequestRepository {
    fun save(friendRequest: FriendRequest): FriendRequest
    fun findAll(pageable: Pageable): Slice<FriendRequest>
    fun findAllByReceiverEmail(email: String, pageable: Pageable): Slice<FriendRequest>
    fun findAllByRequesterEmail(email: String, pageable: Pageable): Slice<FriendRequest>
    fun findById(id: UUID): FriendRequest?
    fun deleteById(id: UUID)
    fun deleteBetween(user1Id: UUID, user2Id: UUID)
    fun existsByRequesterIdAndReceiverId(requesterId: UUID, receiverId: UUID): Boolean
}
