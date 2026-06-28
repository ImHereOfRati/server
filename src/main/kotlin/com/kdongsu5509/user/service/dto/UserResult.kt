package com.kdongsu5509.user.service.dto

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import java.util.*

data class UserResult(
    val id: UUID,
    val email: String,
    val nickname: String,
    val oauthProvider: OAuth2Provider,
    val role: UserRole,
    val status: UserStatus,
    val oidcSubject: String? = null,
    val refreshTokenVersion: Long = 0
) {
    companion object {
        fun fromDomain(user: User): UserResult = UserResult(
            id = user.id!!,
            email = user.email,
            nickname = user.nickname,
            oauthProvider = user.oauthProvider,
            role = user.role,
            status = user.status,
            oidcSubject = user.oidcSubject,
            refreshTokenVersion = user.refreshTokenVersion
        )
    }

    fun toDomain() = User(
        id = id,
        email = email,
        nickname = nickname,
        role = role,
        oauthProvider = oauthProvider,
        status = status,
        oidcSubject = oidcSubject,
        refreshTokenVersion = refreshTokenVersion
    )
}
