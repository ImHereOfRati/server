package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.`in`.ForceLogoutUseCase
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForceLogoutService(
    private val userRepository: UserRepository
) : ForceLogoutUseCase {

    @Transactional
    override fun logout(userEmail: String) {
        val user = userRepository.findByEmail(userEmail) ?: return
        userRepository.update(user.rotateRefreshTokenVersion())
    }
}
