package com.kdongsu5509.user.application.port.`in`

interface ManageUserStatusUseCase {
    fun block(userEmail: String)
    fun unblock(userEmail: String)
}
