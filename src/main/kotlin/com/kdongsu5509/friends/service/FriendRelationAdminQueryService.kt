package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.repository.FriendRelationQueryRepository
import com.kdongsu5509.friends.service.dto.FriendRequestView
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
import com.kdongsu5509.friends.service.dto.FriendshipView
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
class FriendRelationAdminQueryService(
    private val friendRelationQueryRepository: FriendRelationQueryRepository,
    private val friendMemberLoader: FriendMemberLoader
) {

    fun findAllRequests(pageable: Pageable): Slice<FriendRequestView> =
        friendMemberLoader.toViews(
            friendRelationQueryRepository.findAllRequests(pageable),
            FriendRequestView::of
        )

    fun findAllFriendships(pageable: Pageable): Slice<FriendshipView> =
        friendMemberLoader.toViews(
            friendRelationQueryRepository.findAllFriendships(pageable),
            FriendshipView::ofAny
        )

    fun findAllRestrictions(pageable: Pageable): Slice<FriendRestrictionView> =
        friendMemberLoader.toViews(
            friendRelationQueryRepository.findAllRestrictions(pageable),
            FriendRestrictionView::of
        )
}
