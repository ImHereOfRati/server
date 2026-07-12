package com.kdongsu5509.auth.domain

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.support.exception.throwIt

/**
 * 리프레시 토큰 버전 일치 불변식.
 *
 * 토큰에 실린 refreshTokenVersion이 사용자의 현재 버전과 일치해야 재발급을 허용한다.
 * (강제 로그아웃/토큰 로테이션 시 버전을 올려 이전 리프레시 토큰을 무효화하는 규칙)
 *
 * 이전에는 이 판정이 out-adapter(ImHereTokenProviderAdapter, Interfacer 스테레오타입)에
 * 인라인되어 도메인 정책이 어댑터로 새어 있었다. 판정 지식을 도메인 정책 객체로 회수한다.
 */
object RefreshTokenVersionPolicy {

    fun assertMatches(currentVersion: Long, tokenVersion: Long) {
        if (currentVersion != tokenVersion) {
            AuthException.IMHERE_INVALID_TOKEN.throwIt()
        }
    }
}
