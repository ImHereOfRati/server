package com.kdongsu5509.user.application.dto

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.exception.UserError
import java.util.*

data class UserResponse(
    val id: UUID,
    val email: String,
    val nickname: String
) {
    companion object {
        fun convertToUserResponse(user: User): UserResponse = UserResponse(
            id = extractIdOrThrowBusinessException(user),
            email = user.email,
            nickname = user.nickname
        )

        private fun extractIdOrThrowBusinessException(user: User): UUID {
            return user.id ?: let { UserError.USER_ID_NULL.throwIt() }
        }
    }
}
