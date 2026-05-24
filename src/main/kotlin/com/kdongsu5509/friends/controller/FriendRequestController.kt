package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.friends.controller.dto.*
import com.kdongsu5509.friends.service.FriendRequestService
import com.kdongsu5509.support.response.ApiResponse
import com.kdongsu5509.support.response.SliceResponse
import com.kdongsu5509.support.response.toOkResponse
import jakarta.validation.constraints.NotNull
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/friends/requests", version = "1")
class FriendRequestController(
    private val friendRequestService: FriendRequestService
) {
    @PostMapping
    fun request(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: NewFriendRequest
    ): NewFriendRequestResponse {
        val result = friendRequestService.request(user.email, request.targetId, request.message)
        return NewFriendRequestResponse(result.id!!)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    fun findAll(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRequestResponse>>> {
        val requests = friendRequestService.findAll(pageable)
        val sliceResponse = SliceResponse.from(requests.map { FriendRequestResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping(params = ["type"])
    fun findSentOrReceivedAll(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @RequestParam type: FriendRequestViewType,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRequestResponse>>> {
        val requests = friendRequestService.findAllByEmailAndType(userDetails.email, type, pageable)
        val sliceResponse = SliceResponse.from(requests.map { FriendRequestResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping("/{id}")
    fun readById(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @Validated @NotNull @PathVariable id: UUID
    ): FriendRequestResponse {
        val result = friendRequestService.findByIdAndParticipantEmail(id, userDetails.email)
        return FriendRequestResponse.from(result)
    }

    @PostMapping("/{id}/accept")
    fun acceptFriendRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: ImHereUserDetails
    ): FriendshipResponse {
        val result = friendRequestService.acceptRequest(userDetails.email, id)
        return FriendshipResponse.from(result)
    }

    @PostMapping("/{id}/reject")
    fun rejectFriendRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: ImHereUserDetails
    ): FriendRestrictionResponse {
        val result = friendRequestService.rejectRequest(userDetails.email, id)
        return FriendRestrictionResponse.fromDomain(result)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, @AuthenticationPrincipal userDetails: ImHereUserDetails) =
        friendRequestService.deleteByIdAndReceiverEmail(id, userDetails.email)


    @DeleteMapping("/{id}/sent")
    fun cancelSentRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: ImHereUserDetails
    ) = friendRequestService.deleteByIdAndRequesterEmail(id, userDetails.email)

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    fun deleteById(@PathVariable id: UUID) = friendRequestService.deleteById(id)
}
