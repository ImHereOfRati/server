package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthResponse
import com.kdongsu5509.auth.adapter.`in`.web.dto.TokenRefreshRequest
import com.kdongsu5509.auth.application.port.`in`.TokenRefreshUseCase
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/refresh", version = "1")
class RefreshController(
    private val tokenRefreshUseCase: TokenRefreshUseCase
) {
    @PostMapping
    fun login(@Validated @RequestBody request: TokenRefreshRequest): OIDCAuthResponse {
        val imHereJwtToken = tokenRefreshUseCase.refresh(request.refreshToken)
        return OIDCAuthResponse.fromImHereJwtToken(imHereJwtToken)
    }
}
