package com.common.testsupport

import com.common.testsupport.jwt.ImHereTestJwtProvider
import com.common.testsupport.jwt.KakaoTestJwtProvider
import java.util.*

object TestJwtBuilder {


    fun buildIdToken(email: String): String {
        return KakaoTestJwtProvider.buildIdToken(email)
    }

    fun buildDSKOIdToken(): String {
        return KakaoTestJwtProvider.buildIdToken()
    }

    fun buildImHereAccessToken(
        email: String,
        nickname: String,
        role: String = "NORMAL",
        status: String = "PENDING",
        uid: UUID = UUID.randomUUID()
    ): String = ImHereTestJwtProvider.buildAccessToken(email, nickname, role, status, uid)

    fun buildImHereRefreshToken(
        email: String,
        nickname: String,
        role: String = "NORMAL",
        status: String = "PENDING",
        uid: UUID = UUID.randomUUID()
    ): String = ImHereTestJwtProvider.buildRefreshToken(email, nickname, role, status, uid)
}
