package com.kdongsu5509.imhereuserservice.application.port.out.user.oauth

import com.kdongsu5509.imhereuserservice.application.dto.OIDCUserInformation

interface OIDCVerificationPort {
    fun verifyAndReturnUserInformation(idToken: String): OIDCUserInformation
}