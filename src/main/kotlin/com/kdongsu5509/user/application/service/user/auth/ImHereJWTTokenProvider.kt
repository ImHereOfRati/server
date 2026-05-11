package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.adapter.out.auth.jwt.ImHereJwtProperties
import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.dto.JwtTokenClaims
import com.kdongsu5509.user.application.port.out.user.CachePort
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenIssuerPort
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenParserPort
import com.kdongsu5509.user.exception.AuthError
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ImHereJWTTokenProvider(
    private val tokenIssuer: ImHereTokenIssuerPort,
    private val tokenParser: ImHereTokenParserPort,
    private val cachePort: CachePort,
    private val imHereJwtProperties: ImHereJwtProperties
) : JwtTokenProvider {

    override fun issue(claims: JwtTokenClaims): ImHereJwt {
        val accessToken = tokenIssuer.createAccessToken(claims)
        val refreshToken = tokenIssuer.createRefreshToken(claims)

        val redisKey = getTokenRedisKey(claims.email)
        cachePort.save(redisKey, refreshToken, Duration.ofDays(imHereJwtProperties.refreshExpirationDays))

        return ImHereJwt(accessToken, refreshToken)
    }

    override fun reissueByRefreshToken(refreshToken: String): ImHereJwt {
        tokenParser.validate(refreshToken)

        val claims = tokenParser.parse(refreshToken)
        val refreshTokenSavedAtRedis = findTokenFromRedisWithUserEmail(claims.email)

        if (refreshTokenSavedAtRedis != refreshToken) AuthError.IMHERE_INVALID_TOKEN.throwIt()

        return issue(claims)
    }

    override fun reissueByEmail(email: String): ImHereJwt {
        val refreshTokenFromRedis = findTokenFromRedisWithUserEmail(email)
        val claims = tokenParser.parse(refreshTokenFromRedis)

        return issue(claims)
    }

    private fun findTokenFromRedisWithUserEmail(email: String): String {
        val redisKey = getTokenRedisKey(email)

        val refreshTokenFromRedis = cachePort.find(redisKey, String::class.java)
            ?: AuthError.IMHERE_KEY_NOT_FOUND_IN_REDIS.throwIt()

        return refreshTokenFromRedis
    }

    private fun getTokenRedisKey(userEmail: String): String = "refresh:$userEmail"
}
