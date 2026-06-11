package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.FriendRestrictionResponse
import com.kdongsu5509.friends.service.FriendRestrictionService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/friend-restrictions", version = "1")
class FriendRestrictionAdminController(
    private val friendRestrictionService: FriendRestrictionService
) {
    @GetMapping
    fun findAll(
        @PageableDefault pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<FriendRestrictionResponse>>> {
        val restrictions = friendRestrictionService.findAll(pageable)
        val sliceResponse = SliceResponse.from(restrictions.map { FriendRestrictionResponse.fromDomain(it) })
        return sliceResponse.toOkResponse()
    }

    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable @Validated id: UUID) = friendRestrictionService.deleteById(id)
}
