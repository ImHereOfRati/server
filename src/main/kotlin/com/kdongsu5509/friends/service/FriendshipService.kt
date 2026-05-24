package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.Friendship
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendshipService {
    fun findAllByOwnerEmail(ownerEmail: String, pageable: Pageable): Slice<Friendship>
    fun findAll(pageable: Pageable): Slice<Friendship>
    fun findByIdAndOwnerEmail(id: UUID, ownerEmail: String): Friendship
    fun findByOwnerEmailAndFriendId(ownerEmail: String, friendId: UUID): Friendship?
    fun updateAliasByIdAndOwnerEmail(id: UUID, ownerEmail: String, alias: String): Friendship
    fun deleteById(id: UUID)
    fun deleteByIdAndOwnerEmail(id: UUID, ownerEmail: String)
    fun blockByIdAndOwnerEmail(id: UUID, ownerEmail: String)
}
