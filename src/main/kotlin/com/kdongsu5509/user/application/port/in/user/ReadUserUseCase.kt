package com.kdongsu5509.user.application.port.`in`.user

import com.kdongsu5509.user.application.dto.UserResponse

interface ReadUserUseCase {
    fun findByEmailAndNickname(userEmail: String, keyword: String): List<UserResponse>
    fun findByEmail(email: String): UserResponse
}
