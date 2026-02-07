package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserLoadPort
import com.kdongsu5509.imhereuserservice.domain.user.User
import org.springframework.stereotype.Component

@Component
class UserLoadPersistenceAdapter(
    private val userMapper: UserMapper,
    private val springQueryDSLUserRepository: SpringQueryDSLUserRepository

) : UserLoadPort {
    override fun findUserByEmailOrNull(email: String): User? {
        val queryResult = springQueryDSLUserRepository.findUserByEmail(email)
        if (queryResult.isEmpty) {
            return null
        }
        return userMapper.mapToDomainEntity(queryResult.get())
    }

    override fun findActiveUserByEmailOrNull(email: String): User? {
        val queryResult = springQueryDSLUserRepository.findActiveUserByEmail(email)
        if (queryResult.isEmpty) {
            return null
        }
        return userMapper.mapToDomainEntity(queryResult.get())
    }

    override fun findByEmailAndNickname(keyword: String): List<User> {
        val findJpaEntities = springQueryDSLUserRepository.findUserByKeyword(keyword)
        if (findJpaEntities.isEmpty()) return listOf()
        return findJpaEntities.stream()
            .map { entity -> userMapper.mapToDomainEntity(entity) }
            .toList()
    }
}