package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendRequestAdminService {
    fun findAll(pageable: Pageable): Slice<FriendRequest>
    fun deleteById(id: UUID)
}
