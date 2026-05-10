package com.kdongsu5509.user.domain.user

import java.util.*

data class User(
    val id: UUID?,
    val email: String,
    var nickname: String,
    val oauthProvider: OAuth2Provider,
    var role: UserRole,
    var status: UserStatus
) {
    companion object {
        fun createWaitingForAgreementUser(email: String, nickname: String, oauthProvider: OAuth2Provider): User {
            return User(null, email, nickname, oauthProvider, UserRole.NORMAL, UserStatus.PENDING)
        }
    }
}
