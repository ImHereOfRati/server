package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserMapper
import com.kdongsu5509.imhereuserservice.application.port.out.SaveUserPort
import com.kdongsu5509.imhereuserservice.domain.User
import org.springframework.stereotype.Component

@Component
class UserSavePersistenceAdapter(
    private val userMapper: UserMapper,
    private val springDataUserRepository: SpringDataUserRepository,

    ) : SaveUserPort {
    override fun save(user: User) {
        val jpaEntity = userMapper.mapToJpaEntity(user)
        springDataUserRepository.save(jpaEntity)
    }
}