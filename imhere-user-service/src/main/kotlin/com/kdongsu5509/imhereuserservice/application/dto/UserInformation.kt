package com.kdongsu5509.imhereuserservice.application.dto

import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
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
                throw BusinessException(ErrorCode.USER_ID_NULL)
            }
        }
    }
}