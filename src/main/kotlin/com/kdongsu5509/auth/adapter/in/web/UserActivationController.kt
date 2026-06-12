package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthResponse
import com.kdongsu5509.auth.adapter.`in`.web.dto.UserActivationRequest
import com.kdongsu5509.auth.application.port.`in`.ActivateUserUseCase
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.support.exception.ImHereBaseException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/activation", version = "1")
class UserActivationController(
    private val activateUserUseCase: ActivateUserUseCase
) {

    @PostMapping
    fun activate(
        @AuthenticationPrincipal userDetails: ImHereUserDetails?,
        @Validated @RequestBody request: UserActivationRequest
    ): OIDCAuthResponse {
        val details = userDetails ?: throw ImHereBaseException(AuthException.IMHERE_INVALID_TOKEN)
        val command = request.toCommand(details.username)
        val token = activateUserUseCase.activate(command, details.status)
        return OIDCAuthResponse.fromImHereJwtToken(token)
    }
}
