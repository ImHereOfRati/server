package com.kdongsu5509.support.external

import jakarta.servlet.http.HttpServletRequest

/**
 * HTTP 요청에서 알림 표현에 필요한 값(메서드/URI/클라이언트 IP/사용자)만 추출한 값 객체.
 *
 * `HttpServletRequest`에서 값을 캐내던 로직(feature envy)을 한곳에 응집해, 알림 조립부가 서블릿 API에 결합되지 않게 한다.
 */
data class RequestContext(
    val method: String,
    val uri: String,
    val clientIp: String,
    val user: String,
) {
    companion object {
        fun from(request: HttpServletRequest): RequestContext {
            val query = request.queryString
            val uri = if (query.isNullOrEmpty()) request.requestURI else "${request.requestURI}?$query"
            val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                ?: request.remoteAddr
            val user = request.userPrincipal?.name ?: "anonymous"
            return RequestContext(method = request.method, uri = uri, clientIp = clientIp, user = user)
        }
    }
}
