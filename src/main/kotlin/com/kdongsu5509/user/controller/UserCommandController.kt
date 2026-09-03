package com.kdongsu5509.user.controller

import com.kdongsu5509.user.controller.dto.CompactUserResponse
import com.kdongsu5509.user.controller.dto.NicknameUpdateRequest
import com.kdongsu5509.user.service.UserLifecycleService
import com.kdongsu5509.user.service.UserProfileService
import com.kdongsu5509.support.security.AllowPendingUser
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users/my", version = "1")
class UserCommandController(
    private val userProfileService: UserProfileService,
    private val userLifecycleService: UserLifecycleService,
) {
    @PatchMapping
    fun updateMe(
        @AuthenticationPrincipal(expression = "email") userEmail: String,
        @Validated @RequestBody request: NicknameUpdateRequest
    ): CompactUserResponse {
        val userInfo = userProfileService.updateNickname(userEmail, request.nickname)
        return CompactUserResponse.from(userInfo)
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AllowPendingUser
    @DeleteMapping("/withdrawal")
    fun withdraw(@AuthenticationPrincipal(expression = "email") userEmail: String) {
        userLifecycleService.withdraw(userEmail)
    }
}
