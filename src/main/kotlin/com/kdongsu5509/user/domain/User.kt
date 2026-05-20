package com.kdongsu5509.user.domain

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import java.util.*

data class User(
    val id: UUID?,
    val email: String,
    var nickname: String,
    var role: UserRole,
    val oauthProvider: OAuth2Provider,
    var status: UserStatus
) {
    companion object {
        fun createWithPendingStatus(email: String, nickname: String, oauthProvider: OAuth2Provider): User {
            return User(null, email, nickname, UserRole.NORMAL, oauthProvider, UserStatus.PENDING)
        }
    }

    fun roleName(): String {
        return this.role.name
    }

    fun statusName(): String {
        return this.status.name
    }

    fun activate() {
        this.status = UserStatus.ACTIVE
    }
}
