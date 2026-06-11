package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.`in`.ForceLogoutUseCase
import com.kdongsu5509.auth.application.port.out.CachePort
import org.springframework.stereotype.Service

@Service
class ForceLogoutService(
    private val cachePort: CachePort
) : ForceLogoutUseCase {

    override fun logout(userEmail: String) {
        val tokenKey = "refresh:$userEmail"
        cachePort.delete(tokenKey)
    }
}
