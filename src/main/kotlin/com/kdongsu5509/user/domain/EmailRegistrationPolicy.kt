package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.exception.UserException

/**
 * 이메일 재가입(중복) 판정 정책 (U4).
 * 기존에는 User(정보 보유자)의 메서드 validateDuplicateEmailAllowed로 얹혀 있었으나,
 * this(자기 상태)를 전혀 사용하지 않는 무상태 판정이라 도메인 정책 객체로 재배치했다.
 */
object EmailRegistrationPolicy {
    fun assertNotDuplicated(isDuplicated: Boolean) {
        if (isDuplicated) {
            UserException.DUPLICATE_EMAIL.throwIt()
        }
    }
}
