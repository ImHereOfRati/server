package com.kdongsu5509.imhereuserservice.application.port.out.user

import com.kdongsu5509.imhereuserservice.domain.user.User

interface UserUpdatePort {
    fun activate(userEmail: String)
    fun updateNickname(userEmail: String, newNickname: String): User
}