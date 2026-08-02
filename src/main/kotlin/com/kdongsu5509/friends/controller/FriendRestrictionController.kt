package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.CreateFriendRestrictionRequest
import com.kdongsu5509.friends.controller.dto.FriendRestrictionResponse
import com.kdongsu5509.friends.service.FriendRelationCommandService
import com.kdongsu5509.friends.service.FriendRelationQueryService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/friends/restrictions", version = "1")
class FriendRestrictionController(
    private val friendRelationCommandService: FriendRelationCommandService,
    private val friendRelationQueryService: FriendRelationQueryService
) {
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRestrictionResponse>>> {
        val restrictions = friendRelationQueryService.findRestrictions(userId, pageable)
        val sliceResponse = SliceResponse.from(restrictions.map { FriendRestrictionResponse.fromDomain(it) })
        return sliceResponse.toOkResponse()
    }

    @PostMapping
    fun block(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @Validated @RequestBody request: CreateFriendRestrictionRequest
    ): FriendRestrictionResponse {
        val result = friendRelationCommandService.block(userId, request.targetUserId)
        return FriendRestrictionResponse.fromDomain(result)
    }

    @GetMapping("/target/{targetUserId}")
    fun checkRestrictionStatus(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable targetUserId: UUID
    ): Boolean = friendRelationQueryService.existsRestriction(userId, targetUserId)

    @DeleteMapping("/blocked-users/{targetUserId}")
    fun unblock(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable @Validated targetUserId: UUID
    ) = friendRelationCommandService.unblock(userId, targetUserId)
}
