package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthResponse
import com.kdongsu5509.auth.application.port.`in`.RegisterUseCase
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth/registration", version = "1")
class RegistrationController(
    private val registerUseCase: RegisterUseCase
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Validated @RequestBody request: OIDCAuthRequest): OIDCAuthResponse {
        val imHereJwtToken = registerUseCase.register(request.provider, request.idToken)
        return OIDCAuthResponse.fromImHereJwtToken(imHereJwtToken)
    }
}
