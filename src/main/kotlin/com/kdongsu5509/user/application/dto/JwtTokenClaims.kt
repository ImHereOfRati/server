package com.kdongsu5509.user.application.dto

import com.kdongsu5509.user.domain.user.User
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
        fun from(user: User): JwtTokenClaims {
            return JwtTokenClaims(
                uid = user.id!!,
                email = user.email,
                nickname = user.nickname,
                role = user.role.name,
                status = user.status.name
            )
        }
    }
}
