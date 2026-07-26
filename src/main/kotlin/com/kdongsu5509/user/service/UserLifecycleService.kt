package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserActivationContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.event.UserForceLogoutEvent
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserLifecycleService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : UserActivationContract {

    @Transactional
    override fun activateIfPending(userId: UUID): UserResult {
        val user = findById(userId)
        if (user.status != UserStatus.PENDING) return UserResult.fromDomain(user)

        val activeUser = user.activate()
        userRepository.update(activeUser)
        return UserResult.fromDomain(activeUser)
    }

    @Transactional
    fun block(userEmail: String): UserResult {
        val blockedUser = findByEmail(userEmail).block()
        userRepository.update(blockedUser)
        publishForceLogout(userEmail)
        return UserResult.fromDomain(blockedUser)
    }

    @Transactional
    fun unblock(userEmail: String): UserResult {
        val unblockedUser = findByEmail(userEmail).unblock()
        userRepository.update(unblockedUser)
        return UserResult.fromDomain(unblockedUser)
    }

    @Transactional
    fun withdraw(userEmail: String): UserResult {
        val withdrawnUser = findByEmail(userEmail).withdraw()
        userRepository.update(withdrawnUser)
        publishForceLogout(userEmail)
        return UserResult.fromDomain(withdrawnUser)
    }

    @Transactional
    fun requestForceLogout(userEmail: String) {
        publishForceLogout(userEmail)
    }

    private fun findById(userId: UUID) =
        userRepository.findById(userId) ?: UserException.USER_NOT_FOUND.throwIt()

    private fun findByEmail(userEmail: String) =
        userRepository.findByEmail(userEmail) ?: UserException.USER_NOT_FOUND.throwIt()

    private fun publishForceLogout(userEmail: String) {
        eventPublisher.publishEvent(UserForceLogoutEvent(userEmail))
    }
}
