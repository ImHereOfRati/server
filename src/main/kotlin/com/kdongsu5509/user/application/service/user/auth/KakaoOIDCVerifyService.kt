package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import com.kdongsu5509.user.application.dto.OIDCUserInfo
import com.kdongsu5509.user.application.port.out.user.oauth.OIDCIdTokenVerifyPort
import com.kdongsu5509.user.application.port.out.user.oauth.OIDCVerifyPort
import com.kdongsu5509.user.application.port.out.user.oauth.PublicKeyLoadPort
import com.kdongsu5509.user.exception.AuthError
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class KakaoOIDCVerifyService(
    private val oidcIdTokenVerifyPort: OIDCIdTokenVerifyPort,
    private val publicKeyLoadPort: PublicKeyLoadPort,
) : OIDCVerifyPort {

    override fun verify(idToken: String): OIDCUserInfo {
        val kid = oidcIdTokenVerifyPort.getKid(idToken)
        val publicKey = publicKeyLoadPort.loadPublicKey(kid)

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
            email = payload.email ?: AuthError.OIDC_INVALID.throwIt(customMessage = "ID 토큰에 이메일 정보가 없습니다."),
            nickname = payload.nickname.orEmpty()
        )
    }
}
