package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringQueryDSLUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserMapper
import com.kdongsu5509.imhereuserservice.application.port.out.LoadUserPort
import com.kdongsu5509.imhereuserservice.domain.User
import com.kdongsu5509.imhereuserservice.support.exception.domain.auth.UserNotFoundException
import org.springframework.stereotype.Component

@Component
class UserLoadPersistenceAdapter(
    private val userMapper: UserMapper,
    private val springDataUserRepository: SpringDataUserRepository,
    private val springQueryDSLUserRepository: SpringQueryDSLUserRepository

) : LoadUserPort {
    override fun findByEmail(email: String): User {
        val findJpaEntity: UserJpaEntity? = springDataUserRepository.findByEmail(email)
        findJpaEntity ?: throw UserNotFoundException()
        return userMapper.mapToDomainEntity(findJpaEntity)
    }

    override fun findByEmailAndNickname(keyword: String): List<User> {
        val findJpaEntities = springQueryDSLUserRepository.findUserByKeyword(keyword)
        if (findJpaEntities.isEmpty()) return listOf()
        return findJpaEntities.stream()
            .map { entity -> userMapper.mapToDomainEntity(entity) }
            .toList()
    }
}