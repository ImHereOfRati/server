package com.kdongsu5509.user.application.port.out.user

import com.kdongsu5509.user.domain.user.User

interface UserUpdatePort {
    fun activate(userEmail: String)
    fun updateNickname(userEmail: String, newNickname: String): User
    fun block(userEmail: String)
    fun unblock(userEmail: String)
}
