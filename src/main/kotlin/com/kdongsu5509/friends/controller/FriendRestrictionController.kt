package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.friends.controller.dto.CreateFriendRestrictionRequest
import com.kdongsu5509.friends.controller.dto.FriendRestrictionResponse
import com.kdongsu5509.friends.service.FriendRestrictionService
import com.kdongsu5509.support.response.ApiResponse
import com.kdongsu5509.support.response.SliceResponse
import com.kdongsu5509.support.response.toOkResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/friends/restrictions", version = "1")
class FriendRestrictionController(
    private val friendRestrictionService: FriendRestrictionService
) {
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRestrictionResponse>>> {
        val restrictions = friendRestrictionService.findAllByRestrictorEmail(userDetails.email, pageable)
        val sliceResponse = SliceResponse.from(restrictions.map { FriendRestrictionResponse.fromDomain(it) })
        return sliceResponse.toOkResponse()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    fun findAll(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRestrictionResponse>>> {
        val restrictions = friendRestrictionService.findAll(pageable)
        val sliceResponse = SliceResponse.from(restrictions.map { FriendRestrictionResponse.fromDomain(it) })
        return sliceResponse.toOkResponse()
    }

    @PostMapping
    fun restrictUser(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @Validated @RequestBody request: CreateFriendRestrictionRequest
    ): FriendRestrictionResponse {
        val result = friendRestrictionService.restrictUser(userDetails.email, request.targetUserId)
        return FriendRestrictionResponse.fromDomain(result)
    }

    @GetMapping("/target/{targetUserId}")
    fun checkRestrictionStatus(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable targetUserId: UUID
    ): Boolean = friendRestrictionService.existRestricted(userDetails.email, targetUserId)

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable @Validated id: UUID
    ) = friendRestrictionService.deleteByIdAndRestrictorEmail(id, userDetails.email)

    @DeleteMapping("/blocked-users/{restrictedId}")
    fun unblock(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable @Validated restrictedId: UUID
    ) = friendRestrictionService.unblockByRestrictorEmailAndRestrictedId(
        userDetails.email,
        restrictedId
    )

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    fun deleteById(@PathVariable @Validated id: UUID) =
        friendRestrictionService.deleteById(id)
}
