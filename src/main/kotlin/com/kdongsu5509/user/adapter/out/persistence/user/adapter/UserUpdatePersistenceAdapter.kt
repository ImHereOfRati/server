package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.user.application.port.out.user.UserUpdatePort
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.domain.user.UserStatus
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

    override fun block(userEmail: String) {
        val entity = findUserJpaEntity(userEmail)
        entity.block()
        springDataUserRepository.save(entity)
    }

    override fun unblock(userEmail: String) {
        val entity = findUserJpaEntity(userEmail)
        entity.unblock()
        springDataUserRepository.save(entity)
    }

    private fun findUserJpaEntity(userEmail: String): UserJpaEntity {
        return springDataUserRepository.findByEmail(userEmail)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
    }
}
