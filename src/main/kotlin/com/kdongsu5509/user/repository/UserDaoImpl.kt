package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserDaoImpl(
    private val userMapper: UserMapper,
    private val springDataUserRepository: SpringDataUserRepository,
    private val springQueryDSLUserRepository: SpringQueryDSLUserRepository
) : UserDao {
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


    override fun activate(userId: UUID) {
        val queryResult = springDataUserRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }

        if (queryResult.status == UserStatus.PENDING) {
            queryResult.activate()
            springDataUserRepository.save(queryResult)
        }
    }

    override fun updateNickname(userEmail: String, newNickname: String): User {
        val queryResult = springDataUserRepository.findByEmail(userEmail) ?: UserException.USER_NOT_FOUND.throwIt()
        queryResult.changeNickname(newNickname)
        return userMapper.toDomain(springDataUserRepository.save(queryResult))!!
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
        return springDataUserRepository.findByEmail(userEmail) ?: UserException.USER_NOT_FOUND.throwIt()
    }
}
