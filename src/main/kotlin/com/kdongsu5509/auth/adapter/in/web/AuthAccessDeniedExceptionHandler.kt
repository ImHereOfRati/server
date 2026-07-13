package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toFailResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 인증/인가 실패(401/403) 응답을 auth 모듈이 소유한다.
 *
 * 과거에는 support의 GlobalExceptionHandler가 auth의 [AuthException] 에러코드를 하드코딩해
 * support→auth 순환 의존이 있었다. [AuthException]을 정당하게 소유하는 auth 모듈로 이 판정을 옮겨 순환을 끊는다.
 * (응답 포맷·imhereErrorCode 문자열은 클라이언트 계약이므로 불변으로 유지한다.)
 */
@RestControllerAdvice(basePackages = ["com.kdongsu5509"])
class AuthAccessDeniedExceptionHandler {

    @ExceptionHandler(
        AuthorizationDeniedException::class,
        AccessDeniedException::class
    )
    fun handleAuthorizationDeniedException(e: Exception): ResponseEntity<ApiResponse<Unit>> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null ||
            authentication.name == "anonymousUser" ||
            authentication is AnonymousAuthenticationToken
        ) {
            return null.toFailResponse(
                status = HttpStatus.UNAUTHORIZED,
                imhereErrorCode = AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode,
                errorMessage = "인증이 필요합니다."
            )
        }
        return null.toFailResponse(
            status = HttpStatus.FORBIDDEN,
            imhereErrorCode = AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode,
            errorMessage = "접근 권한이 없습니다."
        )
    }
}
