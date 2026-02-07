package com.kdongsu5509.imhereuserservice.application.service.friend

import com.kdongsu5509.imhereuserservice.application.dto.UserInformation
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.ReadUserUseCase
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserLoadPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class ReadUserService(private val userLoadPort: UserLoadPort) : ReadUserUseCase {
    override fun searchUser(keyword: String): List<UserInformation> {
        val matchedUsers = userLoadPort.findByEmailAndNickname(keyword)
        return matchedUsers.map { user -> UserInformation(user.email, user.nickname) }
    }

    override fun searchMe(email: String): UserInformation {
        val me = userLoadPort.findActiveUserByEmailOrNull(email)
        return UserInformation(me!!.email, me.nickname)
    }
}