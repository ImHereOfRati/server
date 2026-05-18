package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthResponse
import com.kdongsu5509.auth.application.port.`in`.LoginUseCase
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/login", version = "1")
class LoginController(
    private val loginUseCase: LoginUseCase
) {
    @PostMapping
    fun login(@Validated @RequestBody request: OIDCAuthRequest): OIDCAuthResponse {
        val imHereJwtToken = loginUseCase.login(request.provider, request.idToken)
        return OIDCAuthResponse.fromImHereJwtToken(imHereJwtToken)
    }
}
