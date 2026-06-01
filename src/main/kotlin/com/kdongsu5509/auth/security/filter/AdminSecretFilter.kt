package com.kdongsu5509.auth.security.filter

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.shared.response.APIResponseSerializers
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class AdminSecretFilter(private val adminSecret: String) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.requestURI == "/api/admin/auth/ott" && request.method == "POST") {
            val receivedHeader = request.getHeader("X-ADMIN-SECRET")

            if (receivedHeader == null || receivedHeader != adminSecret) {
                logger.warn("부적절한 관리자 접근 시도 감지: IP ${request.remoteAddr}")
                val error = AuthException.ADMIN_SECRET_INVALID
                APIResponseSerializers.writeErrorResponse(
                    response = response,
                    status = error.httpStatus,
                    imhereErrorCode = error.imhereErrorCode,
                    errorMessage = error.errorMessage
                )
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}
