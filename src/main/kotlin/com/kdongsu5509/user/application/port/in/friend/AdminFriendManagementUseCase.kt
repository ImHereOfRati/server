package com.kdongsu5509.user.application.port.`in`.friend

interface AdminFriendManagementUseCase {
    fun forceClearFriendRelationship(userAEmail: String, userBEmail: String)
    fun forceClearFriendRequest(requesterEmail: String, receiverEmail: String)
}
