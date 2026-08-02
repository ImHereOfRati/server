package com.kdongsu5509.auth.security

import jakarta.servlet.http.HttpServletRequest

object ClientIpResolver {

    private const val X_REAL_IP = "X-Real-IP"
    private const val X_FORWARDED_FOR = "X-Forwarded-For"
    private const val UNKNOWN = "unknown"

    fun resolve(request: HttpServletRequest): String {
        val realIp = request.getHeader(X_REAL_IP)
        if (!realIp.isNullOrBlank() && !realIp.contains(UNKNOWN)) {
            return realIp.trim()
        }

        val xff = request.getHeader(X_FORWARDED_FOR)
        if (!xff.isNullOrBlank() && !xff.contains(UNKNOWN)) {
            val lastHop = xff.split(",").map { it.trim() }.lastOrNull { it.isNotEmpty() }
            if (!lastHop.isNullOrEmpty()) {
                return lastHop
            }
        }

        return request.remoteAddr
    }
}
