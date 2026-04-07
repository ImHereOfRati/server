package com.kdongsu5509.user.application.port.`in`.friend

import com.kdongsu5509.user.domain.friend.FriendRequest
import java.util.*

interface CreateFriendRequestUseCase {
    fun request(
        myEmail: String,
        myNickname: String,
        receiverId: UUID,
        receiverEmail: String,
        message: String
    ): FriendRequest
}
