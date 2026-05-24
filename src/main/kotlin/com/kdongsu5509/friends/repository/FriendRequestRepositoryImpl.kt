package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.jpa.FriendRequestJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.friends.repository.mapper.FriendRequestMapper
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class FriendRequestRepositoryImpl(
    private val entityManager: EntityManager,
    private val friendRequestMapper: FriendRequestMapper,
    private val springDataFriendRequestRepository: SpringDataFriendRequestRepository,
) : FriendRequestRepository {

    override fun save(friendRequest: FriendRequest): FriendRequest {
        val requesterRef = entityManager.getReference(UserJpaEntity::class.java, friendRequest.requesterId())
        val receiverRef = entityManager.getReference(UserJpaEntity::class.java, friendRequest.receiverId())

        val entity = FriendRequestJpaEntity(requesterRef, receiverRef, friendRequest.message)
        val result = springDataFriendRequestRepository.save(entity)

        return friendRequestMapper.toDomain(result)
    }

    override fun findAll(pageable: Pageable): Slice<FriendRequest> =
        springDataFriendRequestRepository.findAll(pageable)
            .map { friendRequestMapper.toDomain(it) }

    override fun findAllByReceiverEmail(email: String, pageable: Pageable): Slice<FriendRequest> =
        springDataFriendRequestRepository.findAllByReceiverEmail(email, pageable)
            .map { friendRequestMapper.toDomain(it) }

    override fun findAllByRequesterEmail(email: String, pageable: Pageable): Slice<FriendRequest> =
        springDataFriendRequestRepository.findAllByRequesterEmail(email, pageable)
            .map { friendRequestMapper.toDomain(it) }

    override fun findById(id: UUID): FriendRequest? =
        springDataFriendRequestRepository.findById(id)
            .map { friendRequestMapper.toDomain(it) }
            .orElse(null)

    override fun deleteById(id: UUID) = springDataFriendRequestRepository.deleteById(id)

    override fun deleteBetween(user1Id: UUID, user2Id: UUID) {
        val u1 = entityManager.getReference(UserJpaEntity::class.java, user1Id)
        val u2 = entityManager.getReference(UserJpaEntity::class.java, user2Id)
        springDataFriendRequestRepository.deleteByRequesterAndReceiver(u1, u2)
        springDataFriendRequestRepository.deleteByRequesterAndReceiver(u2, u1)
    }

    override fun existsByRequesterIdAndReceiverId(requesterId: UUID, receiverId: UUID): Boolean =
        springDataFriendRequestRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)
}
