package com.kdongsu5509.user.application.port.out.user.oauth

import com.kdongsu5509.user.application.dto.OIDCUserInfo

interface OIDCVerifyPort {
    fun verify(idToken: String): OIDCUserInfo
}
