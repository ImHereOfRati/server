package com.kdongsu5509.user.domain.friend


import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
import java.time.LocalDateTime
import java.util.*

data class FriendRequest(
    val friendRequestId: Long? = null,
    val requester: FriendRequestUserInfo,
    val receiver: FriendRequestUserInfo,
    val message: String?,
    val createdAt: LocalDateTime? = null
) {
    init {
        if (requester.id == receiver.id) {
            throw BusinessException(FriendErrorCode.SELF_FRIENDSHIP)
        }

        message?.let {
            if (it.length > 255) {
                throw BusinessException(FriendErrorCode.FRIENDSHIP_REQUEST_MESSAGE_OVER)
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