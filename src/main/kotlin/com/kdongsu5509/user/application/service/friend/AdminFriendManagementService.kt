package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.user.application.port.`in`.friend.AdminFriendManagementUseCase
import com.kdongsu5509.user.application.port.out.friend.AdminFriendPort
import org.springframework.stereotype.Service

@Service
class AdminFriendManagementService(
    private val adminFriendPort: AdminFriendPort
) : AdminFriendManagementUseCase {

    override fun forceClearFriendRelationship(userAEmail: String, userBEmail: String) {
        adminFriendPort.forceClearFriendRelationship(userAEmail, userBEmail)
    }

    override fun forceClearFriendRequest(requesterEmail: String, receiverEmail: String) {
        adminFriendPort.forceClearFriendRequest(requesterEmail, receiverEmail)
    }
}
