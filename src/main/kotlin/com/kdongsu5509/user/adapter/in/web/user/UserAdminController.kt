package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.response.APIResponseBody
import com.kdongsu5509.support.response.toOkResponse
import com.kdongsu5509.user.application.port.`in`.user.ForceLogoutUseCase
import com.kdongsu5509.user.application.port.`in`.user.ManageUserStatusUseCase
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/users")
class UserAdminController(
    private val forceLogoutUseCase: ForceLogoutUseCase,
    private val manageUserStatusUseCase: ManageUserStatusUseCase
) {
    @DeleteMapping("/{email}/token")
    fun forceLogout(@PathVariable email: String): ResponseEntity<APIResponseBody<Unit>> {
        forceLogoutUseCase.forceLogout(email)
        return Unit.toOkResponse()
    }

    @PostMapping("/{email}/block")
    fun blockUser(@PathVariable email: String): ResponseEntity<APIResponseBody<Unit>> {
        manageUserStatusUseCase.blockUser(email)
        return Unit.toOkResponse()
    }

    @DeleteMapping("/{email}/block")
    fun unblockUser(@PathVariable email: String): ResponseEntity<APIResponseBody<Unit>> {
        manageUserStatusUseCase.unblockUser(email)
        return Unit.toOkResponse()
    }
}
