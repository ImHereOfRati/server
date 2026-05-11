package com.kdongsu5509.user.adapter.`in`.web.user

import com.kdongsu5509.support.response.APIResponseBody
import com.kdongsu5509.support.response.toOkResponse
import com.kdongsu5509.support.response.toSuccessResponse
import com.kdongsu5509.user.adapter.`in`.web.user.dto.request.AuthenticationRequest
import com.kdongsu5509.user.adapter.`in`.web.user.dto.request.ReAuthenticationRequest
import com.kdongsu5509.user.adapter.`in`.web.user.dto.response.AuthenticationResponse
import com.kdongsu5509.user.application.port.`in`.user.AuthenticateWithOidcUseCase
import com.kdongsu5509.user.application.port.`in`.user.ReissueJWTUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user/auth", version = "1")
class AuthController(
    val authenticateWithOidcUseCase: AuthenticateWithOidcUseCase,
    val reissueJwtUseCase: ReissueJWTUseCase
) {
    @PostMapping("/login")
    fun handleIdToken(@Validated @RequestBody authenticationRequest: AuthenticationRequest): ResponseEntity<APIResponseBody<AuthenticationResponse>> {
        val authResult =
            authenticateWithOidcUseCase.authenticate(authenticationRequest.idToken, authenticationRequest.provider)
        val response = AuthenticationResponse.fromAuthenticationProcessResult(authResult)
        return convertToProperResponse(authResult.isNewUser, response)
    }

    private fun convertToProperResponse(
        isNewUser: Boolean,
        response: AuthenticationResponse
    ): ResponseEntity<APIResponseBody<AuthenticationResponse>> {
        if (isNewUser) {
            return response.toSuccessResponse(HttpStatus.CREATED)
        }
        return response.toOkResponse()
    }

    @PostMapping("/reissue")
    fun handleJwtTokenReissueRequest(@Validated @RequestBody reAuthenticationRequest: ReAuthenticationRequest): ResponseEntity<APIResponseBody<AuthenticationResponse>> {
        val jwt = reissueJwtUseCase.reissueByRefreshToken(reAuthenticationRequest.refreshToken)
        return AuthenticationResponse(jwt.accessToken, jwt.refreshToken).toOkResponse()
    }
}
