package com.kdongsu5509.auth.application.service.dto

data class OIDCDecodePayload(
    val iss: String, // ID Token을 발급한 OAuth 2.0 제공자의 URL
    val aud: String, // ID Token이 발급된 앱의 앱 키
    val sub: String, // 사용자 식별값
    val email: String?, // 이메일
    val nickname: String? // 닉네임
)
