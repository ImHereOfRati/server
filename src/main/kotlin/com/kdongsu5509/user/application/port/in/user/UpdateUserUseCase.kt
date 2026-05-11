package com.kdongsu5509.user.application.port.`in`.user

import com.kdongsu5509.user.application.dto.UserResponse

interface UpdateUserUseCase {
    fun updateNickname(userEmail: String, newNickname: String): UserResponse
}
