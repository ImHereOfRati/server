package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.common.dto.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.user.application.port.`in`.user.AgreementTermUseCase
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user/terms")
class UserAgreementController(
    private val agreementTermUseCase: AgreementTermUseCase
) {
    @PostMapping("/consent")
    fun consentAll(
        @AuthenticationPrincipal userDetail: UserDetails,
        @Validated @RequestBody userTermsConsentRequest: UserTermsConsentRequest
    ): APIResponse<Unit> {
        agreementTermUseCase.consentAll(userDetail.username, userTermsConsentRequest)
        return APIResponse.success()
    }

    @PostMapping("/consent/{termDefinitionId}")
    fun consentSingle(
        @AuthenticationPrincipal userDetail: UserDetails,
        @PathVariable @Validated @NotNull @Positive(message = "약관 ID는 양의 정수입니다") termDefinitionId: Long
    ): APIResponse<Unit> {
        agreementTermUseCase.consent(userDetail.username, termDefinitionId)
        return APIResponse.success()
    }
}
