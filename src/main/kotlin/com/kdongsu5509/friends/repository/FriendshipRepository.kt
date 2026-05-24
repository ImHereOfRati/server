package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.Friendship
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendshipRepository {
    fun findById(id: UUID): Friendship?
    fun findByOwnerEmail(email: String, pageable: Pageable): Slice<Friendship>
    fun findAll(pageable: Pageable): Slice<Friendship>
    fun findByOwnerEmailAndFriendId(ownerEmail: String, friendId: UUID): Friendship?
    fun delete(ownerId: UUID, friendId: UUID)
    fun updateAlias(newFriendship: Friendship): Friendship
    fun save(friendship: Friendship): Friendship
    fun existsByOwnerUserIdAndFriendUserId(ownerId: UUID, friendId: UUID): Boolean
}
