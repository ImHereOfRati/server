package com.kdongsu5509.notifications.application.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SmsDailyRecipientRateLimiterConcurrencyTest {

    @Test
    fun `동일 발신자-동일 수신자 300스레드 동시 요청은 예외 없이 1건만 신규 예약된다`() {
        val limiter = SmsDailyRecipientRateLimiter()
        val senderId = UUID.randomUUID()
        val phone = listOf("010-0000-9999")
        val threadCount = 300

        val exceptions = CopyOnWriteArrayList<Throwable>()
        val pool = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)

        repeat(threadCount) {
            pool.submit {
                ready.countDown()
                start.await()
                try {
                    limiter.reserve(senderId, phone)
                } catch (e: Throwable) {
                    exceptions += e
                } finally {
                    done.countDown()
                }
            }
        }

        ready.await()
        start.countDown()
        done.await(30, TimeUnit.SECONDS)
        pool.shutdown()

        exceptions.forEach { it.printStackTrace() }
        println("threads=$threadCount, exceptions=${exceptions.size}")
        exceptions.groupBy { it.javaClass.name }.forEach { (type, list) ->
            println("  $type: ${list.size}건, 첫 메시지=${list.first().message}")
        }

        assertThat(exceptions).isEmpty()
    }
}
