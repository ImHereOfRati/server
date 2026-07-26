package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthResponse
import com.kdongsu5509.auth.adapter.`in`.web.dto.UserActivationRequest
import com.kdongsu5509.auth.application.port.`in`.ActivateUserUseCase
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.auth.security.shared.AllowPendingUser
import com.kdongsu5509.support.exception.ImHereBaseException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/activation", version = "1")
class UserActivationController(
    private val activateUserUseCase: ActivateUserUseCase
) {
    // TODO: Agreement 기반 활성화 API로 클라이언트 전환이 완료되면 이 레거시 엔드포인트를 삭제한다.
    // 삭제 대상(main): UserActivationController, ActivateUserUseCase, ActivateUserService,
    // UserActivationRequest, UserActivationCommand
    // 삭제 대상(test): UserActivationControllerWebMvcTest, UserActivationControllerIntegrationTest,
    // ActivateUserServiceTest
    @Deprecated(
        message = "대체할 예정입니다! Agreement 모듈에 별도로 분리할게요.",
        replaceWith = ReplaceWith(
            imports = arrayOf("agreements"),
            expression = "사용자 활성화 및 토큰 재발급이라는 측면에서 과도하다고 판단하여 분리합니다"
        )
    )
    @PostMapping
    @AllowPendingUser
    fun activate(
        @AuthenticationPrincipal userDetails: ImHereUserDetails?,
        @Validated @RequestBody request: UserActivationRequest
    ): OIDCAuthResponse {

        val details = userDetails ?: throw ImHereBaseException(AuthException.IMHERE_INVALID_TOKEN)
        val userId = details.userId ?: throw ImHereBaseException(AuthException.IMHERE_INVALID_TOKEN)
        val command = request.toCommand(userId, details.username)
        val token = activateUserUseCase.activate(command, details.status)
        return OIDCAuthResponse.fromImHereJwtToken(token)
    }
}
