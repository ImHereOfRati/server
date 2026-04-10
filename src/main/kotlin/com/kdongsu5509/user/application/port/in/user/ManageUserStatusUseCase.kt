package com.kdongsu5509.user.application.port.`in`.user

interface ManageUserStatusUseCase {
    fun blockUser(userEmail: String)
    fun unblockUser(userEmail: String)
}
