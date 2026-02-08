package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.ImHereJwt
import com.kdongsu5509.imhereuserservice.application.port.out.user.CachePort
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Component
class ImHereJWTTokenProvider(
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val jwtTokenUtil: JwtTokenUtil,
    private val cachePort: CachePort,
) : JwtTokenProvider {
    override fun issueJwtToken(email: String, role: String): ImHereJwt {
        val accessToken = jwtTokenIssuer.createAccessToken(email, role)
        val refreshToken = jwtTokenIssuer.createRefreshToken(email, role)

        val expiredDateTime = jwtTokenUtil.getExpirationDateFromToken(refreshToken).atZone(ZoneId.systemDefault())
        val duration: Duration = Duration.between(Instant.now(), expiredDateTime.toInstant())

        val redisKey = "refresh:$email"
        cachePort.save(redisKey, refreshToken, duration)

        return ImHereJwt(accessToken, refreshToken)
    }

    override fun reissueJwtToken(refreshToken: String): ImHereJwt {
        val username = jwtTokenUtil.getUsernameFromToken(refreshToken)
        val role = jwtTokenUtil.getRoleFromToken(refreshToken)

        // 토큰 유효성 검사 (만료 시간 포함)
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw BusinessException(ErrorCode.IMHERE_INVALID_TOKEN)
        }

        val refreshTokenFromRedis = cachePort.find("refresh:$username") as String?

        if (refreshTokenFromRedis != null && refreshTokenFromRedis == refreshToken) {
            return issueJwtToken(username, role)
        }

        throw IllegalArgumentException("일치하지 않는 리프레시 토큰")
    }
}