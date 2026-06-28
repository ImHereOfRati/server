package com.kdongsu5509.user.domain

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.exception.UserException
import java.util.*

data class User(
    val id: UUID?,
    val email: String,
    var nickname: String,
    var role: UserRole,
    val oauthProvider: OAuth2Provider,
    var status: UserStatus,
    val oidcSubject: String? = null,
    val refreshTokenVersion: Long = 0
) {
    companion object {
        fun createWithPendingStatus(
            email: String,
            nickname: String,
            oauthProvider: OAuth2Provider,
            oidcSubject: String? = null
        ): User {
            return User(null, email, nickname, UserRole.NORMAL, oauthProvider, UserStatus.PENDING, oidcSubject)
        }
    }

    fun roleName(): String {
        return this.role.name
    }

    fun statusName(): String {
        return this.status.name
    }

    fun activate(): User {
        if (this.status != UserStatus.PENDING) {
            UserException.INVALID_USER_STATUS.throwIt()
        }
        return User(
            id = id,
            email = email,
            nickname = nickname,
            role = role,
            oauthProvider = oauthProvider,
            status = UserStatus.ACTIVE,
            oidcSubject = oidcSubject,
            refreshTokenVersion = refreshTokenVersion
        )
    }

    fun block(): User {
        if (this.status == UserStatus.BLOCKED) {
            UserException.INVALID_USER_STATUS.throwIt()
        }
        return User(
            id = id,
            email = email,
            nickname = nickname,
            role = role,
            oauthProvider = oauthProvider,
            status = UserStatus.BLOCKED,
            oidcSubject = oidcSubject,
            refreshTokenVersion = refreshTokenVersion
        )
    }

    fun unblock(): User {
        if (this.status != UserStatus.BLOCKED) {
            UserException.INVALID_USER_STATUS.throwIt()
        }
        return User(
            id = id,
            email = email,
            nickname = nickname,
            role = role,
            oauthProvider = oauthProvider,
            status = UserStatus.ACTIVE,
            oidcSubject = oidcSubject,
            refreshTokenVersion = refreshTokenVersion
        )
    }

    fun withdraw(): User {
        if (this.status == UserStatus.WITHDRAWN) {
            UserException.INVALID_USER_STATUS.throwIt()
        }
        return User(
            id = id,
            email = email,
            nickname = nickname,
            role = role,
            oauthProvider = oauthProvider,
            status = UserStatus.WITHDRAWN,
            oidcSubject = oidcSubject,
            refreshTokenVersion = refreshTokenVersion
        )
    }

    fun updateNickname(newNickname: String) = User(
        id = id,
        email = email,
        nickname = newNickname,
        role = role,
        oauthProvider = oauthProvider,
        status = status,
        oidcSubject = oidcSubject,
        refreshTokenVersion = refreshTokenVersion
    )

    fun rotateRefreshTokenVersion(): User = copy(refreshTokenVersion = refreshTokenVersion + 1)

    fun validateDuplicateEmailAllowed(isExistingEmail: Boolean) {
        if (isExistingEmail) {
            UserException.DUPLICATE_EMAIL.throwIt()
        }
    }
}
