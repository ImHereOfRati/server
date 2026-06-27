package com.kdongsu5509.auth.security

import jakarta.servlet.http.HttpServletRequest

/**
 * 신뢰 가능한 클라이언트 IP를 추출한다.
 *
 * 보안 핵심: nginx는 `proxy_set_header X-Real-IP $remote_addr`로 X-Real-IP를 항상 덮어쓰므로
 * client가 직접 주입한 X-Real-IP는 무력화된다. 반면 X-Forwarded-For는
 * `$proxy_add_x_forwarded_for`(append)로 전달되어 첫 요소를 공격자가 위조할 수 있다.
 * 따라서 X-Real-IP를 최우선 신뢰하고, 부재 시(nginx 미경유)에만 XFF의 **마지막**
 * 요소(= 직전 프록시가 append한 실제 peer)로 fallback 한다. XFF 첫 요소는 절대 신뢰하지 않는다.
 */
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
