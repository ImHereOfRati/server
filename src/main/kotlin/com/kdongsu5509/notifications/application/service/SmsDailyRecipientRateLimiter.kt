package com.kdongsu5509.notifications.application.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.support.logger.logger
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Component
class SmsDailyRecipientRateLimiter {
    private val log = logger()
    private val buckets: Cache<DailyKey, DailyRecipients> = Caffeine.newBuilder()
        .maximumSize(MAX_BUCKETS)
        .expireAfterAccess(Duration.ofDays(2))
        .build()

    fun reserve(senderId: UUID, targetIdentifiers: List<String>) {
        val recipients = targetIdentifiers.map(::normalize).toSet()
        if (recipients.isEmpty()) return

        val state = buckets.get(DailyKey(senderId, LocalDate.now(ZONE_ID))) { DailyRecipients() }
        synchronized(state) {
            val newRecipients = recipients - state.recipients
            if (!state.bucket.tryConsume(newRecipients.size.toLong())) {
                val retryAfterSeconds = secondsUntilTomorrow()
                log.warn(
                    "SMS 일일 수신자 한도 초과: senderId={}, 사용량={}/{}, 재시도까지={}초",
                    senderId,
                    state.recipients.size,
                    DAILY_LIMIT,
                    retryAfterSeconds
                )
                NotificationException.SMS_DAILY_RECIPIENT_LIMIT.throwIt(
                    contextData = mapOf(
                        "limit" to DAILY_LIMIT,
                        "used" to state.recipients.size,
                        "retryAfterSeconds" to retryAfterSeconds
                    )
                )
            }
            state.recipients.addAll(newRecipients)
            log.info(
                "SMS 일일 수신자 예약: senderId={}, 신규 수신자={}, 사용량={}/{}",
                senderId,
                newRecipients.size,
                state.recipients.size,
                DAILY_LIMIT
            )
        }
    }

    private fun normalize(value: String): String = value.filter(Char::isDigit)

    private fun secondsUntilTomorrow(): Long {
        val now = java.time.ZonedDateTime.now(ZONE_ID)
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(ZONE_ID)).seconds.coerceAtLeast(1)
    }

    private data class DailyKey(val senderId: UUID, val date: LocalDate)

    private class DailyRecipients {
        val recipients = mutableSetOf<String>()
        val bucket: Bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(DAILY_LIMIT.toLong(), Refill.intervally(DAILY_LIMIT.toLong(), Duration.ofDays(1))))
            .build()
    }

    private companion object {
        const val DAILY_LIMIT = 4
        const val MAX_BUCKETS = 100_000L
        val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
