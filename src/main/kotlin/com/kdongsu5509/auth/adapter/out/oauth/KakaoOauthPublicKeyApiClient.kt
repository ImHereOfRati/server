package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface KakaoOauthPublicKeyApiClient {

    @GetExchange("/.well-known/jwks.json")
    fun fetchKakaoPublicKey(): OIDCPublicKeyResponse
}
