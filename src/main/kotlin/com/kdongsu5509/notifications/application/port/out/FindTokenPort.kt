package com.kdongsu5509.imhere.notification.application.port.out

import com.kdongsu5509.imhere.notification.application.domain.FcmToken

interface FindTokenPort {
    fun findByUserEmail(userEmail: String): FcmToken?
}