package com.kdongsu5509.auth.application.port.`in`

import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.UserActivationCommand

interface ActivateUserUseCase {
    fun activate(command: UserActivationCommand, userStatus: String): ImHereJwtToken
}
