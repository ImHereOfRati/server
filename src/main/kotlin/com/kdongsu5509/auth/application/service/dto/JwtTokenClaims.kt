package com.kdongsu5509.auth.application.service.dto

import com.kdongsu5509.user.api.UserResult
import java.time.LocalDateTime
import java.util.*

data class JwtTokenClaims(
    val uid: UUID,
    val email: String,
    val nickname: String,
    val role: String,
    val status: String,
    val expiration: LocalDateTime? = null,
    val refreshTokenVersion: Long = 0,
    val tokenId: String? = null
) {
    companion object {
        fun fromUser(user: UserResult): JwtTokenClaims {
            return JwtTokenClaims(
                uid = user.id,
                email = user.email,
                nickname = user.nickname,
                role = user.role.name,
                status = user.status.name,
                refreshTokenVersion = user.refreshTokenVersion,
            )
        }
    }
}
