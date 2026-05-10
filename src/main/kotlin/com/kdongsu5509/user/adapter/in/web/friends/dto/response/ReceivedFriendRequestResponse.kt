package com.kdongsu5509.user.adapter.`in`.web.friends.dto.response

data class ReceivedFriendRequestResponse(
    val friendRequestId: Long,
    val requesterEmail: String,
    val requesterNickname: String
)
