package com.kdongsu5509.user.domain

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.exception.UserException
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
            status = UserStatus.ACTIVE
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
            status = UserStatus.BLOCKED
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
            status = UserStatus.ACTIVE
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
            status = UserStatus.WITHDRAWN
        )
    }

    fun updateNickname(newNickname: String) = User(
        id = id,
        email = email,
        nickname = newNickname,
        role = role,
        oauthProvider = oauthProvider,
        status = status
    )

    fun validateDuplicateEmailAllowed(isExistingEmail: Boolean) {
        if (isExistingEmail) {
            UserException.DUPLICATE_EMAIL.throwIt()
        }
    }
}
