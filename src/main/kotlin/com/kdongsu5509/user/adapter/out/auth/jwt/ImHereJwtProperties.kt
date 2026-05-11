package com.kdongsu5509.user.adapter.out.auth.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * application.yml의 'jwt' 접두사를 가진 설정값들을 매핑하는 데이터 클래스입니다.
 * 시크릿 키, 토큰별 만료 시간 등 자체 JWT 운영에 필요한 설정값들을 보관합니다.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
data class ImHereJwtProperties(
    var secret: String = "",

    var accessExpirationMinutes: Long = 0,

    var refreshExpirationDays: Long = 0,

    var adminExpirationMinutes: Long = 5,

    var accessHeaderName: String = "",
)
