package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.FriendshipResponse
import com.kdongsu5509.friends.service.FriendshipAdminService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/friendships", version = "1")
class FriendshipAdminController(
    private val friendshipAdminService: FriendshipAdminService
) {
    @GetMapping
    fun readAll(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendshipResponse>>> {
        val friendships = friendshipAdminService.findAll(pageable)
        val sliceResponse = SliceResponse.from(friendships.map { FriendshipResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @DeleteMapping("/{id}")
    fun deleteFriendship(@PathVariable id: UUID): ResponseEntity<Unit> {
        friendshipAdminService.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
