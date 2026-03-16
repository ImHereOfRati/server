package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.user.application.port.`in`.friend.ReadFriendsUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.friends.dto.FriendRelationshipResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/friends")
class FriendsReadController(
    private val readFriendsUseCase: ReadFriendsUseCase
) {
    @GetMapping
    fun getMyFriends(
        @AuthenticationPrincipal user: UserDetails,
    ): APIResponse<List<FriendRelationshipResponse>> {
        val myFriends = readFriendsUseCase.getMyFriends(user.username)
        return APIResponse.success(
            myFriends.map { it ->
                FriendRelationshipResponse.fromDomain(it)
            }
        )
    }
}
