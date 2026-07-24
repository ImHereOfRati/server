package com.kdongsu5509.agreement.controller

import com.kdongsu5509.agreement.controller.dto.AgreementConsentRequest
import com.kdongsu5509.agreement.controller.dto.AgreementHistoryResponse
import com.kdongsu5509.agreement.service.AgreementService
import com.kdongsu5509.shared.AllowPendingUser
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/agreements", version = "1")
class AgreementController(
    private val agreementService: AgreementService,
) {
    @GetMapping
    fun findHistory(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
    ): List<AgreementHistoryResponse> =
        agreementService.findHistory(userId)
            .map { AgreementHistoryResponse.from(it) }

    @PostMapping
    @AllowPendingUser
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun consent(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @Validated @RequestBody request: AgreementConsentRequest,
    ) {
        agreementService.consent(userId, request.toCommand())
    }

    @PostMapping("/renewals/{termId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun consentToRenewedTerm(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable termId: Long,
    ) {
        agreementService.consentToRenewedTerm(userId, termId)
    }

    @DeleteMapping("/{termId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdrawAgreement(
        @AuthenticationPrincipal(expression = "userId") userId: UUID,
        @PathVariable termId: Long,
    ) {
        agreementService.withdraw(userId, termId)
    }
}
