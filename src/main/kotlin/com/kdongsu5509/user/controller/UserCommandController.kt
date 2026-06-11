package com.kdongsu5509.user.controller

import com.kdongsu5509.auth.application.port.`in`.ForceLogoutUseCase
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toOkResponse
import com.kdongsu5509.user.controller.dto.CompactUserResponse
import com.kdongsu5509.user.controller.dto.UserUpdateRequest
import com.kdongsu5509.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users", version = "1")
class UserCommandController(
    private val userService: UserService,
    private val forceLogoutUseCase: ForceLogoutUseCase
) {
    @PatchMapping("/my")
    fun updateMe(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: UserUpdateRequest
    ): CompactUserResponse {
        val nickname = request.nickname

        val userInfo =
            if (nickname != null) {
                userService.updateNickname(user.email, nickname)
            } else {
                userService.findByEmail(user.username)
            }

        return CompactUserResponse.from(userInfo)
    }

    @DeleteMapping("/my/withdrawal")
    fun withdraw(@AuthenticationPrincipal user: ImHereUserDetails): ResponseEntity<ApiResponse<Unit>> {
        userService.withdraw(user.email)
        forceLogoutUseCase.logout(user.email)
        return Unit.toOkResponse()
    }
}
