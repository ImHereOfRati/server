package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.user.adapter.`in`.web.user.dto.NicknameChangeRequest
import com.kdongsu5509.user.adapter.`in`.web.user.dto.UserInfoResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.UserSearchResponse
import com.kdongsu5509.user.application.port.`in`.user.ReadUserUseCase
import com.kdongsu5509.user.application.port.`in`.user.UpdateUserUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
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
     * ?òÏùò ?ïÎ≥¥ Ï°∞Ìöå
     */
    @GetMapping("/me")
    fun searchMyInfo(
        @AuthenticationPrincipal user: UserDetails
    ): APIResponse<UserInfoResponse> {
        val myInfo = readUserUseCase.searchMe(user.username)

        return APIResponse.success(
            UserInfoResponse(
                myInfo.email, myInfo.nickname
            )
        )
    }

    /**
     * ?òÏùò ?âÎÑ§??Î≥ÄÍ≤?
     */
    @PostMapping("/nickname")
    fun changeNickName(
        @AuthenticationPrincipal user: UserDetails,
        @Validated @RequestBody newNicknameChangeRequest: NicknameChangeRequest
    ): APIResponse<UserInfoResponse> {
        val updatedMyInfo = updateUserUseCase.changeNickName(
            user.username,
            newNicknameChangeRequest.newNickname
        )

        return APIResponse.success(
            UserInfoResponse(
                updatedMyInfo.email, updatedMyInfo.nickname
            )
        )
    }

    /**
     * ?¨Ïö©??Ï°∞Ìöå
     */
    @GetMapping("/{keyword}")
    fun searchUsers(
        @AuthenticationPrincipal user: UserDetails,
        @PathVariable @NotBlank(message = "?¥Î©î???πÏ? ?¨Ïö©???âÎÑ§?ÑÏùÑ ?ÖÎ†•?òÏó¨???©Îãà??)
        keyword: String
    ): APIResponse<List<UserSearchResponse>> {
        val findingUsers = readUserUseCase.searchPotentialFriendsUser(user.username, keyword)

        return APIResponse.success(
            findingUsers.map { user ->
                UserSearchResponse(user.id, user.email, user.nickname)
            }
        )
    }
}
