package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.jpa.FriendshipJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendshipRepository
import com.kdongsu5509.friends.repository.mapper.FriendshipMapper
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class FriendshipRepositoryImpl(
    private val entityManager: EntityManager,
    private val friendshipMapper: FriendshipMapper,
    private val springDataFriendshipRepository: SpringDataFriendshipRepository,
) : FriendshipRepository {

    override fun findById(id: UUID): Friendship? =
        springDataFriendshipRepository.findById(id)
            .map { friendshipMapper.toDomain(it) }
            .orElse(null)

    override fun findByOwnerEmail(email: String, pageable: Pageable): Slice<Friendship> =
        springDataFriendshipRepository.findByOwnerUserEmail(email, pageable)
            .map { friendshipMapper.toDomain(it) }

    override fun findAll(pageable: Pageable): Slice<Friendship> =
        springDataFriendshipRepository.findAll(pageable)
            .map { friendshipMapper.toDomain(it) }

    override fun findByOwnerEmailAndFriendId(ownerEmail: String, friendId: UUID): Friendship? =
        springDataFriendshipRepository.findByOwnerUserEmailAndFriendUserId(ownerEmail, friendId)
            .map { friendshipMapper.toDomain(it) }
            .orElse(null)


    override fun delete(ownerId: UUID, friendId: UUID) {
        springDataFriendshipRepository.deleteBoth(ownerId, friendId)
    }

    override fun updateAlias(newFriendship: Friendship): Friendship {
        val friendshipJpaEntity = entityManager.getReference(
            FriendshipJpaEntity::class.java,
            newFriendship.id!!
        )

        friendshipJpaEntity.friendAlias = newFriendship.friendAlias

        return friendshipMapper.toDomain(friendshipJpaEntity)
    }

    override fun save(friendship: Friendship): Friendship {
        val owner = entityManager.getReference(UserJpaEntity::class.java, friendship.owner.id!!)
        val friend = entityManager.getReference(UserJpaEntity::class.java, friendship.friend.id!!)
        val entity = FriendshipJpaEntity.create(owner, friend, friendship.friendAlias)

        return friendshipMapper.toDomain(springDataFriendshipRepository.save(entity))
    }

    override fun existsByOwnerUserIdAndFriendUserId(ownerId: UUID, friendId: UUID): Boolean =
        springDataFriendshipRepository.existsByOwnerUserIdAndFriendUserId(ownerId, friendId)
}
