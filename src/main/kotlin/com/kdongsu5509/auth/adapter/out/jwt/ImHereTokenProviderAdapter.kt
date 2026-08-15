package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.shared.cache.CachePort
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserLookupContract
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class ImHereTokenProviderAdapter(
    private val tokenIssuer: ImHereTokenIssuerPort,
    private val tokenParser: ImHereTokenParserPort,
    private val userLookupContract: UserLookupContract,
    private val cachePort: CachePort,
    private val jwtProperties: ImHereJwtProperties,
) : ImHereTokenProviderPort {

    companion object {
        private const val REFRESH_TOKEN_KEY_PREFIX = "refresh:"
    }

    override fun issue(claims: JwtTokenClaims): ImHereJwtToken {
        val issuedClaims = claims.copy(tokenId = UUID.randomUUID().toString())
        val token = issueTokens(issuedClaims)
        saveRefreshToken(issuedClaims.email, token.refreshToken)
        return token
    }

    private fun issueTokens(claims: JwtTokenClaims): ImHereJwtToken = ImHereJwtToken(
        accessToken = tokenIssuer.createAccessToken(claims),
        refreshToken = tokenIssuer.createRefreshToken(claims),
        userStatus = claims.status
    )

    override fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken {
        val claims = tokenParser.parseRefreshToken(refreshToken)

        val user = loadUserByEmail(claims.email)
        val currentTokenId = claims.tokenId
        if (currentTokenId.isNullOrBlank() ||
            cachePort.find(refreshTokenKey(claims.email), String::class.java) != currentTokenId ||
            user.refreshTokenVersion != claims.refreshTokenVersion
        ) {
            AuthException.IMHERE_INVALID_TOKEN.throwIt()
        }

        val newClaims = JwtTokenClaims.fromUser(user).copy(tokenId = UUID.randomUUID().toString())
        val token = issueTokens(newClaims)
        val newTokenId = tokenParser.parseRefreshToken(token.refreshToken).tokenId
            ?: AuthException.IMHERE_INVALID_TOKEN.throwIt()
        if (!cachePort.replace(refreshTokenKey(claims.email), currentTokenId, newTokenId, refreshTokenDuration())) {
            AuthException.IMHERE_INVALID_TOKEN.throwIt()
        }
        return token
    }

    override fun reissueByEmail(email: String): ImHereJwtToken {
        val user = loadUserByEmail(email)
        return issue(JwtTokenClaims.fromUser(user))
    }

    private fun loadUserByEmail(email: String) =
        userLookupContract.findByEmailOrNull(email) ?: AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.throwIt()

    private fun saveRefreshToken(email: String, refreshToken: String) {
        val tokenId = tokenParser.parseRefreshToken(refreshToken).tokenId
            ?: AuthException.IMHERE_INVALID_TOKEN.throwIt()
        cachePort.save(refreshTokenKey(email), tokenId, refreshTokenDuration())
    }

    private fun refreshTokenKey(email: String) = "$REFRESH_TOKEN_KEY_PREFIX$email"

    private fun refreshTokenDuration() = Duration.ofDays(jwtProperties.refreshExpirationDays)
}

