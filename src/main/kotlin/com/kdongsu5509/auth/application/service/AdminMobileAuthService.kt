package com.kdongsu5509.auth.application.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.auth.adapter.out.jwt.AdminMobileAuthProperties
import com.kdongsu5509.auth.adapter.out.jwt.ImHereJjwtIssuerAdapter
import com.kdongsu5509.auth.adapter.out.jwt.ImHereJjwtParserAdapter
import com.kdongsu5509.auth.adapter.out.jwt.ImHereJwtProperties
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.adapter.`in`.web.dto.AdminMobileChallengeResponse
import com.kdongsu5509.auth.adapter.`in`.web.dto.AdminMobileTokenResponse
import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.shared.cache.CachePort
import com.kdongsu5509.support.exception.throwIt
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.slf4j.LoggerFactory
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class AdminMobileAuthService(
    private val properties: AdminMobileAuthProperties,
    private val issuer: ImHereJjwtIssuerAdapter,
    private val parser: ImHereJjwtParserAdapter,
    private val jwtProperties: ImHereJwtProperties,
    private val cache: CachePort,
    @Value("\${admin.id}") private val adminId: String,
    @Value("\${admin.nickname:rati}") private val nickname: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val passwordEncoder = BCryptPasswordEncoder()
    private val challenges: Cache<String, ChallengeState> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofSeconds(properties.challengeExpirationSeconds))
        .build()
    private val loginFailures: Cache<String, Int> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build()

    fun begin(admin: String, password: String, clientIp: String = "unknown"): AdminMobileChallengeResponse {
        val loginFailureKey = "admin-login:${admin.trim()}:$clientIp"
        if ((loginFailures.getIfPresent(loginFailureKey) ?: 0) >= MAX_LOGIN_FAILURES) {
            reject("Too many login attempts")
        }

        if (admin != adminId || properties.passwordHash.isBlank() || !passwordEncoder.matches(password, properties.passwordHash)) {
            val attempts = loginFailures.asMap().merge(loginFailureKey, 1) { current, _ -> current + 1 } ?: 1
            if (attempts >= MAX_LOGIN_FAILURES) {
                log.warn("Admin login rate limit reached for adminId={} from clientIp={}", admin, clientIp)
            }
            reject("Invalid administrator credentials")
        }
        loginFailures.invalidate(loginFailureKey)
        val challenge = UUID.randomUUID().toString()
        challenges.put(
            challenge,
            ChallengeState(Instant.now().plusSeconds(properties.challengeExpirationSeconds), 0)
        )
        return AdminMobileChallengeResponse(challenge, properties.challengeExpirationSeconds)
    }

    fun verify(challenge: String, code: String): AdminMobileTokenResponse {
        val state = challenges.getIfPresent(challenge)
        if (state == null || state.expiresAt.isBefore(Instant.now())) {
            reject("Invalid MFA code")
        }
        if (properties.totpSecret.isBlank() || !verifyTotp(code, properties.totpSecret)) {
            val nextAttempts = state.failedAttempts + 1
            if (nextAttempts >= MAX_MFA_FAILURES) {
                challenges.invalidate(challenge)
                log.warn("Admin MFA challenge invalidated after too many failures")
            } else {
                challenges.put(challenge, state.copy(failedAttempts = nextAttempts))
            }
            reject("Invalid MFA code")
        }
        challenges.invalidate(challenge)
        val claims = claims()
        val access = issuer.createAdminAccessToken(claims)
        val refreshClaims = claims.copy(tokenId = UUID.randomUUID().toString())
        val refresh = issuer.createRefreshToken(refreshClaims)
        cache.save(refreshKey(refreshClaims.tokenId!!), refreshClaims.tokenId, Duration.ofDays(properties.refreshExpirationDays))
        return AdminMobileTokenResponse(access, refresh, jwtProperties.adminExpirationMinutes * 60)
    }

    fun refresh(refreshToken: String): AdminMobileTokenResponse {
        val claims = try { parser.parseRefreshToken(refreshToken) } catch (_: Exception) { reject("Invalid refresh token") }
        val tokenId = claims.tokenId ?: reject("Invalid refresh token")
        val current = cache.find(refreshKey(tokenId), String::class.java)
        if (current != tokenId || claims.email != adminId) reject("Invalid refresh token")
        val nextClaims = claims().copy(tokenId = UUID.randomUUID().toString())
        val nextRefresh = issuer.createRefreshToken(nextClaims)
        if (!cache.replace(refreshKey(tokenId), tokenId, nextClaims.tokenId!!, Duration.ofDays(properties.refreshExpirationDays))) {
            reject("Refresh token reuse detected")
        }
        return AdminMobileTokenResponse(issuer.createAdminAccessToken(nextClaims), nextRefresh, jwtProperties.adminExpirationMinutes * 60)
    }

    fun session(): com.kdongsu5509.auth.adapter.`in`.web.dto.AdminMobileSessionResponse =
        com.kdongsu5509.auth.adapter.`in`.web.dto.AdminMobileSessionResponse(adminId, nickname, "ADMIN")

    private fun claims() = JwtTokenClaims(
        uid = UUID.nameUUIDFromBytes("imhere-admin:$adminId".toByteArray()),
        email = adminId,
        nickname = nickname,
        role = "ADMIN",
        status = "ACTIVE",
    )

    private fun refreshKey(tokenId: String) = "admin-mobile-refresh:$tokenId"

    private data class ChallengeState(
        val expiresAt: Instant,
        val failedAttempts: Int,
    )

    private companion object {
        const val MAX_LOGIN_FAILURES = 5
        const val MAX_MFA_FAILURES = 5
    }


    private fun reject(message: String): Nothing = AuthException.IMHERE_INVALID_TOKEN.throwIt(cause = IllegalArgumentException(message))

    private fun verifyTotp(value: String, secret: String): Boolean {
        val normalized = value.trim()
        if (!normalized.matches(Regex("\\d{6}"))) return false
        val key = decodeBase32(secret)
        val counter = Instant.now().epochSecond / 30
        return (-1L..1L).any { offset ->
            val data = ByteBuffer.allocate(8).putLong(counter + offset).array()
            val hash = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key, "HmacSHA1")) }.doFinal(data)
            val index = hash.last().toInt() and 0x0f
            val binary = ((hash[index].toInt() and 0x7f) shl 24) or
                ((hash[index + 1].toInt() and 0xff) shl 16) or
                ((hash[index + 2].toInt() and 0xff) shl 8) or
                (hash[index + 3].toInt() and 0xff)
            binary % 1_000_000 == normalized.toInt()
        }
    }

    private fun decodeBase32(value: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0
        var bits = 0
        val output = ArrayList<Byte>()
        value.uppercase().filter { it != '=' && !it.isWhitespace() }.forEach { ch ->
            val digit = alphabet.indexOf(ch)
            if (digit < 0) return ByteArray(0)
            buffer = (buffer shl 5) or digit
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output += ((buffer shr bits) and 0xff).toByte()
            }
        }
        return output.toByteArray()
    }
}
