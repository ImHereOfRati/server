package com.kdongsu5509.user.adapter.out.auth

import com.kdongsu5509.support.exception.AuthErrorCode
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import com.kdongsu5509.user.application.port.out.user.JwtParserPort
import com.kdongsu5509.user.application.port.out.user.JwtVerificationPort
import com.kdongsu5509.user.application.port.out.user.oauth.PublicKeyLoadPort
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.util.*

@Component
class JjwtParserAdapter(
    private val publicKeyLoadPort: PublicKeyLoadPort,
    private val kakaoOIDCProperties: KakaoOIDCProperties,
    private val jwtVerificationPort: JwtVerificationPort
) : JwtParserPort {
    companion object {
        // getKidFromOriginTokenHeader 전용 헤더 디코더. 아래 함수의 주석 참고.
        private val HEADER_JSON_MAPPER = JsonMapper.builder().build()
    }

    override fun parse(idToken: String): OIDCDecodePayload {
        val oidcPublicKey: OIDCPublicKey = findProperOIDCPublicKey(idToken)

        val jws = jwtVerificationPort.verifySignature(
            idToken,
            oidcPublicKey.n,
            oidcPublicKey.e
        )

        return extractPayloadFromJws(jws)
    }

    private fun findProperOIDCPublicKey(idToken: String): OIDCPublicKey {
        val kidFromOriginTokenHeader = getKidFromOriginTokenHeader(idToken)
        return publicKeyLoadPort.loadPublicKey(kidFromOriginTokenHeader)
    }

    /**
     * 카카오 OIDC ID 토큰 헤더에서 kid(key id)만 추출한다. 서명 검증 이전 단계.
     *
     * jjwt를 쓰지 않고 base64 + Jackson으로 헤더를 직접 디코드하는 이유:
     * - jjwt 0.11.x의 parseClaimsJwt는 서명 없는 토큰을 lenient하게 파싱했지만,
     *   0.12.x의 parseUnsecuredClaims는 헤더의 alg가 정확히 "none"인 토큰만 허용한다.
     * - 카카오 ID 토큰은 alg:RS256이라 서명을 잘라내도 parseUnsecuredClaims를 통과할 수 없다.
     * - 이 단계에서는 어떤 공개키를 fetch할지 결정하려고 kid만 필요하므로
     *   서명 검증 없이 헤더만 안전하게 읽으면 된다. 본격적인 iss/aud/서명 검증은
     *   JwtVerificationPort.verifySignature 와 JjwtVerifyAdapter.verifyPayLoad 에서 수행된다.
     *
     * 0.13.x 이상에서 parseUnsecuredClaims 동작이 다시 lenient해지면 jjwt로 되돌릴 수 있다.
     */
    fun getKidFromOriginTokenHeader(token: String): String {
        val parts = token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size != 3) {
            throw BusinessException(AuthErrorCode.OIDC_INVALID)
        }
        return try {
            val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))
            HEADER_JSON_MAPPER.readTree(headerJson).path("kid").stringValue()
                ?: throw BusinessException(AuthErrorCode.OIDC_INVALID)
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            throw BusinessException(AuthErrorCode.OIDC_INVALID)
        }
    }

    private fun extractPayloadFromJws(jws: Jws<Claims>): OIDCDecodePayload {
        val body = jws.payload
        return OIDCDecodePayload(
            iss = body.issuer,
            aud = body.audience.first(),
            sub = body.subject,
            email = body.get("email", String::class.java),
            nickname = body.get("nickname", String::class.java)
        )
    }
}