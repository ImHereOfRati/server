package com.kdongsu5509.user.api

interface UserRegistrationContract {
    fun register(command: RegisterUserCommand): UserResult
}
