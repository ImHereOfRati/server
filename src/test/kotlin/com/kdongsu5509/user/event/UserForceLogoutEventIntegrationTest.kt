package com.kdongsu5509.user.event

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.user.service.UserLifecycleService
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class UserForceLogoutEventIntegrationTest : PersistenceTestSupport() {

    @Autowired
    lateinit var userLifecycleService: UserLifecycleService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    @DisplayName("강제 로그아웃 이벤트는 커밋 후 비동기로 처리되고 발행 기록을 완료한다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun force_logout_is_processed_asynchronously_after_commit_and_publication_is_completed() {
        val email = "async-event@test.com"
        val publisherThread = Thread.currentThread().name
        val listenerThread = AtomicReference<String>()
        val listenerStarted = CountDownLatch(1)
        val allowListenerCompletion = CountDownLatch(1)

        doAnswer {
            listenerThread.set(Thread.currentThread().name)
            listenerStarted.countDown()
            allowListenerCompletion.await(5, TimeUnit.SECONDS)
            null
        }.whenever(userRepository).findByEmail(email)

        userLifecycleService.requestForceLogout(email)

        Assertions.assertThat(listenerStarted.await(5, TimeUnit.SECONDS)).isTrue()
        Assertions.assertThat(listenerThread.get())
            .startsWith("application-event-")
            .isNotEqualTo(publisherThread)
        Assertions.assertThat(publicationCount()).isEqualTo(1)

        allowListenerCompletion.countDown()

        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted { Assertions.assertThat(publicationCount()).isZero() }
    }

    private fun publicationCount(): Long =
        jdbcTemplate.queryForObject("select count(*) from event_publication", Long::class.java)!!
}
