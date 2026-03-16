package com.kdongsu5509.user.application.service.user

import com.kdongsu5509.support.exception.AuthErrorCode
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.port.out.user.CachePort
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*

@Component
class ImHereJWTTokenProvider(
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val jwtTokenUtil: JwtTokenUtil,
    private val cachePort: CachePort,
) : JwtTokenProvider {
    override fun issueJwtToken(
        id: UUID,
        email: String,
        role: String
    ): ImHereJwt {
        val accessToken = jwtTokenIssuer.createAccessToken(id, email, role)
        val refreshToken = jwtTokenIssuer.createRefreshToken(id, email, role)

        val expiredDateTime = jwtTokenUtil.getExpirationDateFromToken(refreshToken).atZone(ZoneId.systemDefault())
        val duration: Duration = Duration.between(Instant.now(), expiredDateTime.toInstant())

        val redisKey = "refresh:$email"
        cachePort.save(redisKey, refreshToken, duration)

        return ImHereJwt(accessToken, refreshToken)
    }

    override fun reissueJwtToken(refreshToken: String): ImHereJwt {
        val username = jwtTokenUtil.getUsernameFromToken(refreshToken)
        val role = jwtTokenUtil.getRoleFromToken(refreshToken)
        val uid = jwtTokenUtil.getUIDFromToken(refreshToken)

        // 토큰 유효성 검사 (만료 시간 포함)
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw BusinessException(AuthErrorCode.IMHERE_INVALID_TOKEN)
        }

        val refreshTokenFromRedis = cachePort.find("refresh:$username") as String?

        if (refreshTokenFromRedis != null && refreshTokenFromRedis == refreshToken) {
            return issueJwtToken(uid, username, role)
        }

        throw IllegalArgumentException("일치하지 않는 리프레시 토큰")
    }
}