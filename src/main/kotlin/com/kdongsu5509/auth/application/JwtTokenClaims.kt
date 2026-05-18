package com.kdongsu5509.auth.application

import com.kdongsu5509.user.domain.User
import java.time.LocalDateTime
import java.util.*

data class JwtTokenClaims(
    val uid: UUID,
    val email: String,
    val nickname: String,
    val role: String,
    val status: String,
    val expiration: LocalDateTime? = null
) {
    companion object {
        fun fromUser(user: User): JwtTokenClaims {
            return JwtTokenClaims(
                uid = user.id!!,
                email = user.email,
                nickname = user.nickname,
                role = user.roleName(),
                status = user.statusName()
            )
        }
    }
}
