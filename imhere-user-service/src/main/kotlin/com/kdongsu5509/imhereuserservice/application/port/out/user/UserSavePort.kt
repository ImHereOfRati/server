package com.kdongsu5509.imhereuserservice.application.port.out.user

import com.kdongsu5509.imhereuserservice.domain.user.User

interface UserSavePort {
    fun save(user: User): User
}