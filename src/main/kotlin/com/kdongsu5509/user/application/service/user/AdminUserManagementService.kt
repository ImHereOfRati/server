package com.kdongsu5509.user.application.service.user

import com.kdongsu5509.user.application.port.`in`.user.ForceLogoutUseCase
import com.kdongsu5509.user.application.port.`in`.user.ManageUserStatusUseCase
import com.kdongsu5509.user.application.port.out.user.CachePort
import com.kdongsu5509.user.application.port.out.user.UserUpdatePort
import org.springframework.stereotype.Service

@Service
class AdminUserManagementService(
    private val cachePort: CachePort,
    private val userUpdatePort: UserUpdatePort
) : ForceLogoutUseCase, ManageUserStatusUseCase {

    override fun forceLogout(userEmail: String) {
        cachePort.delete("refresh:$userEmail")
    }

    override fun blockUser(userEmail: String) {
        userUpdatePort.block(userEmail)
    }

    override fun unblockUser(userEmail: String) {
        userUpdatePort.unblock(userEmail)
    }
}
