package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.friends.repository.mapper.FriendRestrictionMapper
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

@Repository
class FriendRestrictionRepository(
    private val entityManager: EntityManager,
    private val friendRestrictionMapper: FriendRestrictionMapper,
    private val springDataFriendRestrictionRepository: SpringDataFriendRestrictionRepository,
) {
    fun save(friendRestriction: FriendRestriction): FriendRestriction {
        val restrictor = entityManager.getReference(UserJpaEntity::class.java, friendRestriction.restrictor.id!!)
        val restricted = entityManager.getReference(UserJpaEntity::class.java, friendRestriction.restricted.id!!)
        val entity = FriendRestrictionJpaEntity.create(
            restrictor,
            restricted,
            friendRestriction.type,
            friendRestriction.expiredAt
        )

        return friendRestrictionMapper.toDomain(springDataFriendRestrictionRepository.save(entity))
    }

    fun findById(id: UUID): FriendRestriction? =
        springDataFriendRestrictionRepository.findById(id)
            .map { friendRestrictionMapper.toDomain(it) }
            .orElse(null)

    fun findAllByEmail(email: String, pageable: Pageable): Slice<FriendRestriction> =
        springDataFriendRestrictionRepository.findByRestrictorEmail(email, pageable)
            .map { friendRestrictionMapper.toDomain(it) }

    fun findAll(pageable: Pageable): Slice<FriendRestriction> =
        springDataFriendRestrictionRepository.findAll(pageable)
            .map { friendRestrictionMapper.toDomain(it) }

    fun deleteById(id: UUID) {
        springDataFriendRestrictionRepository.deleteById(id)
    }

    fun deleteBlockByRestrictorEmailAndRestrictedId(restrictorEmail: String, restrictedId: UUID) {
        springDataFriendRestrictionRepository.deleteByRestrictorEmailAndRestrictedIdAndType(
            restrictorEmail,
            restrictedId,
            FriendRestrictionType.BLOCK
        )
    }

    fun deleteExpiredRestrictions() {
        springDataFriendRestrictionRepository.deleteExpired(LocalDateTime.now())
    }

    fun existsRestriction(requesterEmail: String, targetEmail: String): Boolean =
        springDataFriendRestrictionRepository.existsByRestrictorEmailAndRestrictedEmail(
            requesterEmail,
            targetEmail
        )
}
