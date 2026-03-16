package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRestrictionMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionLoadPort
import com.kdongsu5509.user.domain.friend.FriendRestriction
import org.springframework.stereotype.Component

@Component
class FriendRestrictionLoadPersistenceAdapter(
    private val friendRestrictionMapper: FriendRestrictionMapper,
    private val userRepository: SpringQueryDSLUserRepository,
    private val springDataFriendRestrictionRepository: SpringDataFriendRestrictionRepository,
) : FriendRestrictionLoadPort {

    override fun loadAll(email: String): List<FriendRestriction> {
        fetchRequiredUser(email)
        val queryResult = springDataFriendRestrictionRepository.findByActorId(
            fetchRequiredUser(email).id!!
        )

        return queryResult.map { result ->
            friendRestrictionMapper.mapToDomainEntity(result)
        }
    }

    override fun loadById(friendRestrictionId: Long): FriendRestriction {
        val queryResult = springDataFriendRestrictionRepository.findById(friendRestrictionId).orElseThrow {
            throw BusinessException(FriendErrorCode.FRIEND_RESTRICTION_NOT_FOUND)
        }

        return friendRestrictionMapper.mapToDomainEntity(queryResult)
    }

    private fun fetchRequiredUser(email: String): UserJpaEntity {
        return userRepository.findActiveUserByEmail(email).orElseThrow {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
    }
}