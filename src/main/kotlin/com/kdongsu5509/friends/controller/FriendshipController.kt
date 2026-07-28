package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.FriendshipResponse
import com.kdongsu5509.friends.controller.dto.UpdateAliasRequest
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
@RequestMapping("/api/friendships", version = "1")
class FriendshipController(
    private val friendRelationCommandService: FriendRelationCommandService,
    private val friendRelationQueryService: FriendRelationQueryService
) {
    @GetMapping
    fun readAll(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> {
        val friendships = friendRelationQueryService.findFriends(userId, pageable)
        val sliceResponse = SliceResponse.from(friendships.map { FriendshipResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping("/target/{targetUserId}")
    fun checkFriendStatus(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable targetUserId: UUID
    ): ResponseEntity<ApiResponse<Boolean>> {
        val friendship = friendRelationQueryService.findFriendByTarget(userId, targetUserId)
        return (friendship != null).toOkResponse()
    }

    @GetMapping("/{id}")
    fun readById(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable id: UUID
    ): FriendshipResponse {
        val result = friendRelationQueryService.findFriend(id, userId)
        return FriendshipResponse.from(result)
    }

    @DeleteMapping("/{id}")
    fun deleteFriendship(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        friendRelationCommandService.deleteFriendship(id, userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/alias")
    fun updateAlias(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable id: UUID,
        @Validated @RequestBody request: UpdateAliasRequest
    ): FriendshipResponse {
        val result = friendRelationCommandService.updateAlias(id, userId, request.alias)
        return FriendshipResponse.from(result)
    }
}
