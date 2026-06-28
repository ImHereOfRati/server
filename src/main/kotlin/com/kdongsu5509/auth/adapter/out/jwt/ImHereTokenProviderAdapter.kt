package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class ImHereTokenProviderAdapter(
    private val tokenIssuer: ImHereTokenIssuerPort,
    private val tokenParser: ImHereTokenParserPort,
    private val userRepository: UserRepository
) : ImHereTokenProviderPort {

    override fun issue(claims: JwtTokenClaims): ImHereJwtToken {
        val accessToken = tokenIssuer.createAccessToken(claims)
        val refreshToken = tokenIssuer.createRefreshToken(claims)

        return ImHereJwtToken(accessToken, refreshToken, claims.status)
    }

    override fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken {
        tokenParser.validate(refreshToken)

        val claims = tokenParser.parse(refreshToken)

        val user = findUserByEmail(claims.email)
        if (user.refreshTokenVersion != claims.refreshTokenVersion) AuthException.IMHERE_INVALID_TOKEN.throwIt()

        return issue(claims.copy(refreshTokenVersion = user.refreshTokenVersion))
    }

    override fun reissueByEmail(email: String): ImHereJwtToken {
        val user = findUserByEmail(email)
        return issue(JwtTokenClaims.fromUser(user))
    }

    private fun findUserByEmail(email: String) =
        userRepository.findByEmail(email) ?: AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.throwIt()
}

