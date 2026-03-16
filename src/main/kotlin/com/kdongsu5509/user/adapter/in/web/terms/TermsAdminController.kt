package com.kdongsu5509.user.adapter.`in`.web.terms

import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermVersionRequest
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermVersionUseCase
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermsDefinitionUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/user/terms")
class TermsAdminController(
    private val createTermsDefinitionUseCase: CreateTermsDefinitionUseCase,
    private val createTermVersionUseCase: CreateTermVersionUseCase,
) {
    /**
     * Create
     */
    // ?½ê? ì¢…ë¥˜
    @PostMapping("/definition")
    fun createNewTermDefinition(
        @Validated @RequestBody newTermDefinitionRequest: NewTermDefinitionRequest
    ): APIResponse<Unit> {
        createTermsDefinitionUseCase.createNewTermsDefinition(newTermDefinitionRequest)
        return APIResponse.success()
    }

    // ?½ê? ?¸ë? ?´ìš©
    @PostMapping("/version")
    fun createNewTermVersion(
        @Validated @RequestBody newTermVersionRequest: NewTermVersionRequest
    ): APIResponse<Unit> {
        createTermVersionUseCase.createNewTermVersion(newTermVersionRequest)
        return APIResponse.success()
    }
}
