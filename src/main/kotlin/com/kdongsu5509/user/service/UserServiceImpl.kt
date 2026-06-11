package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {
    override fun findById(id: UUID): UserResult {
        val queryResult = userRepository.findById(id) ?: UserException.USER_NOT_FOUND.throwIt()
        return UserResult.fromDomain(queryResult)
    }

    override fun findByEmail(email: String): UserResult {
        return UserResult.fromDomain(
            findAndValidateUser(email)
        )
    }

    override fun findAll(pageable: Pageable): Slice<UserResult> {
        return userRepository.findAll(pageable)
            .map { UserResult.fromDomain(it) }
    }

    override fun findByKeyword(
        email: String,
        keyword: String,
        pageable: Pageable
    ): Slice<UserResult> {
        return userRepository.findSliceByEmailAndNickname(email, keyword, pageable)
            .map { UserResult.fromDomain(it) }
    }

    @Transactional
    override fun updateNickname(userEmail: String, newNickname: String): UserResult {
        val user = findAndValidateUser(userEmail)
        val updatedUser = user.updateNickname(newNickname)
        userRepository.update(updatedUser)
        return UserResult.fromDomain(updatedUser)
    }

    @Transactional
    override fun block(userEmail: String): UserResult {
        val user = findAndValidateUser(userEmail)
        val blockedUser = user.block()
        userRepository.update(blockedUser)
        return UserResult.fromDomain(blockedUser)
    }

    @Transactional
    override fun unblock(userEmail: String): UserResult {
        val user = findAndValidateUser(userEmail)
        val unblockedUser = user.unblock()
        userRepository.update(unblockedUser)
        return UserResult.fromDomain(unblockedUser)
    }

    @Transactional
    override fun withdraw(userEmail: String): UserResult {
        val user = findAndValidateUser(userEmail)
        val withdrawnUser = user.withdraw()
        userRepository.update(withdrawnUser)
        return UserResult.fromDomain(withdrawnUser)
    }

    private fun findAndValidateUser(userEmail: String): User =
        userRepository.findByEmail(userEmail) ?: UserException.USER_NOT_FOUND.throwIt()
}
