package com.kdongsu5509.imhereuserservice.application.port.`in`.friend

import com.kdongsu5509.imhereuserservice.domain.friend.FriendRequest
import java.util.*

interface CreateFriendRequestUseCase {
    fun request(myEmail: String, receiverId: UUID, message: String): FriendRequest
}