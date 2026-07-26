package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class UserQueryService(
    private val userRepository: UserRepository,
) : UserLookupContract {

    override fun findById(id: UUID): UserResult =
        UserResult.fromDomain(
            userRepository.findById(id) ?: UserException.USER_NOT_FOUND.throwIt()
        )

    override fun findByEmail(email: String): UserResult =
        UserResult.fromDomain(
            userRepository.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()
        )

    override fun findByEmailOrNull(email: String): UserResult? =
        userRepository.findByEmail(email)?.let(UserResult::fromDomain)

    fun findAll(pageable: Pageable): Slice<UserResult> =
        userRepository.findAll(pageable)
            .map(UserResult::fromDomain)

    fun findByKeyword(
        email: String,
        keyword: String,
        pageable: Pageable,
    ): Slice<UserResult> =
        userRepository.findSliceByEmailAndNickname(email, keyword, pageable)
            .map(UserResult::fromDomain)
}
