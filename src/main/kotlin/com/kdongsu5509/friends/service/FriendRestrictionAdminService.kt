package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendRestriction
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendRestrictionAdminService {
    fun findAll(pageable: Pageable): Slice<FriendRestriction>
    fun deleteById(id: UUID)
}
