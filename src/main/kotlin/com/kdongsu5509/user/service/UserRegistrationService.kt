package com.kdongsu5509.user.service

import com.kdongsu5509.user.api.RegisterUserCommand
import com.kdongsu5509.user.api.UserRegistrationContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.EmailRegistrationPolicy
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
) : UserRegistrationContract {

    @Transactional
    override fun register(command: RegisterUserCommand): UserResult {
        val existingUser = userRepository.findByEmail(command.email)
        EmailRegistrationPolicy.assertNotDuplicated(existingUser != null)

        val newUser = User(
            email = command.email,
            nickname = command.nickname,
            oauthProvider = command.oauthProvider,
            oidcSubject = command.oidcSubject,
        )
        return UserResult.fromDomain(userRepository.save(newUser))
    }
}
