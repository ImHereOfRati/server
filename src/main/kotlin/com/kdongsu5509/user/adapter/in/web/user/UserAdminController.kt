package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.common.dto.APIResponse
import com.kdongsu5509.user.application.port.`in`.user.ForceLogoutUseCase
import com.kdongsu5509.user.application.port.`in`.user.ManageUserStatusUseCase
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
    fun forceLogout(@PathVariable email: String): APIResponse<Unit> {
        forceLogoutUseCase.forceLogout(email)
        return APIResponse.success()
    }

    @PostMapping("/{email}/block")
    fun blockUser(@PathVariable email: String): APIResponse<Unit> {
        manageUserStatusUseCase.blockUser(email)
        return APIResponse.success()
    }

    @DeleteMapping("/{email}/block")
    fun unblockUser(@PathVariable email: String): APIResponse<Unit> {
        manageUserStatusUseCase.unblockUser(email)
        return APIResponse.success()
    }
}
