package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.CachePort
import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ImHereTokenProviderAdapter(
    private val tokenIssuer: ImHereTokenIssuerPort,
    private val tokenParser: ImHereTokenParserPort,
    private val cachePort: CachePort,
    private val imHereJwtProperties: ImHereJwtProperties
) : ImHereTokenProviderPort {

    override fun issue(claims: JwtTokenClaims): ImHereJwtToken {
        val accessToken = tokenIssuer.createAccessToken(claims)
        val refreshToken = tokenIssuer.createRefreshToken(claims)

        val redisKey = getTokenRedisKey(claims.email)
        cachePort.save(redisKey, refreshToken, Duration.ofDays(imHereJwtProperties.refreshExpirationDays))

        return ImHereJwtToken(accessToken, refreshToken, claims.status)
    }

    override fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken {
        tokenParser.validate(refreshToken)

        val claims = tokenParser.parse(refreshToken)
        val refreshTokenSavedAtRedis = findTokenFromRedisWithUserEmail(claims.email)

        if (refreshTokenSavedAtRedis != refreshToken) AuthException.IMHERE_INVALID_TOKEN.throwIt()

        return issue(claims)
    }

    override fun reissueByEmail(email: String): ImHereJwtToken {
        val refreshTokenFromRedis = findTokenFromRedisWithUserEmail(email)
        val claims = tokenParser.parse(refreshTokenFromRedis)

        return issue(claims)
    }

    private fun findTokenFromRedisWithUserEmail(email: String): String {
        val redisKey = getTokenRedisKey(email)
        return cachePort.find(redisKey, String::class.java) ?: AuthException.IMHERE_KEY_NOT_FOUND_IN_REDIS.throwIt()
    }

    private fun getTokenRedisKey(userEmail: String): String = "refresh:$userEmail"
}

