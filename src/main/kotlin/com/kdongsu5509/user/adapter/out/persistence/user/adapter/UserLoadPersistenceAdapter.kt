package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.user.application.port.out.user.UserLoadPort
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.exception.UserError
import org.springframework.stereotype.Component

@Component
class UserLoadPersistenceAdapter(
    private val userMapper: UserMapper,
    private val springQueryDSLUserRepository: SpringQueryDSLUserRepository
) : UserLoadPort {
    override fun findByEmail(email: String): User {
        val queryResult = springQueryDSLUserRepository.findUserByEmail(email) ?: UserError.USER_NOT_FOUND.throwIt()
        return userMapper.toDomain(queryResult)
    }

    override fun findActiveUserByEmail(email: String): User? {
        val queryResult =
            springQueryDSLUserRepository.findActiveUserByEmail(email) ?: UserError.USER_NOT_FOUND.throwIt()

        return userMapper.toDomain(queryResult)
    }

    override fun findPotentialFriendsByEmailAndNickname(userEmail: String, keyword: String): List<User> {
        val findJpaEntities = springQueryDSLUserRepository.searchNewFriendCandidates(userEmail, keyword)
        if (findJpaEntities.isEmpty()) return listOf()
        return findJpaEntities.stream()
            .map { entity -> userMapper.toDomain(entity) }
            .toList()
    }
}
