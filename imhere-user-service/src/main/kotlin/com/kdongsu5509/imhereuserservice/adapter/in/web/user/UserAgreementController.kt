package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.common.APIResponse
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.AgreementTermUseCase
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
    /**
     * 사용자가 회원 정보를 동의하는 것을 받는 `API` 포인트입니다.
     */
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
        @PathVariable @Validated @NotNull @Positive(message = "올바른 약관 ID가 아닙니다") termDefinitionId: Long
    ): APIResponse<Unit> {
        agreementTermUseCase.consent(userDetail.username, termDefinitionId)
        return APIResponse.success()
    }
}