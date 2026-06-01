package com.kdongsu5509.support.logger

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.time.LocalDateTime
import java.util.*

class LoggingFilter(
    private val logPrinter: AccessLogPrinter
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (isIgnoredUrl(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        val cachingRequest = ContentCachingRequestWrapper(request, 65536)
        val cachingResponse = ContentCachingResponseWrapper(response)

        val traceId = UUID.randomUUID().toString()
        val requestAt = LocalDateTime.now()

        MDC.put("traceId", traceId)
        MDC.put("method", request.method)
        MDC.put("uri", request.requestURI)

        try {
            filterChain.doFilter(cachingRequest, cachingResponse)
        } finally {
            val responseAt = LocalDateTime.now()
            val durationMs = java.time.Duration.between(requestAt, responseAt).toMillis()

            MDC.put("status", cachingResponse.status.toString())
            MDC.put("durationMs", durationMs.toString())

            val accessLog = AccessLog(
                traceId = traceId,
                method = request.method,
                uri = request.requestURI,
                queryString = request.queryString,
                requestBody = getRequestBody(cachingRequest),
                responseBody = getResponseBody(cachingResponse),
                headers = extractHeaders(request),
                userAgent = request.getHeader("User-Agent"),
                remoteIp = extractRemoteIp(request),
                status = cachingResponse.status,
                threadName = Thread.currentThread().name,
                requestAt = requestAt,
                responseAt = responseAt,
                durationMs = durationMs
            )

            logPrinter.print(accessLog, true)
            cachingResponse.copyBodyToResponse()
            MDC.clear()
        }
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        val content = request.contentAsByteArray
        if (content.isEmpty()) return ""
        return String(content, Charsets.UTF_8)
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String {
        val content = response.contentAsByteArray
        if (content.isEmpty()) return ""
        return String(content, Charsets.UTF_8)
    }

    private fun extractHeaders(request: HttpServletRequest): Map<String, String> {
        val headerMap = request.headerNames.asSequence()
            .associateWith { name -> request.getHeaders(name).asSequence().toList() }
        return AccessLog.extractHeaders(headerMap)
    }

    private fun extractRemoteIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrEmpty()) {
            return forwarded.split(",")[0].trim()
        }
        return request.remoteAddr ?: "unknown"
    }

    private fun isIgnoredUrl(uri: String): Boolean {
        return uri.startsWith("/actuator") ||
                uri.startsWith("/health") ||
                uri.startsWith("/favicon.ico") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs")
    }
}
