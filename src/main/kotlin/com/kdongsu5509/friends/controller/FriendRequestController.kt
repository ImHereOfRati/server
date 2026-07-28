package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.*
import com.kdongsu5509.friends.domain.FriendRequestViewType
import com.kdongsu5509.friends.service.FriendRelationCommandService
import com.kdongsu5509.friends.service.FriendRelationQueryService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import jakarta.validation.constraints.NotNull
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/friends/requests", version = "1")
class FriendRequestController(
    private val friendRelationCommandService: FriendRelationCommandService,
    private val friendRelationQueryService: FriendRelationQueryService
) {
    @PostMapping
    fun request(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @Validated @RequestBody request: NewFriendRequest
    ): NewFriendRequestResponse {
        val result = friendRelationCommandService.sendRequest(userId, request.targetId, request.message)
        return NewFriendRequestResponse(result.id!!)
    }

    @GetMapping(params = ["type"])
    fun findSentOrReceivedAll(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @RequestParam type: FriendRequestViewType,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRequestResponse>>> {
        val requests = friendRelationQueryService.findRequests(userId, type, pageable)
        val sliceResponse = SliceResponse.from(requests.map { FriendRequestResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping("/{id}")
    fun readById(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @Validated @NotNull @PathVariable id: UUID
    ): FriendRequestResponse {
        val result = friendRelationQueryService.findRequest(id, userId)
        return FriendRequestResponse.from(result)
    }

    @PostMapping("/{id}/accept")
    fun acceptFriendRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
    ): FriendshipResponse {
        val result = friendRelationCommandService.acceptRequest(id, userId)
        return FriendshipResponse.from(result)
    }

    @PostMapping("/{relationId}/reject")
    fun rejectFriendRequest(
        @PathVariable relationId: UUID,
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
    ): FriendRestrictionResponse {
        val result = friendRelationCommandService.rejectRequest(relationId, userId)
        return FriendRestrictionResponse.fromDomain(result)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
    ) =
        friendRelationCommandService.deleteReceivedRequest(id, userId)


    @DeleteMapping("/{id}/sent")
    fun cancelSentRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
    ) = friendRelationCommandService.cancelSentRequest(id, userId)
}
