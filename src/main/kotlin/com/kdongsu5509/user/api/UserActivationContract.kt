package com.kdongsu5509.user.api

import java.util.*

interface UserActivationContract {
    fun activateIfPending(userId: UUID): UserResult
}
