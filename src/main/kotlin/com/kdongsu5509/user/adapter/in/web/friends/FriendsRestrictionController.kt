package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.user.application.port.`in`.friend.ReadFriendsRestrictionUseCase
import com.kdongsu5509.user.application.port.`in`.friend.UpdateFriendRestrictionUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.FriendRestrictionDeletedResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.FriendRestrictionResponse
import org.jetbrains.annotations.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user/friends/restriction")
class FriendsRestrictionController(
    private val readFriendsRestrictionUseCase: ReadFriendsRestrictionUseCase,
    private val updateFriendRestrictionUseCase: UpdateFriendRestrictionUseCase
) {
    @GetMapping
    fun getMyFriends(
        @AuthenticationPrincipal user: UserDetails,
    ): APIResponse<List<FriendRestrictionResponse>> {
        val myRestrictedFriends = readFriendsRestrictionUseCase.getRestrictedFriends(user.username)
        return APIResponse.success(
            myRestrictedFriends.map {
                FriendRestrictionResponse.fromDomain(it)
            }
        )
    }

    @DeleteMapping("/{friendRestrictionId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun unrestrict(
        @AuthenticationPrincipal user: UserDetails,
        @PathVariable @Validated @NotNull friendRestrictionId: Long
    ): APIResponse<FriendRestrictionDeletedResponse> {
        val deleteRestriction = updateFriendRestrictionUseCase.deleteRestriction(user.username, friendRestrictionId)
        return APIResponse.successWithHttpStatusCode(
            HttpStatus.CREATED.value(),
            FriendRestrictionDeletedResponse.fromDomain(deleteRestriction)
        )
    }
}
