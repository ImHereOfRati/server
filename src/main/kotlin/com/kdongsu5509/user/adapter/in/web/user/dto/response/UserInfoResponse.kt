package com.kdongsu5509.user.adapter.`in`.web.user.dto.response

import com.kdongsu5509.user.application.dto.UserResponse
import java.util.*

data class UserInfoResponse(
    val userId: UUID,
    val userEmail: String,
    val userNickname: String
) {
    companion object {
        fun fromUserResponse(userResponse: UserResponse): UserInfoResponse {
            return UserInfoResponse(userResponse.id, userResponse.email, userResponse.nickname)
        }
    }
}
