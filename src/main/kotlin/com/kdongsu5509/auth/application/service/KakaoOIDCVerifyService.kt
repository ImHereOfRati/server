package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.out.oauth.OIDCProperties
import com.kdongsu5509.auth.application.port.out.OIDCIdTokenVerifyPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.port.out.PublicKeyLoadPort
import com.kdongsu5509.auth.application.service.dto.OIDCDecodePayload
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OIDCVerifyService(
    private val oidcIdTokenVerifyPort: OIDCIdTokenVerifyPort,
    private val publicKeyLoadPort: PublicKeyLoadPort,
    private val oidcProperties: OIDCProperties,
) : OIDCVerifyPort {

    override fun verify(provider: OAuth2Provider, idToken: String, nonce: String?): OIDCUserInfo {
        val providerProperties = oidcProperties.get(provider)
        val kid = oidcIdTokenVerifyPort.getKid(idToken)
        val publicKey = publicKeyLoadPort.findByKeyId(provider, kid)

        val jws = oidcIdTokenVerifyPort.verifySignature(idToken, publicKey.n, publicKey.e)

        val payload = OIDCDecodePayload(
            iss = jws.payload.issuer ?: "",
            aud = jws.payload.audience?.firstOrNull() ?: "",
            sub = jws.payload.subject ?: "",
            nonce = jws.payload["nonce"] as? String,
            email = jws.payload["email"] as? String,
            nickname = jws.payload["nickname"] as? String,
            name = jws.payload["name"] as? String
        )

        val expectedNonce = nonce?.takeIf { it.isNotBlank() } ?: AuthException.OIDC_NONCE_INVALID.throwIt()

        oidcIdTokenVerifyPort.verifyPayLoad(payload, providerProperties.issuer, providerProperties.audience, expectedNonce)

        return OIDCUserInfo(
            email = payload.email
                ?: AuthException.OIDC_MISSING_EMAIL.throwIt(customMessage = "ID 토큰에 이메일 정보가 없습니다."),
            nickname = resolveNickname(payload),
            sub = payload.sub
        )
    }

    private fun resolveNickname(payload: OIDCDecodePayload): String {
        return payload.nickname
            ?.takeIf { it.isNotBlank() }
            ?: payload.name?.takeIf { it.isNotBlank() }
            ?: payload.email.orEmpty().substringBefore("@").ifBlank { "user" }
    }
}
