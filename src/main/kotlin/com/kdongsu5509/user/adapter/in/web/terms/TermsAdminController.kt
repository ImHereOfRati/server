package com.kdongsu5509.user.adapter.`in`.web.terms

import com.kdongsu5509.support.common.dto.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermVersionRequest
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermVersionUseCase
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermsDefinitionUseCase
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/terms")
class TermsAdminController(
    private val createTermsDefinitionUseCase: CreateTermsDefinitionUseCase,
    private val createTermVersionUseCase: CreateTermVersionUseCase,
) {
    @PostMapping("/definition")
    fun createNewTermDefinition(
        @Validated @RequestBody newTermDefinitionRequest: NewTermDefinitionRequest
    ): APIResponse<Unit> {
        createTermsDefinitionUseCase.createNewTermsDefinition(newTermDefinitionRequest)
        return APIResponse.success()
    }

    @PostMapping("/version")
    fun createNewTermVersion(
        @Validated @RequestBody newTermVersionRequest: NewTermVersionRequest
    ): APIResponse<Unit> {
        createTermVersionUseCase.createNewTermVersion(newTermVersionRequest)
        return APIResponse.success()
    }
}
