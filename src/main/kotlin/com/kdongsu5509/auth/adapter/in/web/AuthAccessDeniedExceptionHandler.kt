package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toFailResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import com.kdongsu5509.support.logger.logger
import jakarta.servlet.http.HttpServletRequest

@RestControllerAdvice(basePackages = ["com.kdongsu5509"])
@Order(2)
class AuthAccessDeniedExceptionHandler {
    private val log = logger()

    @ExceptionHandler(
        AuthorizationDeniedException::class,
        AccessDeniedException::class
    )
    fun handleAuthorizationDeniedException(
        e: Exception,
        request: HttpServletRequest? = null
    ): ResponseEntity<ApiResponse<Unit>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val anonymous = isAnonymousAuthentication(authentication)
        log.warn(
            "접근 권한 거부: status={}, uri={}, userId={}, authorities={}, reason={}",
            if (anonymous) 401 else 403,
            request?.requestURI,
            authentication?.name,
            authentication?.authorities?.map { it.authority },
            e.javaClass.simpleName
        )
        if (anonymous)
            return null.toFailResponse(
                status = HttpStatus.UNAUTHORIZED,
                imhereErrorCode = AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode,
                errorMessage = "인증이 필요합니다."
            )

        return null.toFailResponse(
            status = HttpStatus.FORBIDDEN,
            imhereErrorCode = AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode,
            errorMessage = "접근 권한이 없습니다."
        )
    }

    private fun isAnonymousAuthentication(authentication: Authentication?): Boolean {
        return authentication == null ||
                authentication.name == "anonymousUser" ||
                authentication is AnonymousAuthenticationToken
    }
}
