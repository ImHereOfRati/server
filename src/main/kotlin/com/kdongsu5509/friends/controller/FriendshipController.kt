package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.FriendshipResponse
import com.kdongsu5509.friends.controller.dto.UpdateAliasRequest
import com.kdongsu5509.friends.service.FriendshipService
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
@RequestMapping("/api/friendships", version = "1")
class FriendshipController(
    private val friendshipService: FriendshipService
) {
    @GetMapping
    fun readAll(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> {
        val friendships = friendshipService.findAllByOwnerEmail(userEmail, pageable)
        val sliceResponse = SliceResponse.from(friendships.map { FriendshipResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping("/target/{targetUserId}")
    fun checkFriendStatus(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @PathVariable targetUserId: UUID
    ): ResponseEntity<ApiResponse<Boolean>> {
        val friendship = friendshipService.findByOwnerEmailAndFriendId(userEmail, targetUserId)
        return (friendship != null).toOkResponse()
    }

    @GetMapping("/{id}")
    fun readById(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @PathVariable id: UUID
    ): FriendshipResponse {
        val result = friendshipService.findByIdAndOwnerEmail(id, userEmail)
        return FriendshipResponse.from(result)
    }

    @DeleteMapping("/{id}")
    fun deleteFriendship(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        friendshipService.deleteByIdAndOwnerEmail(id, userEmail)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/alias")
    fun updateAlias(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @PathVariable id: UUID,
        @Validated @RequestBody request: UpdateAliasRequest
    ): FriendshipResponse {
        val result = friendshipService.updateAliasByIdAndOwnerEmail(id, userEmail, request.alias)
        return FriendshipResponse.from(result)
    }

    @PostMapping("/{id}/block")
    fun blockFriend(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @PathVariable id: UUID
    ) = friendshipService.blockByIdAndOwnerEmail(id, userEmail)
}
