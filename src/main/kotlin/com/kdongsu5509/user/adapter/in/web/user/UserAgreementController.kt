package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.user.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.user.application.port.`in`.user.AgreementTermUseCase
import com.kdongsu5509.user.adapter.`in`.web.common.APIResponse
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
     * ?¬ìš©?ê? ?Œì› ?•ë³´ë¥??™ì˜?˜ëŠ” ê²ƒì„ ë°›ëŠ” `API` ?¬ì¸?¸ì…?ˆë‹¤.
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
        @PathVariable @Validated @NotNull @Positive(message = "?¬ë°”ë¥??½ê? IDê°€ ?„ë‹™?ˆë‹¤") termDefinitionId: Long
    ): APIResponse<Unit> {
        agreementTermUseCase.consent(userDetail.username, termDefinitionId)
        return APIResponse.success()
    }
}
