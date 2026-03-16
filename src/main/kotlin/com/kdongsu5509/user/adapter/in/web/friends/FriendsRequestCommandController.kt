package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.user.application.port.`in`.friend.CreateFriendRequestUseCase
import com.kdongsu5509.user.application.port.`in`.friend.UpdateFriendRequestUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.CreateFriendRequest
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.CreateFriendRequestResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.FriendRelationshipResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.FriendRestrictionResponse
import jakarta.validation.constraints.NotNull
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user/friends/request")
class FriendsRequestCommandController(
    private val createFriendRequestUseCase: CreateFriendRequestUseCase,
    private val updateFriendRequestUseCase: UpdateFriendRequestUseCase
) {
    /**
     * ?îÏ≤≠ ?ùÏÑ±
     */
    @PostMapping
    fun requestFriendship(
        @AuthenticationPrincipal user: UserDetails,
        @Validated @RequestBody createFriendRequest: CreateFriendRequest
    ): APIResponse<CreateFriendRequestResponse> {

        val result = createFriendRequestUseCase.request(
            user.username,
            createFriendRequest.receiverId,
            createFriendRequest.message
        )

        return APIResponse.success(
            CreateFriendRequestResponse(result.friendRequestId!!)
        )
    }

    @PostMapping("/accept/{requestId}")
    fun acceptToFriendRequest(
        @Validated
        @NotNull(message = "requestId???ÑÏàò?ÖÎãà??)
        @PathVariable
        requestId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): APIResponse<FriendRelationshipResponse> {
        val createdFriendRelationship = updateFriendRequestUseCase.acceptFriendRequest(user.username, requestId)

        return APIResponse.success(
            FriendRelationshipResponse.fromDomain(createdFriendRelationship)
        )
    }

    @PostMapping("/reject/{requestId}")
    fun rejectToFriendRequest(
        @Validated
        @NotNull(message = "requestId???ÑÏàò?ÖÎãà??)
        @PathVariable
        requestId: Long,
        @AuthenticationPrincipal user: UserDetails,
    ): APIResponse<FriendRestrictionResponse> {
        val createdRestriction = updateFriendRequestUseCase.rejectFriendRequest(user.username, requestId)
        return APIResponse.success(
            FriendRestrictionResponse.fromDomain(createdRestriction)
        )
    }
}
