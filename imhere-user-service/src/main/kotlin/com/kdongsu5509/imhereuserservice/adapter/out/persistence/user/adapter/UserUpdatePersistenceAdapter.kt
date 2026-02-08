package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserUpdatePort
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class UserUpdatePersistenceAdapter(
    private val userMapper: UserMapper,
    private val springDataUserRepository: SpringDataUserRepository,
) : UserUpdatePort {

    override fun activate(userEmail: String) {
        val queryResult = findUserJpaEntity(userEmail)

        if (queryResult.status == UserStatus.PENDING) {
            queryResult.activate()
            springDataUserRepository.save(queryResult)
        }
    }

    override fun updateNickname(userEmail: String, newNickname: String): User {
        val queryResult = findUserJpaEntity(userEmail)
        queryResult.changeNickname(newNickname)
        return userMapper.mapToDomainEntity(
            springDataUserRepository.save(queryResult)
        )
    }

    private fun findUserJpaEntity(userEmail: String): UserJpaEntity {
        return springDataUserRepository.findByEmail(userEmail)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }
}