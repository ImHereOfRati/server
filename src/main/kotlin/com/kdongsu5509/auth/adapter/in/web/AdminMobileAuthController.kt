package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.adapter.`in`.web.dto.*
import com.kdongsu5509.auth.application.service.AdminMobileAuthService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toOkResponse
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/mobile/auth", version = "1")
class AdminMobileAuthController(private val service: AdminMobileAuthService) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminMobileLoginRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AdminMobileChallengeResponse>> =
        service.begin(request.adminId, request.password, com.kdongsu5509.auth.security.ClientIpResolver.resolve(servletRequest)).toOkResponse()

    @PostMapping("/mfa/verify")
    fun verify(@Valid @RequestBody request: AdminMobileMfaRequest): ResponseEntity<ApiResponse<AdminMobileTokenResponse>> =
        service.verify(request.challenge, request.code).toOkResponse()

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: AdminMobileRefreshRequest): ResponseEntity<ApiResponse<AdminMobileTokenResponse>> =
        service.refresh(request.refreshToken).toOkResponse()

    @GetMapping("/me")
    fun me(): ResponseEntity<ApiResponse<AdminMobileSessionResponse>> = service.session().toOkResponse()
}
