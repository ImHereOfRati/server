package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.FriendPair
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRelationRepository
import com.kdongsu5509.friends.repository.mapper.FriendRelationMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
class FriendRelationRepository(
    private val springDataFriendRelationRepository: SpringDataFriendRelationRepository,
    private val friendRelationMapper: FriendRelationMapper
) {

    fun save(relation: FriendRelation): FriendRelation {
        val existing = relation.id?.let { springDataFriendRelationRepository.findById(it).orElse(null) }
            ?: springDataFriendRelationRepository.findByLowUserIdAndHighUserId(
                relation.pair.low,
                relation.pair.high
            )

        val entity = existing
            ?.also { friendRelationMapper.applyTo(it, relation) }
            ?: friendRelationMapper.toEntity(relation)

        return friendRelationMapper.toDomain(springDataFriendRelationRepository.save(entity))
    }

    fun findById(id: UUID): FriendRelation? =
        springDataFriendRelationRepository.findById(id).orElse(null)?.let(friendRelationMapper::toDomain)

    fun findByPair(one: UUID, other: UUID): FriendRelation? {
        if (one == other) return null

        val (low, high) = FriendPair.ordered(one, other)
        return springDataFriendRelationRepository
            .findByLowUserIdAndHighUserId(low, high)
            ?.let(friendRelationMapper::toDomain)
    }

    fun deleteById(id: UUID) = springDataFriendRelationRepository.deleteById(id)

    fun deleteExpired(now: LocalDateTime = LocalDateTime.now()) =
        springDataFriendRelationRepository.deleteExpired(now)
}
