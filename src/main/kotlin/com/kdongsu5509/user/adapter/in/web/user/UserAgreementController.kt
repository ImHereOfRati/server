package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.response.APIResponseBody
import com.kdongsu5509.support.response.toOkResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.request.UserTermsConsentRequest
import com.kdongsu5509.user.adapter.`in`.web.user.dto.response.AuthenticationResponse
import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.port.`in`.user.AgreementTermUseCase
import com.kdongsu5509.user.application.port.`in`.user.ReissueJWTUseCase
import com.kdongsu5509.user.application.service.user.SimpleTokenUserDetails
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/me/agreements", version = "1")
class UserAgreementController(
    private val agreementTermUseCase: AgreementTermUseCase,
    private val reissueJWTUseCase: ReissueJWTUseCase
) {

    /**
     * 약관 일괄 동의
     */
    @PostMapping
    fun consentAll(
        @AuthenticationPrincipal userDetail: SimpleTokenUserDetails,
        @Validated @RequestBody userTermsConsentRequest: UserTermsConsentRequest
    ): ResponseEntity<APIResponseBody<AuthenticationResponse>> {
        val userEmail = userDetail.username

        agreementTermUseCase.consentAll(userEmail, userTermsConsentRequest)
        val imHereJwt: ImHereJwt = reissueJWTUseCase.reissueByUserEmail(userEmail)

        return AuthenticationResponse.fromImHereJwt(imHereJwt).toOkResponse()
    }

    /**
     * 개별 약관 동의
     */
    @PostMapping("/{termDefinitionId}")
    fun consentSingle(
        @AuthenticationPrincipal userDetail: UserDetails,
        @PathVariable @Validated @NotNull @Positive(message = "약관 ID는 양의 정수입니다") termDefinitionId: Long
    ): ResponseEntity<APIResponseBody<Unit>> {
        agreementTermUseCase.consent(userDetail.username, termDefinitionId)
        return Unit.toOkResponse()
    }
}
