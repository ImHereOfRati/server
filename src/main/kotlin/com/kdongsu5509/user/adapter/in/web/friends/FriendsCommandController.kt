package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.user.application.port.`in`.friend.UpdateFriendsUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.FriendRelationshipResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.UpdateFriendAliasRequest
import org.jetbrains.annotations.NotNull
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/user/friends")
class FriendsCommandController(
    private val updateFriendsUseCase: UpdateFriendsUseCase
) {

    @PostMapping("/alias")
    fun changeFriendAlias(
        @AuthenticationPrincipal user: UserDetails,
        @Validated @RequestBody updateFriendAliasRequest: UpdateFriendAliasRequest
    ): APIResponse<FriendRelationshipResponse> {
        val result = updateFriendsUseCase.changeFriendAlias(
            user.username,
            updateFriendAliasRequest.friendRelationshipId,
            updateFriendAliasRequest.newFriendAlias
        )

        return APIResponse.success(
            FriendRelationshipResponse.fromDomain(result)
        )
    }

    @PostMapping("/block/{friendRelationshipId}")
    fun blockFriend(
        @AuthenticationPrincipal user: UserDetails,
        @PathVariable @Validated @NotNull friendRelationshipId: UUID
    ) {
        updateFriendsUseCase.block(
            user.username,
            friendRelationshipId
        )

    }

    @DeleteMapping("/{friendRelationshipId}")
    fun deleteMyFriends(
        @AuthenticationPrincipal user: UserDetails,
        @PathVariable @Validated @NotNull friendRelationshipId: UUID
    ) {
        updateFriendsUseCase.deleteRelationship(
            user.username,
            friendRelationshipId
        )
    }

}
