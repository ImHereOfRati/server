package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.exception.UserException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class EmailRegistrationPolicyTest {

    @Test
    @DisplayName("이미 존재하는 이메일이면 DUPLICATE_EMAIL 예외를 던진다")
    fun assertNotDuplicated_fail_when_duplicated() {
        assertThatThrownBy {
            EmailRegistrationPolicy.assertNotDuplicated(isDuplicated = true)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.DUPLICATE_EMAIL)
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외 없이 통과한다")
    fun assertNotDuplicated_success_when_not_duplicated() {
        assertThatCode {
            EmailRegistrationPolicy.assertNotDuplicated(isDuplicated = false)
        }.doesNotThrowAnyException()
    }
}
