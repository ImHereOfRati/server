package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.FriendRestriction
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendRestrictionRepository {
    fun save(friendRestriction: FriendRestriction): FriendRestriction
    fun findById(id: UUID): FriendRestriction?
    fun findAllByEmail(email: String, pageable: Pageable): Slice<FriendRestriction>
    fun findAll(pageable: Pageable): Slice<FriendRestriction>
    fun deleteById(id: UUID)
    fun deleteBlockByRestrictorEmailAndRestrictedId(restrictorEmail: String, restrictedId: UUID)
    fun deleteExpiredRestrictions()
    fun existsRestriction(requesterEmail: String, targetEmail: String): Boolean
}
