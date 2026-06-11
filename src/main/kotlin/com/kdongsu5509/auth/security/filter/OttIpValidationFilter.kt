package com.kdongsu5509.auth.security.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.slf4j.LoggerFactory

class OttIpValidationFilter(
    private val ottIpFilterConfig: OttIpFilterConfig
) : Filter {

    private val log = LoggerFactory.getLogger(OttIpValidationFilter::class.java)

    companion object {
        private const val OTT_REQUEST_URL = "/admin/ott/request"
        private const val X_FORWARDED_FOR = "X-Forwarded-For"
        private const val X_REAL_IP = "X-Real-IP"
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (shouldValidateIp(httpRequest)) {
            val clientIp = extractClientIp(httpRequest)
            if (!isIpAllowed(clientIp)) {
                httpResponse.status = HttpStatus.FORBIDDEN.value()
                httpResponse.contentType = "application/json"
                httpResponse.writer.write("""{"error": "Forbidden"}""")
                return
            }
        }

        chain.doFilter(request, response)
    }

    private fun shouldValidateIp(request: HttpServletRequest): Boolean {
        return request.requestURI == OTT_REQUEST_URL && request.method == "POST"
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        var ip = request.getHeader(X_FORWARDED_FOR)
        if (ip.isNullOrEmpty() || ip.contains("unknown")) {
            ip = request.getHeader(X_REAL_IP)
        }
        if (ip.isNullOrEmpty() || ip.contains("unknown")) {
            ip = request.remoteAddr
        }
        return ip?.split(",")?.firstOrNull()?.trim() ?: request.remoteAddr
    }

    private fun isIpAllowed(clientIp: String): Boolean {
        val allowed = ottIpFilterConfig.allowedIps.contains(clientIp)
        log.info("OTT IP Validation: clientIp=$clientIp, allowedIps=${ottIpFilterConfig.allowedIps}, allowed=$allowed")
        return allowed
    }
}
