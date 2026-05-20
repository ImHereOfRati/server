package com.kdongsu5509.user.controller

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.support.response.ApiResponse
import com.kdongsu5509.support.response.SliceResponse
import com.kdongsu5509.support.response.toOkResponse
import com.kdongsu5509.user.controller.dto.CompactUserResponse
import com.kdongsu5509.user.controller.dto.DetailUserResponse
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("/api/users", version = "1")
class UserReadController(
    private val userService: UserService
) {

    @GetMapping("/my")
    fun readMe(@AuthenticationPrincipal user: ImHereUserDetails): CompactUserResponse {
        val result = userService.findByEmail(user.email)
        return CompactUserResponse.from(result)
    }

    @GetMapping(params = ["keyword"])
    fun readOthers(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @RequestParam @NotBlank(message = "검색어(이메일 또는 닉네임)는 필수입니다.") keyword: String,
        @PageableDefault(size = 15) pageable: Pageable
    ): ResponseEntity<ApiResponse<SliceResponse<CompactUserResponse>>> {
        val findingUsers: Slice<UserResult> = userService.findByKeyword(user.email, keyword, pageable)
        val sliceResponse = SliceResponse.from(findingUsers.map { CompactUserResponse.from(it) })
        return sliceResponse.toOkResponse()
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun readAll(@PageableDefault(size = 15) pageable: Pageable): ResponseEntity<ApiResponse<SliceResponse<DetailUserResponse>>> {
        val findingUsers: Slice<UserResult> = userService.findAll(pageable)
        val sliceResponse = SliceResponse.from(findingUsers.map { DetailUserResponse.from(it) })
        return sliceResponse.toOkResponse()
    }
}
