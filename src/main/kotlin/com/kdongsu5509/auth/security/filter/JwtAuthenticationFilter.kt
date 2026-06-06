package com.kdongsu5509.auth.security.filter

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.shared.response.APIResponseSerializers
import com.kdongsu5509.support.exception.ImHereBaseException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val tokenParser: ImHereTokenParserPort,
    private val securityWhiteList: SecurityWhiteList
) : OncePerRequestFilter() {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val AUTH_HEADER = "Authorization"
    }

    private val pathMatcher = AntPathMatcher()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return securityWhiteList.whitelist.any { path ->
            pathMatcher.match(path, request.servletPath)
        }
    }

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwt = resolveToken(request) ?: return filterChain.doFilter(request, response)

        try {
            if (!tokenParser.validate(jwt)) {
                return sendErrorResponse(response, AuthException.IMHERE_INVALID_TOKEN)
            }
        } catch (e: ImHereBaseException) {
            return sendErrorResponse(response, e.errorCode as AuthException)
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            try {
                val authentication = createAuthentication(jwt, request)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: ImHereBaseException) {
                return sendErrorResponse(response, e.errorCode as AuthException)
            } catch (e: Exception) {
                return sendErrorResponse(response, AuthException.IMHERE_ACCESS_DENIED, e.message)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTH_HEADER)
        return if (bearerToken?.startsWith(BEARER_PREFIX) == true) {
            bearerToken.removePrefix(BEARER_PREFIX)
        } else null
    }

    private fun createAuthentication(jwt: String, request: HttpServletRequest): UsernamePasswordAuthenticationToken {
        val claims = tokenParser.parse(jwt)
        val userDetails = ImHereUserDetails(
            email = claims.email,
            nickname = claims.nickname,
            role = claims.role,
            status = claims.status
        )

        validateUserStatus(userDetails)

        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities).apply {
            details = WebAuthenticationDetailsSource().buildDetails(request)
        }
    }

    private fun validateUserStatus(userDetails: ImHereUserDetails) {
        if (!userDetails.isEnabled) throw ImHereBaseException(AuthException.USER_DISABLED)
        if (!userDetails.isAccountNonLocked) throw ImHereBaseException(AuthException.USER_LOCKED)
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        authException: AuthException,
        customMessage: String? = null
    ) {
        APIResponseSerializers.writeErrorResponse(
            response = response,
            status = authException.httpStatus,
            imhereErrorCode = authException.imhereErrorCode,
            errorMessage = customMessage ?: authException.errorMessage
        )
    }
}

