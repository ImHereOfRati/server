package com.kdongsu5509.auth.security.handler

import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.shared.response.APIResponseSerializers
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.util.*

@Component
class OttLoginSuccessHandler(
    private val tokenIssuer: ImHereTokenIssuerPort,
    @param:Value("\${admin.id}") private val adminId: String,
    @param:Value("\${admin.nickname}") private val adminNickname: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val adminClaims = JwtTokenClaims(
            uid = UUID.nameUUIDFromBytes(adminId.toByteArray()),
            email = adminId,
            nickname = adminNickname,
            role = UserRole.ADMIN.name,
            status = UserStatus.ACTIVE.name
        )

        val accessToken = tokenIssuer.createAdminAccessToken(adminClaims)

        APIResponseSerializers.writeSuccessResponse(
            response = response,
            data = mapOf("accessToken" to accessToken)
        )
    }
}
