package com.kdongsu5509.user.application.port.out.friend

interface AdminFriendPort {
    fun forceClearFriendRelationship(userAEmail: String, userBEmail: String)
    fun forceClearFriendRequest(requesterEmail: String, receiverEmail: String)
}
