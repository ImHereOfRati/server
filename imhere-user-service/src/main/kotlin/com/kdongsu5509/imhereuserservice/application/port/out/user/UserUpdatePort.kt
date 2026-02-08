package com.kdongsu5509.imhereuserservice.application.port.out.user

interface UserUpdatePort {
    fun activate(userEmail: String)
}