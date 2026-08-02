package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.controller.dto.FriendCandidateResponse
import com.kdongsu5509.friends.service.FriendCandidateSearchService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.SliceResponse
import com.kdongsu5509.shared.response.toOkResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@Validated
@RequestMapping("/api/users", version = "1")
class FriendCandidateController(
    private val friendCandidateSearchService: FriendCandidateSearchService,
) {
    @GetMapping(params = ["keyword"])
    fun findCandidates(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @RequestParam @NotBlank(message = "검색어(이메일 또는 닉네임)는 필수입니다.") keyword: String,
        @PageableDefault(size = 15) pageable: Pageable,
    ): ResponseEntity<ApiResponse<SliceResponse<FriendCandidateResponse>>> {
        val candidates = friendCandidateSearchService.search(userId, keyword, pageable)
        return SliceResponse.from(
            candidates.map {
                FriendCandidateResponse.from(it)
            }
        ).toOkResponse()
    }
}
