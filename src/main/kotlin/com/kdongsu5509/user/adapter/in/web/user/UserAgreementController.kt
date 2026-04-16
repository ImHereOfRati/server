package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.response.APIResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.AuthenticationResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.user.application.port.`in`.user.AgreementTermUseCase
import com.kdongsu5509.user.application.port.`in`.user.ReissueJWTUseCase
import com.kdongsu5509.user.application.service.user.SimpleTokenUserDetails
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/terms", version = "1")
class UserAgreementController(
    private val agreementTermUseCase: AgreementTermUseCase,
    private val reissueJWTUseCase: ReissueJWTUseCase
) {

    @PostMapping("/consent")
    fun consentAll(
        @AuthenticationPrincipal userDetail: SimpleTokenUserDetails,
        @Validated @RequestBody userTermsConsentRequest: UserTermsConsentRequest
    ): APIResponse<AuthenticationResponse?> {
        val userEmail = userDetail.username

        print("----userEmail---- : $userEmail")

        agreementTermUseCase.consentAll(userEmail, userTermsConsentRequest)
        val jwt = reissueJWTUseCase.reissueByUserEmail(userEmail)
        return APIResponse.success(
            AuthenticationResponse(jwt.accessToken, jwt.refreshToken)
        )
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
