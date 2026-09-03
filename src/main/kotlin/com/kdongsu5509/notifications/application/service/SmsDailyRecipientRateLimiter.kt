package com.kdongsu5509.notifications.application.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.support.logger.logger
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

private const val DAILY_LIMIT = 4
private const val MAX_TRACKED_SENDERS_PER_DAY = 100_000L
private val BUCKET_RETENTION: Duration = Duration.ofDays(2)
private val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

@Component
class SmsDailyRecipientRateLimiter {

    private val log = logger()

    private val bucketsBySenderAndDate: Cache<DailyBucketKey, DailyRecipientBucket> = Caffeine.newBuilder()
        .maximumSize(MAX_TRACKED_SENDERS_PER_DAY)
        .expireAfterAccess(BUCKET_RETENTION)
        .build()

    fun reserve(senderId: UUID, targetIdentifiers: List<String>) {
        val candidates = targetIdentifiers.map(::normalizePhoneNumber).toSet()
        if (candidates.isEmpty()) return

        val bucket = bucketsBySenderAndDate.get(
            DailyBucketKey(senderId, LocalDate.now(ZONE_ID))
        ) {
            DailyRecipientBucket()
        }

        when (val result = bucket.reserve(candidates)) {
            is ReservationResult.Accepted -> log.info(
                "SMS 일일 수신자 예약: senderId={}, 신규 수신자={}, 사용량={}/{}",
                senderId, result.newlyReservedCount, result.usedCount, DAILY_LIMIT
            )

            is ReservationResult.Rejected -> {
                log.warn(
                    "SMS 일일 수신자 한도 초과: senderId={}, 사용량={}/{}, 재시도까지={}초",
                    senderId, result.usedCount, DAILY_LIMIT, result.retryAfterSeconds
                )
                NotificationException.SMS_DAILY_RECIPIENT_LIMIT.throwIt(
                    contextData = mapOf(
                        "limit" to DAILY_LIMIT,
                        "used" to result.usedCount,
                        "retryAfterSeconds" to result.retryAfterSeconds
                    )
                )
            }
        }
    }

    private fun normalizePhoneNumber(value: String): String = value.filter(Char::isDigit)
}

private data class DailyBucketKey(val senderId: UUID, val date: LocalDate)

private sealed interface ReservationResult {
    data class Accepted(val newlyReservedCount: Int, val usedCount: Int) : ReservationResult
    data class Rejected(val usedCount: Int, val retryAfterSeconds: Long) : ReservationResult
}

/** 발신자 1명의 하루치 수신자 집합과 그 한도를 함께 관리한다. */
private class DailyRecipientBucket {

    private val reservedRecipients = mutableSetOf<String>()

    private val bucket: Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(DAILY_LIMIT.toLong())
                .refillIntervally(DAILY_LIMIT.toLong(), Duration.ofDays(1))
                .build()
        )
        .build()

    @Synchronized
    fun reserve(candidates: Set<String>): ReservationResult {
        val newRecipients = candidates - reservedRecipients
        if (!bucket.tryConsume(newRecipients.size.toLong())) {
            return ReservationResult.Rejected(reservedRecipients.size, secondsUntilTomorrow())
        }

        reservedRecipients += newRecipients
        return ReservationResult.Accepted(newRecipients.size, reservedRecipients.size)
    }

    private fun secondsUntilTomorrow(): Long {
        val now = ZonedDateTime.now(ZONE_ID)
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(ZONE_ID)).seconds.coerceAtLeast(1)
    }
}
