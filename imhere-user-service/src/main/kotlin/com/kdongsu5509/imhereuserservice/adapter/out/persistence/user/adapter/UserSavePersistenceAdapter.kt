package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserSavePort
import com.kdongsu5509.imhereuserservice.domain.user.User
import org.springframework.stereotype.Component

@Component
class UserSavePersistenceAdapter(
    private val userMapper: UserMapper,
    private val springDataUserRepository: SpringDataUserRepository,

    ) : UserSavePort {
    override fun save(user: User): User {
        val jpaEntity = userMapper.mapToJpaEntity(user)
        val savedEntity = springDataUserRepository.save(jpaEntity)
        return userMapper.mapToDomainEntity(savedEntity)
    }
}