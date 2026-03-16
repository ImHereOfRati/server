package com.kdongsu5509.user.adapter.`in`.web.terms

import com.kdongsu5509.user.adapter.`in`.web.terms.dto.TermDefinitionResponse
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.TermVersionResponse
import com.kdongsu5509.user.application.port.`in`.terms.ReadTermsDefinitionUseCase
import com.kdongsu5509.user.application.port.`in`.terms.ReadTermsVersionUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/terms")
class TermsController(
    private val readTermsDefinitionUseCase: ReadTermsDefinitionUseCase,
    private val readTermsVersionUseCase: ReadTermsVersionUseCase
) {
    /**
     * READ
     */
    // ?„ìž¬ ?½ê? ì¢…ë¥˜ ?„ì²´ ì¡°íšŒ
    @GetMapping
    fun readAllTermsDefinitions(
        @PageableDefault(size = 10) pageable: Pageable
    ): APIResponse<Page<TermDefinitionResponse>> {
        val response = readTermsDefinitionUseCase.readAll(pageable)
        return APIResponse.success(response)
    }

    //?½ê? ë²„ì „ ?¸ë? ?´ìš© ì¡°íšŒ
    @GetMapping("/version/{termDefinitionId}")
    fun readTermsVersion(
        @PathVariable termDefinitionId: Long
    ): APIResponse<TermVersionResponse> {
        val result = readTermsVersionUseCase.read(termDefinitionId)
        return APIResponse.success(result)
    }
}
