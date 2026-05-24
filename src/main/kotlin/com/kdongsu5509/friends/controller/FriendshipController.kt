package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.friends.controller.dto.FriendshipResponse
import com.kdongsu5509.friends.controller.dto.UpdateAliasRequest
import com.kdongsu5509.friends.service.FriendshipService
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
@RequestMapping("/api/friendships", version = "1")
class FriendshipController(
    private val friendshipService: FriendshipService
) {
    @GetMapping
    fun readAll(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> {
        val friendships = friendshipService.findAllByOwnerEmail(userDetails.email, pageable)
        val sliceResponse = SliceResponse.from(friendships.map { FriendshipResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    fun readAll(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> {
        val friendships = friendshipService.findAll(pageable)
        val sliceResponse = SliceResponse.from(friendships.map { FriendshipResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping("/target/{targetUserId}")
    fun checkFriendStatus(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable targetUserId: UUID
    ): ResponseEntity<ApiResponse<Boolean>> {
        val friendship = friendshipService.findByOwnerEmailAndFriendId(userDetails.email, targetUserId)
        return (friendship != null).toOkResponse()
    }

    @GetMapping("/{id}")
    fun readById(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable id: UUID
    ): FriendshipResponse {
        val result = friendshipService.findByIdAndOwnerEmail(id, userDetails.email)
        return FriendshipResponse.from(result)
    }

    @DeleteMapping("/{id}")
    fun deleteFriendship(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        friendshipService.deleteByIdAndOwnerEmail(id, userDetails.email)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    fun deleteFriendship(@PathVariable id: UUID): ResponseEntity<Unit> {
        friendshipService.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/alias")
    fun updateAlias(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable id: UUID,
        @Validated @RequestBody request: UpdateAliasRequest
    ): FriendshipResponse {
        val result = friendshipService.updateAliasByIdAndOwnerEmail(id, userDetails.email, request.alias)
        return FriendshipResponse.from(result)
    }

    @PostMapping("/{id}/block")
    fun blockFriend(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable id: UUID
    ) = friendshipService.blockByIdAndOwnerEmail(id, userDetails.email)
}
