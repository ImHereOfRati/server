package com.kdongsu5509.auth.application.port.`in`

import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.UserActivationCommand

interface ActivateUserUseCase {
    fun activate(command: UserActivationCommand): ImHereJwtToken
}
