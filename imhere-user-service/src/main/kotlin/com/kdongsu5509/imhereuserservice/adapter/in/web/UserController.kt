package com.kdongsu5509.imhereuserservice.adapter.`in`.web

import com.kdongsu5509.imhereuserservice.adapter.dto.resp.UserSearchResponse
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.UserSearchUseCase
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/search")
class UserController(private val userSearchUseCase: UserSearchUseCase) {

    @GetMapping("/{keyword}")
    fun searchUsers(
        @PathVariable @NotBlank(message = "이메일 혹은 사용자 닉네임을 입력하여야 합니다")
        keyword: String
    ): APIResponse<List<UserSearchResponse>> {
        val findingUsers = userSearchUseCase.searchUser(keyword)

        val responseValue: List<UserSearchResponse> = findingUsers.map { user ->
            UserSearchResponse(user.email, user.nickname)
        }

        return APIResponse.success(responseValue)
    }

    @GetMapping("/me")
    fun searchMyInfo(
        @AuthenticationPrincipal user: UserDetails
    ): APIResponse<UserSearchResponse> {
        val myInfo = userSearchUseCase.searchMe(user.username)

        return APIResponse.success(
            UserSearchResponse(
                myInfo.email, myInfo.nickname
            )
        )
    }
}
