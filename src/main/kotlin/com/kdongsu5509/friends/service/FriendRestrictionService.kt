package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendRestriction
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendRestrictionService {
    fun findAllByRestrictorEmail(email: String, pageable: Pageable): Slice<FriendRestriction>
    fun findAll(pageable: Pageable): Slice<FriendRestriction>
    fun deleteById(id: UUID)
    fun deleteByIdAndRestrictorEmail(id: UUID, restrictorEmail: String)
    fun unblockByRestrictorEmailAndRestrictedId(restrictorEmail: String, restrictedId: UUID)
    fun restrictUser(restrictorEmail: String, targetUserId: UUID): FriendRestriction
    fun existRestricted(restrictorEmail: String, targetUserId: UUID): Boolean
}
