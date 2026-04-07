package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.`in`.friend.CreateFriendRequestUseCase
import com.kdongsu5509.user.application.port.out.friend.FriendRequestSavePort
import com.kdongsu5509.user.application.port.out.noti.FriendAlertPort
import com.kdongsu5509.user.domain.friend.FriendRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional
class FriendRequestCreateService(
    private val friendRequestSavePort: FriendRequestSavePort,
    private val friendAlertPort: FriendAlertPort
) : CreateFriendRequestUseCase {

    override fun request(
        myEmail: String,
        myNickname: String,
        receiverId: UUID,
        receiverEmail: String,
        message: String
    ): FriendRequest {
        val result = friendRequestSavePort.save(
            requesterEmail = myEmail,
            receiverId = receiverId,
            message = message
        )

        friendAlertPort.sendAlert(
            AlertInformation(
                senderNickname = myNickname,
                body = "$myNickname 님이 친구 요청을 보냈습니다.",
                receiverEmail = receiverEmail
            )
        )

        return result
    }
}
