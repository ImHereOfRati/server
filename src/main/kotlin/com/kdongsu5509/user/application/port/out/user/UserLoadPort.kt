package com.kdongsu5509.user.application.port.out.user

import com.kdongsu5509.user.domain.user.User

interface UserLoadPort {
    fun findByEmail(email: String): User?
    fun findActiveUserByEmail(email: String): User?
    fun findPotentialFriendsByEmailAndNickname(userEmail: String, keyword: String): List<User>
}
