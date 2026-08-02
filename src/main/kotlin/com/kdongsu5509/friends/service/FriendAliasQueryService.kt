package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.api.FriendAliasContract
import com.kdongsu5509.friends.repository.FriendRelationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class FriendAliasQueryService(
    private val friendRelationQueryRepository: FriendRelationQueryRepository,
) : FriendAliasContract {

    @Transactional(readOnly = true)
    override fun findAlias(ownerId: UUID, targetId: UUID): String? {
        if (ownerId == targetId) return null
        return friendRelationQueryRepository
            .findFriendshipByPair(ownerId, targetId)
            ?.getAlias(ownerId)
            ?.value
            ?.takeIf { it.isNotBlank() }
    }
}
