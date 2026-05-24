package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
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
        val result = userRepository.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()
        return UserResult.fromDomain(result)
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
        val updatedUser = userRepository.updateNickname(userEmail, newNickname)
        return UserResult.fromDomain(updatedUser)
    }

    @Transactional
    override fun block(userEmail: String) {
        userRepository.block(userEmail)
    }

    @Transactional
    override fun unblock(userEmail: String) {
        userRepository.unblock(userEmail)
    }
}
