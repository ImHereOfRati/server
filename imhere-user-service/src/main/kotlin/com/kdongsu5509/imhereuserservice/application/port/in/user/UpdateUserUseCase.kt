package com.kdongsu5509.imhereuserservice.application.port.`in`.user

import com.kdongsu5509.imhereuserservice.application.dto.UserInformation

interface UpdateUserUseCase {
    fun changeNickName(userEmail: String, newNickname: String): UserInformation
}