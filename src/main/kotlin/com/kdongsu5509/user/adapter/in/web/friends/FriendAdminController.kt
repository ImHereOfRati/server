package com.kdongsu5509.user.adapter.`in`.web.friends

import com.kdongsu5509.support.common.dto.APIResponse
import com.kdongsu5509.user.application.port.`in`.friend.AdminFriendManagementUseCase
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/friends")
class FriendAdminController(
    private val adminFriendManagementUseCase: AdminFriendManagementUseCase
) {
    @DeleteMapping
    fun forceClearFriendRelationship(
        @RequestParam userA: String,
        @RequestParam userB: String
    ): APIResponse<Unit> {
        adminFriendManagementUseCase.forceClearFriendRelationship(userA, userB)
        return APIResponse.success()
    }

    @DeleteMapping("/requests")
    fun forceClearFriendRequest(
        @RequestParam requester: String,
        @RequestParam receiver: String
    ): APIResponse<Unit> {
        adminFriendManagementUseCase.forceClearFriendRequest(requester, receiver)
        return APIResponse.success()
    }
}
