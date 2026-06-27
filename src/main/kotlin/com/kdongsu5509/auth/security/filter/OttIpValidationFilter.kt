package com.kdongsu5509.auth.security.filter

import com.kdongsu5509.auth.security.ClientIpResolver
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

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // 이 필터는 SecurityConfig의 admin 체인(/admin/**, /api/admin/**)에만 wiring되어 있으므로
        // 도달한 모든 요청을 검사하면 admin 전 경로(로그인 페이지 포함)가 allowlist로 잠긴다.
        val clientIp = ClientIpResolver.resolve(httpRequest)
        if (!isIpAllowed(clientIp)) {
            httpResponse.status = HttpStatus.FORBIDDEN.value()
            httpResponse.contentType = "application/json"
            httpResponse.writer.write("""{"error": "Forbidden"}""")
            return
        }

        chain.doFilter(request, response)
    }

    private fun isIpAllowed(clientIp: String): Boolean {
        val allowed = ottIpFilterConfig.allowedIps.contains(clientIp)
        log.info("OTT IP Validation: clientIp=$clientIp, allowedIps=${ottIpFilterConfig.allowedIps}, allowed=$allowed")
        return allowed
    }
}
