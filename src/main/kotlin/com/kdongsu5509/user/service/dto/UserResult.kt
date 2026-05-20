package com.kdongsu5509.user.service.dto

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import java.util.*

data class UserResult(
    val id: UUID,
    val email: String,
    val nickname: String,
    val oauthProvider: OAuth2Provider,
    var role: UserRole,
    var status: UserStatus
) {
    companion object {
        fun fromDomain(user: User): UserResult = UserResult(
            id = extractIdOrThrow(user),
            email = user.email,
            nickname = user.nickname,
            oauthProvider = user.oauthProvider,
            role = user.role,
            status = user.status
        )

        private fun extractIdOrThrow(user: User): UUID {
            return user.id ?: let { UserException.USER_ID_NULL.throwIt() }
        }
    }
}
