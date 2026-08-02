package com.kdongsu5509.support.external

import jakarta.servlet.http.HttpServletRequest

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
