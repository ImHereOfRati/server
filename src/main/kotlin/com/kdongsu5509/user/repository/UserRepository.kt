package com.kdongsu5509.user.repository

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepository(
    private val userMapper: UserMapper,
    private val entityManager: EntityManager,
    private val springDataUserRepository: SpringDataUserRepository,
    private val springQueryDSLUserRepository: SpringQueryDSLUserRepository
) {
    fun findById(id: UUID): User? {
        val queryResult = springDataUserRepository.findById(id)
        return if (queryResult.isPresent) {
            userMapper.toDomain(queryResult.get())
        } else null
    }

    fun findByEmail(email: String): User? {
        val queryResult = springDataUserRepository.findByEmail(email)
        return queryResult?.let { userMapper.toDomain(it) }
    }

    fun findByOidcIdentity(provider: OAuth2Provider, oidcSubject: String): User? {
        val queryResult = springDataUserRepository.findByProviderAndOidcSubject(provider, oidcSubject)
        return queryResult?.let { userMapper.toDomain(it) }
    }

    fun findAllByIds(ids: Collection<UUID>): List<User> {
        if (ids.isEmpty()) return emptyList()
        return springDataUserRepository.findAllById(ids).mapNotNull { userMapper.toDomain(it) }
    }

    fun findAll(pageable: Pageable): Slice<User> {
        return springDataUserRepository.findAllByStatusNot(UserStatus.WITHDRAWN, pageable)
            .map { userMapper.toDomain(it)!! }
    }

    fun searchActiveByKeyword(
        keyword: String,
        excludedUserIds: Set<UUID>,
        pageable: Pageable
    ): Slice<User> {
        val findJpaEntities = springQueryDSLUserRepository.findAllActiveByKeyword(keyword, excludedUserIds, pageable)
        return findJpaEntities.map { userMapper.toDomain(it)!! }
    }

    fun save(user: User): User {
        val jpaEntity = userMapper.toEntity(user)
        val savedEntity = springDataUserRepository.save(jpaEntity)
        return userMapper.toDomain(savedEntity)!!
    }

    fun update(user: User) {
        val userJpaEntity = entityManager.find(UserJpaEntity::class.java, user.id)
            ?: UserException.USER_NOT_FOUND.throwIt()
        userJpaEntity.update(user)
    }

}
