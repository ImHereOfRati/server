package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.Friendship
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendshipAdminService {
    fun findAll(pageable: Pageable): Slice<Friendship>
    fun deleteById(id: UUID)
}
