package com.kdongsu5509.auth.domain

enum class UserStatus {
    PENDING,    // 가입 대기
    ACTIVE,     // 활성
    BLOCKED,    // 서비스 차원의 차단
    WITHDRAWN   // 탈퇴
}
