package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserDao
import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userDao: UserDao
) : UserService {
    override fun findByEmail(email: String): UserResult {
        val result = userDao.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()
        return UserResult.fromDomain(result)
    }

    override fun findAll(pageable: Pageable): Slice<UserResult> {
        return userDao.findAll(pageable)
            .map { UserResult.fromDomain(it) }
    }

    override fun findByKeyword(
        email: String,
        keyword: String,
        pageable: Pageable
    ): Slice<UserResult> {
        return userDao.findSliceByEmailAndNickname(email, keyword, pageable)
            .map { UserResult.fromDomain(it) }
    }

    @Transactional
    override fun updateNickname(userEmail: String, newNickname: String): UserResult {
        val updatedUser = userDao.updateNickname(userEmail, newNickname)
        return UserResult.fromDomain(updatedUser)
    }

    @Transactional
    override fun block(userEmail: String) {
        userDao.block(userEmail)
    }

    @Transactional
    override fun unblock(userEmail: String) {
        userDao.unblock(userEmail)
    }
}
