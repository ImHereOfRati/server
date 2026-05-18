package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.OIDCIdTokenVerifyPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.port.out.PublicKeyLoadPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import com.kdongsu5509.user.application.dto.OIDCUserInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class KakaoOIDCVerifyService(
    private val oidcIdTokenVerifyPort: OIDCIdTokenVerifyPort,
    private val publicKeyLoadPort: PublicKeyLoadPort,
) : OIDCVerifyPort {

    override fun verify(provider: OAuth2Provider, idToken: String): OIDCUserInfo {
        val kid = oidcIdTokenVerifyPort.getKid(idToken)
        val publicKey = publicKeyLoadPort.findByKeyId(kid)

        val jws = oidcIdTokenVerifyPort.verifySignature(idToken, publicKey.n, publicKey.e)

        val payload = OIDCDecodePayload(
            iss = jws.payload.issuer ?: "",
            aud = jws.payload.audience?.firstOrNull() ?: "",
            sub = jws.payload.subject ?: "",
            email = jws.payload["email"] as? String,
            nickname = jws.payload["nickname"] as? String
        )

        oidcIdTokenVerifyPort.verifyPayLoad(payload)

        return OIDCUserInfo(
            email = payload.email
                ?: AuthException.OIDC_MISSING_EMAIL.throwIt(customMessage = "ID 토큰에 이메일 정보가 없습니다."),
            nickname = payload.nickname.orEmpty()
        )
    }
}
