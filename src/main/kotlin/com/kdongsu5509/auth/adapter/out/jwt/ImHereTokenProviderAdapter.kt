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

        val tokenKey = getTokenCacheKey(claims.email)
        cachePort.save(tokenKey, refreshToken, Duration.ofDays(imHereJwtProperties.refreshExpirationDays))

        return ImHereJwtToken(accessToken, refreshToken, claims.status)
    }

    override fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken {
        tokenParser.validate(refreshToken)

        val claims = tokenParser.parse(refreshToken)
        val refreshTokenSavedAtCache = findTokenFromCacheWithUserEmail(claims.email)

        if (refreshTokenSavedAtCache != refreshToken) AuthException.IMHERE_INVALID_TOKEN.throwIt()

        return issue(claims)
    }

    override fun reissueByEmail(email: String): ImHereJwtToken {
        val refreshTokenFromCache = findTokenFromCacheWithUserEmail(email)
        val claims = tokenParser.parse(refreshTokenFromCache)

        return issue(claims)
    }

    private fun findTokenFromCacheWithUserEmail(email: String): String {
        val tokenKey = getTokenCacheKey(email)
        return cachePort.find(tokenKey, String::class.java) ?: AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.throwIt()
    }

    private fun getTokenCacheKey(userEmail: String): String = "refresh:$userEmail"
}

