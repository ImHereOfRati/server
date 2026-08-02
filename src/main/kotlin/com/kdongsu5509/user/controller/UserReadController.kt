package com.kdongsu5509.user.controller

import com.kdongsu5509.user.controller.dto.CompactUserResponse
import com.kdongsu5509.user.service.UserQueryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@Validated
@RequestMapping("/api/users", version = "1")
class UserReadController(
    private val userQueryService: UserQueryService
) {
    @GetMapping("/my")
    fun readMe(@AuthenticationPrincipal(expression = "userId") userId: UUID): CompactUserResponse {
        val result = userQueryService.findById(userId)
        return CompactUserResponse.from(result)
    }
}
