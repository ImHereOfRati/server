package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userRepository: UserRepository,
) {

    @Transactional
    fun updateNickname(userEmail: String, newNickname: String): UserResult {
        val user = userRepository.findByEmail(userEmail) ?: UserException.USER_NOT_FOUND.throwIt()
        val updatedUser = user.updateNickname(newNickname)
        userRepository.update(updatedUser)
        return UserResult.fromDomain(updatedUser)
    }
}
