package com.kdongsu5509.imhereuserservice.domain.user

data class User(
    val email: String,
    var nickname: String,
    val oauthProvider: OAuth2Provider,
    var role: UserRole,
    var status: UserStatus
) {
    companion object {
        fun createPendingUser(
            email: String,
            nickname: String,
            oauthProvider: OAuth2Provider
        ): User {
            return User(
                email = email,
                nickname = nickname,
                oauthProvider = oauthProvider,
                role = UserRole.NORMAL,
                status = UserStatus.PENDING
            )
        }
    }
}