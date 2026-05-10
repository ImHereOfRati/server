package com.kdongsu5509.support.exception.handler

import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.response.APIResponseBody
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
) : AbstractExceptionHandler() {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<APIResponseBody<Map<String, Any?>>> {
        log.error("Authorization Denied: ", e)
        discordUserErrorNotifier.notifyAbnormalAccess(request, ErrorReason.FORBIDDEN.name, "접근 권한이 없습니다.")
        return createErrorResponse(ErrorReason.FORBIDDEN, "해당 리소스에 접근할 권한이 없습니다.")
    }
}
