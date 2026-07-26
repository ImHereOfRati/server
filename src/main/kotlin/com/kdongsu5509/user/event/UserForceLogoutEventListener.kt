package com.kdongsu5509.user.event

import com.kdongsu5509.user.repository.UserRepository
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class UserForceLogoutEventListener(
    private val userRepository: UserRepository,
) {

    @ApplicationModuleListener
    fun handle(event: UserForceLogoutEvent) {
        val user = userRepository.findByEmail(event.userEmail) ?: return
        userRepository.update(user.rotateRefreshTokenVersion())
    }
}
