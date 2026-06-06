package com.kdongsu5509.user.repository

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserRepositoryImpl(
    private val userMapper: UserMapper,
    private val entityManager: EntityManager,
    private val springDataUserRepository: SpringDataUserRepository,
    private val springQueryDSLUserRepository: SpringQueryDSLUserRepository
) : UserRepository {
    override fun findById(id: UUID): User? {
        val queryResult = springDataUserRepository.findById(id)
        return if (queryResult.isPresent) {
            userMapper.toDomain(queryResult.get())
        } else null
    }

    override fun findByEmail(email: String): User? {
        val queryResult = springDataUserRepository.findByEmail(email)
        return queryResult?.let { userMapper.toDomain(it) }
    }

    override fun findAll(pageable: Pageable): Slice<User> {
        return springQueryDSLUserRepository.findAll(pageable)
            .map { userMapper.toDomain(it)!! }
    }

    override fun findActiveUserByEmail(email: String): User? {
        val queryResult =
            springQueryDSLUserRepository.findActiveUserByEmail(email) ?: return null

        return userMapper.toDomain(queryResult)
    }

    override fun findSliceByEmailAndNickname(
        userEmail: String,
        keyword: String,
        pageable: Pageable
    ): Slice<User> {
        val findJpaEntities = springQueryDSLUserRepository.findAllActiveByEmailAndKeyword(userEmail, keyword, pageable)
        return findJpaEntities.map { userMapper.toDomain(it)!! }
    }

    override fun save(user: User): User {
        val jpaEntity = userMapper.toEntity(user)
        val savedEntity = springDataUserRepository.save(jpaEntity)
        return userMapper.toDomain(savedEntity)!!
    }

    override fun update(user: User) {
        val userJpaEntity = entityManager.find(UserJpaEntity::class.java, user.id)
            ?: UserException.USER_NOT_FOUND.throwIt()
        userJpaEntity.update(user)
    }

    override fun existsByEmail(email: String): Boolean =
        springDataUserRepository.existsByEmail(email)
}
