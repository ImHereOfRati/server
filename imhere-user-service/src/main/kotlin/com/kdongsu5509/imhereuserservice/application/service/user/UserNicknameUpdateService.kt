package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.UserInformation
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.UpdateUserUseCase
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserUpdatePort
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class UserNicknameUpdateService(
    private val userUpdatePort: UserUpdatePort,
) : UpdateUserUseCase {

    override fun changeNickName(
        userEmail: String,
        newNickname: String
    ): UserInformation {
        val updatedUser = userUpdatePort.updateNickname(userEmail, newNickname)
        return UserInformation(updatedUser.email, updatedUser.nickname)
    }
}