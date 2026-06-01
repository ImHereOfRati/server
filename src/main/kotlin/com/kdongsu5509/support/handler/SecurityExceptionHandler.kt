package com.kdongsu5509.support.handler

import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toFailResponse
import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(3)
class SecurityExceptionHandler(
    private val discordUserErrorNotifier: DiscordUserErrorNotifier
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Map<String, Any?>>> {
        log.error("Authorization Denied: ", e)
        discordUserErrorNotifier.notifyAbnormalAccess(
            request,
            CommonErrorCode.FORBIDDEN.imhereErrorCode,
            "접근 권한이 없습니다."
        )

        return null.toFailResponse(
            status = CommonErrorCode.FORBIDDEN.httpStatus,
            imhereErrorCode = CommonErrorCode.FORBIDDEN.imhereErrorCode,
            errorMessage = CommonErrorCode.FORBIDDEN.errorMessage
        )
    }
}
