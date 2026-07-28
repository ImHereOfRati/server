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
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * 관리자 관계 API.
 *
 * 요청·친구·제한 관리자 컨트롤러가 따로 있었지만 하는 일이 목록과 삭제로 같았다.
 * 경로와 응답은 그대로 두고 클래스만 합친다.
 *
 * 목록과 삭제는 서비스가 갈라졌지만 화면 계약은 그대로다. 관리자 화면 하나에서 두 가지를
 * 다 하므로 컨트롤러는 합쳐 두고 주입만 나눈다.
 */
@RestController
@RequestMapping(version = "1")
class FriendRelationAdminController(
    private val friendRelationAdminQueryService: FriendRelationAdminQueryService,
    private val friendRelationAdminCommandService: FriendRelationAdminCommandService
) {

    @GetMapping("/api/admin/friend-requests")
    fun findAllRequests(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRequestResponse>>> =
        SliceResponse.from(
            friendRelationAdminQueryService.findAllRequests(pageable).map { FriendRequestResponse.from(it) }
        ).toOkResponse()

    @DeleteMapping("/api/admin/friend-requests/{id}")
    fun deleteRequestById(@PathVariable id: UUID) = friendRelationAdminCommandService.deleteById(id)

    @GetMapping("/api/admin/friendships")
    fun findAllFriendships(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> =
        SliceResponse.from(
            friendRelationAdminQueryService.findAllFriendships(pageable).map { FriendshipResponse.from(it) }
        ).toOkResponse()

    @DeleteMapping("/api/admin/friendships/{id}")
    fun deleteFriendshipById(@PathVariable id: UUID) = friendRelationAdminCommandService.deleteById(id)

    @GetMapping("/api/admin/friend-restrictions")
    fun findAllRestrictions(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRestrictionResponse>>> =
        SliceResponse.from(
            friendRelationAdminQueryService.findAllRestrictions(pageable).map { FriendRestrictionResponse.fromDomain(it) }
        ).toOkResponse()

    @DeleteMapping("/api/admin/friend-restrictions/{id}")
    fun deleteRestrictionById(@PathVariable @Validated id: UUID) = friendRelationAdminCommandService.deleteById(id)
}
