package com.kdongsu5509.imhereuserservice.application.service.friend

import com.kdongsu5509.imhereuserservice.application.port.`in`.friend.CreateFriendRequestUseCase
import com.kdongsu5509.imhereuserservice.application.port.out.friend.FriendRequestSavePort
import com.kdongsu5509.imhereuserservice.domain.friend.FriendRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional
class FriendRequestCreateService(
    private val friendRequestSavePort: FriendRequestSavePort
) : CreateFriendRequestUseCase {

    override fun request(myEmail: String, receiverId: UUID, message: String): FriendRequest {
        return friendRequestSavePort.createFriendshipRequest(
            myEmail = myEmail,
            receiverId = receiverId,
            message = message
        )
    }
}