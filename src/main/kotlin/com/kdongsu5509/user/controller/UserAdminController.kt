package com.kdongsu5509.user.controller

import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.controller.dto.DetailUserResponse
import com.kdongsu5509.user.service.UserLifecycleService
import com.kdongsu5509.user.service.UserQueryService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/users", version = "1")
class UserAdminController(
    private val userQueryService: UserQueryService,
    private val userLifecycleService: UserLifecycleService,
) {
    @GetMapping
    fun readAll(@PageableDefault(size = 15) pageable: Pageable): ResponseEntity<ApiResponse<SliceResponse<DetailUserResponse>>> {
        val findingUsers: Slice<UserResult> = userQueryService.findAll(pageable)
        val sliceResponse = SliceResponse.from(findingUsers.map { DetailUserResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @PostMapping("/{email}/block")
    fun blockUser(@PathVariable email: String) {
        userLifecycleService.block(email)
    }

    @DeleteMapping("/{email}/block")
    fun unblockUser(@PathVariable email: String) {
        userLifecycleService.unblock(email)
    }

    @DeleteMapping("/{email}/token")
    fun forceLogout(@PathVariable email: String) {
        userLifecycleService.requestForceLogout(email)
    }

    @DeleteMapping("/{email}")
    fun withdrawUser(@PathVariable email: String) {
        userLifecycleService.withdraw(email)
    }
}
