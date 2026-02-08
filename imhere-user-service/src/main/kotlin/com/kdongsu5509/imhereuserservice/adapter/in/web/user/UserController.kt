package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.common.APIResponse
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.friends.dto.UserSearchResponse
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.NicknameChangeRequest
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.ReadUserUseCase
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.UpdateUserUseCase
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user/info")
class UserController(
    private val readUserUseCase: ReadUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) {

    /**
     * 나의 정보 조회
     */
    @GetMapping("/me")
    fun searchMyInfo(
        @AuthenticationPrincipal user: UserDetails
    ): APIResponse<UserSearchResponse> {
        val myInfo = readUserUseCase.searchMe(user.username)

        return APIResponse.success(
            UserSearchResponse(
                myInfo.email, myInfo.nickname
            )
        )
    }

    /**
     * 나의 닉네임 변경
     */
    @PostMapping("/nickname")
    fun changeNickName(
        @AuthenticationPrincipal user: UserDetails,
        @Validated @RequestBody newNicknameChangeRequest: NicknameChangeRequest
    ): APIResponse<UserSearchResponse> {
        val updatedMyInfo = updateUserUseCase.changeNickName(
            user.username,
            newNicknameChangeRequest.newNickname
        )

        return APIResponse.success(
            UserSearchResponse(
                updatedMyInfo.email, updatedMyInfo.nickname
            )
        )
    }

    /**
     * 사용자 조회
     */
    @GetMapping("/{keyword}")
    fun searchUsers(
        @PathVariable @NotBlank(message = "이메일 혹은 사용자 닉네임을 입력하여야 합니다")
        keyword: String
    ): APIResponse<List<UserSearchResponse>> {
        val findingUsers = readUserUseCase.searchUser(keyword)

        val responseValue: List<UserSearchResponse> = findingUsers.map { user ->
            UserSearchResponse(user.email, user.nickname)
        }

        return APIResponse.success(responseValue)
    }
}