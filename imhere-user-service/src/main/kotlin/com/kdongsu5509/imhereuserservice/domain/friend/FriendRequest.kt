package com.kdongsu5509.imhereuserservice.domain.friend


import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import java.time.LocalDateTime
import java.util.*

data class FriendRequest(
    val friendRequestId: UUID? = null,
    val requester: FriendRequestUserInfo,
    val receiver: FriendRequestUserInfo,
    val message: String?,
    val createdAt: LocalDateTime? = null
) {
    init {
        if (requester.id == receiver.id) {
            throw BusinessException(ErrorCode.SELF_FRIENDSHIP)
        }

        message?.let {
            if (it.length > 255) {
                throw BusinessException(ErrorCode.FRIENDSHIP_REQUEST_MESSAGE_OVER)
            }
        }
    }

    companion object {
        fun create(
            requester: FriendRequestUserInfo,
            receiver: FriendRequestUserInfo,
            message: String?
        ): FriendRequest {
            return FriendRequest(
                requester = requester,
                receiver = receiver,
                message = message
            )
        }
    }
}

data class FriendRequestUserInfo(
    val id: UUID,
    val email: String,
    val nickname: String
)