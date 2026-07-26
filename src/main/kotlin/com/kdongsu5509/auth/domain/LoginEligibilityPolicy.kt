package com.kdongsu5509.auth.domain

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.support.exception.throwIt

/**
 * 로그인/재가입 자격 판정 정책.
 *
 * "BLOCKED면 거부, WITHDRAWN이면 거부, PENDING/ACTIVE면 통과"라는 규칙이
 * LoginService/RegisterService/ImHereUserDetails 세 곳에 흩어져 있던 것을
 * auth 경계 안쪽의 단일 정책 객체로 회수한다(user 모듈 UserStatus enum 변경 없이).
 */
object LoginEligibilityPolicy {

    fun assertLoginable(status: UserStatus) {
        when (status) {
            UserStatus.BLOCKED -> AuthException.USER_DISABLED.throwIt()
            UserStatus.WITHDRAWN -> AuthException.USER_WITHDRAWN.throwIt()
            UserStatus.PENDING, UserStatus.ACTIVE -> {}
        }
    }
}
