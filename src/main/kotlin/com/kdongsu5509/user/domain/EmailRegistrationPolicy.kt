package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.exception.UserException

object EmailRegistrationPolicy {
    fun assertNotDuplicated(isDuplicated: Boolean) {
        if (isDuplicated) {
            UserException.DUPLICATE_EMAIL.throwIt()
        }
    }
}
