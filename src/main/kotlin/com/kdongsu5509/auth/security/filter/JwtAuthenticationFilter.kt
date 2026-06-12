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
import org.slf4j.LoggerFactory
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
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    private val pathMatcher = AntPathMatcher()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val shouldSkip = securityWhiteList.whitelist.any { path ->
            pathMatcher.match(path, request.servletPath)
        }
        if (shouldSkip) {
            log.debug("JWT filter skipped for whitelisted path: {}", request.servletPath)
        }
        return shouldSkip
    }

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        log.debug("JwtAuthenticationFilter processing request: {}", request.requestURI)
        val jwt = resolveToken(request) ?: return filterChain.doFilter(request, response)

        log.debug("Resolved JWT token, validating...")
        try {
            if (!tokenParser.validate(jwt)) {
                log.warn("JWT validation failed for path: {}", request.requestURI)
                return sendErrorResponse(response, AuthException.IMHERE_INVALID_TOKEN)
            }
        } catch (e: ImHereBaseException) {
            log.warn("JWT validation exception: {}", e.errorCode)
            return sendErrorResponse(response, e.errorCode as AuthException)
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            try {
                val authentication = createAuthentication(jwt, request)
                SecurityContextHolder.getContext().authentication = authentication
                log.debug("Authentication set for user: {}", authentication.principal)
            } catch (e: ImHereBaseException) {
                log.warn("Authentication creation failed: {}", e.errorCode)
                return sendErrorResponse(response, e.errorCode as AuthException)
            } catch (e: Exception) {
                log.error("Unexpected exception during authentication creation", e)
                return sendErrorResponse(response, AuthException.IMHERE_ACCESS_DENIED, e.message)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTH_HEADER) ?: return null

        val trimmed = authHeader.trim()
        if (!trimmed.startsWith(BEARER_PREFIX, ignoreCase = false)) {
            log.warn("Authorization header invalid format: starts_with='{}', uri={}",
                trimmed.take(30), request.requestURI)
            return null
        }

        return trimmed.substring(BEARER_PREFIX.length).trim()
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

