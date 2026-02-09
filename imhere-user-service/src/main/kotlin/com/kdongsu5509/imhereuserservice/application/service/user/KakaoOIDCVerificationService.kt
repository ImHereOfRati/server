package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.OIDCUserInformation
import com.kdongsu5509.imhereuserservice.application.port.out.user.JwtParserPort
import com.kdongsu5509.imhereuserservice.application.port.out.user.JwtVerificationPort
import com.kdongsu5509.imhereuserservice.application.port.out.user.oauth.OIDCVerificationPort
import io.jsonwebtoken.MalformedJwtException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class KakaoOIDCVerificationService(
    private val jwtParserPort: JwtParserPort,
    private val jwtVerificationPort: JwtVerificationPort
) : OIDCVerificationPort {

    override fun verifyAndReturnUserInformation(idToken: String): OIDCUserInformation {
        val payload = jwtParserPort.parse(idToken)
        jwtVerificationPort.verifyPayLoad(payload)
        payload.email ?: throw MalformedJwtException("ID 토큰에 이메일 정보가 없습니다")
        return OIDCUserInformation(payload.email, payload.nickname.orEmpty())
    }
}