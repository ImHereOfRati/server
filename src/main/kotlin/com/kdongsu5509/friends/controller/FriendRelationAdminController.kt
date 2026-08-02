package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.FriendRequestResponse
import com.kdongsu5509.friends.controller.dto.FriendRestrictionResponse
import com.kdongsu5509.friends.controller.dto.FriendshipResponse
import com.kdongsu5509.friends.service.FriendRelationAdminCommandService
import com.kdongsu5509.friends.service.FriendRelationAdminQueryService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin", version = "1")
class FriendRelationAdminController(
    private val friendRelationAdminQueryService: FriendRelationAdminQueryService,
    private val friendRelationAdminCommandService: FriendRelationAdminCommandService
) {

    @GetMapping("/friend-requests")
    fun findAllRequests(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRequestResponse>>> =
        SliceResponse.from(
            friendRelationAdminQueryService.findAllRequests(pageable).map { FriendRequestResponse.from(it) }
        ).toOkResponse()

    @ResponseStatus(NO_CONTENT)
    @DeleteMapping("/friend-requests/{id}")
    fun deleteRequestById(@PathVariable id: UUID) = friendRelationAdminCommandService.deleteById(id)

    @GetMapping("/friendships")
    fun findAllFriendships(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> =
        SliceResponse.from(
            friendRelationAdminQueryService.findAllFriendships(pageable).map { FriendshipResponse.from(it) }
        ).toOkResponse()

    @ResponseStatus(NO_CONTENT)
    @DeleteMapping("/friendships/{id}")
    fun deleteFriendshipById(@PathVariable id: UUID) = friendRelationAdminCommandService.deleteById(id)

    @GetMapping("/friend-restrictions")
    fun findAllRestrictions(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRestrictionResponse>>> =
        SliceResponse.from(
            friendRelationAdminQueryService.findAllRestrictions(pageable)
                .map { FriendRestrictionResponse.fromDomain(it) }
        ).toOkResponse()

    @ResponseStatus(NO_CONTENT)
    @DeleteMapping("/friend-restrictions/{id}")
    fun deleteRestrictionById(@PathVariable @Validated id: UUID) = friendRelationAdminCommandService.deleteById(id)
}
