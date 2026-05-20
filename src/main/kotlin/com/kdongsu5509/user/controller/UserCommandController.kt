package com.kdongsu5509.user.controller

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.user.controller.dto.CompactUserResponse
import com.kdongsu5509.user.controller.dto.UserUpdateRequest
import com.kdongsu5509.user.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users", version = "1")
class UserCommandController(
    private val userService: UserService
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

    //    @DeleteMapping("/{email}/token")
//    fun forceLogout(@PathVariable email: String): ResponseEntity<ApiResponse<Unit>> {
////        forceLogoutUseCase.logout(email)
//        return Unit.toOkResponse()
//    }
//
//    @PostMapping("/{email}/block")
//    fun blockUser(@PathVariable email: String): ResponseEntity<ApiResponse<Unit>> {
//        userService.block(email)
//        return Unit.toOkResponse()
//    }
//
//    @DeleteMapping("/{email}/block")
//    fun unblockUser(@PathVariable email: String): ResponseEntity<ApiResponse<Unit>> {
//        userService.unblock(email)
//        return Unit.toOkResponse()
//    }
}
