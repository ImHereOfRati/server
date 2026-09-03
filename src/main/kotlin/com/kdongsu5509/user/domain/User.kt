package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.exception.UserException
import java.util.*

data class User(
    val id: UUID?,
    val email: String,
    var nickname: String,
    var role: UserRole,
    val oauthProvider: OAuth2Provider,
    val status: UserStatus,
    val oidcSubject: String? = null,
    val refreshTokenVersion: Long = 0
) {
    constructor(
        email: String,
        nickname: String,
        oauthProvider: OAuth2Provider,
        oidcSubject: String? = null
    ) : this(
        null, email, nickname, UserRole.NORMAL, oauthProvider, UserStatus.PENDING, oidcSubject
    )

    fun roleName(): String {
        return this.role.name
    }

    fun statusName(): String {
        return this.status.name
    }

    fun activate(): User {
        if (this.status != UserStatus.PENDING) {
            UserException.ONLY_PENDING_CAN_BE_ACTIVE_USER.throwIt()
        }
        return copy(status = UserStatus.ACTIVE)
    }

    fun block(): User {
        validateNotWithdraw()
        if (this.status != UserStatus.ACTIVE) {
            UserException.ONLY_ACTIVE_USER_CAN_BE_BLOCKED.throwIt()
        }
        return copy(status = UserStatus.BLOCKED)
    }

    fun unblock(): User {
        validateNotWithdraw()
        if (this.status != UserStatus.BLOCKED) {
            UserException.ONLY_BLOCKED_USER_CAN_BE_UNBLOCKED.throwIt()
        }
        return copy(status = UserStatus.ACTIVE)
    }

    fun withdraw(): User {
        validateNotWithdraw()
        if (this.status != UserStatus.PENDING &&
            this.status != UserStatus.ACTIVE &&
            this.status != UserStatus.BLOCKED
        ) {
            UserException.ONLY_ACTIVE_OR_BLOCKED_USER_CAN_WITHDRAW.throwIt()
        }
        return copy(status = UserStatus.WITHDRAWN)
    }

    fun updateNickname(newNickname: String) = copy(nickname = newNickname)

    fun rotateRefreshTokenVersion(): User = copy(refreshTokenVersion = refreshTokenVersion + 1)

    private fun validateNotWithdraw() {
        if (this.status == UserStatus.WITHDRAWN) UserException.WITHDRAW_USER.throwIt()
    }
}
