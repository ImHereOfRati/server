package com.kdongsu5509.user.controller

import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import com.kdongsu5509.user.controller.dto.DetailUserResponse
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/users", version = "1")
class UserAdminController(
    private val userService: UserService
) {

    @GetMapping
    fun readAll(@PageableDefault(size = 15) pageable: Pageable): ResponseEntity<ApiResponse<SliceResponse<DetailUserResponse>>> {
        val findingUsers: Slice<UserResult> = userService.findAll(pageable)
        val sliceResponse = SliceResponse.from(findingUsers.map { DetailUserResponse.from(it) })
        return sliceResponse.toOkResponse()
    }
}
