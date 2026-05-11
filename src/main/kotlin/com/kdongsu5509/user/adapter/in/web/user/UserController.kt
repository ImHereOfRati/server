package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.response.APIResponseBody
import com.kdongsu5509.support.response.toOkResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.request.UpdateUserRequest
import com.kdongsu5509.user.adapter.`in`.web.user.dto.response.UserInfoResponse
import com.kdongsu5509.user.application.dto.UserResponse
import com.kdongsu5509.user.application.port.`in`.user.ReadUserUseCase
import com.kdongsu5509.user.application.port.`in`.user.UpdateUserUseCase
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/info", version = "1")
class UserController(
    private val readUserUseCase: ReadUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) {

    @GetMapping("/me")
    fun readMyInformation(@AuthenticationPrincipal user: UserDetails): ResponseEntity<APIResponseBody<UserInfoResponse>> {
        val myInfo = readUserUseCase.findByEmail(user.username)
        return UserInfoResponse.fromUserResponse(myInfo).toOkResponse()
    }

    @PatchMapping("/me")
    fun updateMyInformation(
        @AuthenticationPrincipal user: UserDetails,
        @Validated @RequestBody updateUserRequest: UpdateUserRequest
    ): ResponseEntity<APIResponseBody<UserInfoResponse>> {
        val nickname = updateUserRequest.nickname

        val userInfo = if (nickname != null) {
            updateUserUseCase.changeNickName(user.username, nickname)
        } else {
            readUserUseCase.findByEmail(user.username)
        }

        return UserInfoResponse.fromUserResponse(userInfo).toOkResponse()
    }

    @GetMapping
    fun readUsers(
        @AuthenticationPrincipal user: UserDetails,
        @RequestParam @NotBlank(message = "검색어(이메일 또는 닉네임)는 필수입니다.") keyword: String
    ): ResponseEntity<APIResponseBody<List<UserInfoResponse>>> {
        val findingUsers: List<UserResponse> = readUserUseCase.findByEmailAndNickname(user.username, keyword)
        return findingUsers.map { UserInfoResponse.fromUserResponse(it) }.toOkResponse()
    }
}
