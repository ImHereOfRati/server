package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.repository.FriendRelationQueryRepository
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendCandidateSearchService(
    private val friendRelationQueryRepository: FriendRelationQueryRepository,
    private val userLookupContract: UserLookupContract,
) {
    fun search(requesterId: UUID, keyword: String, pageable: Pageable): Slice<UserResult> {
        val excluded = friendRelationQueryRepository.findRelatedUserIds(requesterId) + requesterId
        return userLookupContract.searchActiveByKeyword(keyword, excluded, pageable)
    }
}
