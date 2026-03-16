package com.kdongsu5509.user.application.dto

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.domain.user.User
import java.util.*

data class UserInformation(
    val id: UUID,
    val email: String,
    val nickname: String
) {
    companion object {
        fun convertToUserInformation(user: User): UserInformation = UserInformation(
            id = extractIdOrThrowBusinessException(user),
            email = user.email,
            nickname = user.nickname
        )

        private fun extractIdOrThrowBusinessException(user: User): UUID {
            return user.id ?: let {
                throw BusinessException(UserErrorCode.USER_ID_NULL)
            }
        }
    }
}