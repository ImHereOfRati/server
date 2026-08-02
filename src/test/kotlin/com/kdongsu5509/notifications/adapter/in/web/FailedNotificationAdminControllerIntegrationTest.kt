package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataFcmTokenRepository
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataNotificationRepository
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FailedNotificationAdminControllerIntegrationTest : WebIntegrationTestSupport() {
    @Autowired
    private lateinit var persistencePort: NotificationPersistencePort

    @Autowired
    private lateinit var fcmTokenPersistencePort: FcmTokenPersistencePort

    @Autowired
    private lateinit var notificationRepository: SpringDataNotificationRepository

    @Autowired
    private lateinit var fcmTokenRepository: SpringDataFcmTokenRepository

    private val admin = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")
    private val user = ImHereUserDetails("user@example.com", "user", "USER", "ACTIVE")

    @BeforeEach
    fun clean() {
        notificationRepository.deleteAll()
        fcmTokenRepository.deleteAll()
    }

    @Test
    @DisplayName("관리자는 DEAD 알림 목록을 조회한다")
    fun find_all_dead() {
        val dead = saveDead("list")

        mockMvc.perform(get("/api/admin/failed-notifications").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(dead.id))
            .andExpect(jsonPath("$.data[0].status").value("DEAD"))
    }

    @Test
    @DisplayName("관리자는 실패 알림 단건을 조회한다")
    fun find_one() {
        val dead = saveDead("detail")

        mockMvc.perform(get("/api/admin/failed-notifications/{id}", dead.id).with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.targetIdentifier").value(dead.targetIdentifier))
    }

    @Test
    @DisplayName("관리자 단건 재발송은 DEAD를 SENT로 전이한다")
    fun redeliver_one() {
        val dead = saveDead("one")
        saveToken(dead.targetIdentifier)

        mockMvc.perform(
            post("/api/admin/failed-notifications/{id}/redelivery-jobs", dead.id)
                .with(csrf())
                .with(user(admin))
        ).andExpect(status().isOk)

        assertThat(persistencePort.findById(requireNotNull(dead.id))!!.status).isEqualTo(NotificationStatus.SENT)
    }

    @Test
    @DisplayName("관리자 일괄 재발송은 지정한 수만큼 처리한다")
    fun redeliver_batch() {
        val first = saveDead("batch-1")
        val second = saveDead("batch-2")
        saveToken(first.targetIdentifier)
        saveToken(second.targetIdentifier)

        mockMvc.perform(
            post("/api/admin/failed-notifications/redelivery-jobs")
                .param("count", "2")
                .with(csrf())
                .with(user(admin))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.requestedCount").value(2))

        assertThat(persistencePort.findById(requireNotNull(first.id))!!.status).isEqualTo(NotificationStatus.SENT)
        assertThat(persistencePort.findById(requireNotNull(second.id))!!.status).isEqualTo(NotificationStatus.SENT)
    }

    @Test
    @DisplayName("관리자는 DEAD 알림 기록을 폐기한다")
    fun discard() {
        val dead = saveDead("discard")

        mockMvc.perform(
            delete("/api/admin/failed-notifications/{id}", dead.id)
                .with(csrf())
                .with(user(admin))
        ).andExpect(status().isOk)

        assertThat(persistencePort.findById(requireNotNull(dead.id))).isNull()
    }

    @Test
    @DisplayName("DEAD가 아닌 알림은 재발송할 수 없다")
    fun reject_non_dead_redelivery() {
        val pending = persistencePort.save(requested("pending"))

        mockMvc.perform(
            post("/api/admin/failed-notifications/{id}/redelivery-jobs", pending.id)
                .with(csrf())
                .with(user(admin))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("일반 사용자는 실패 알림 관리자 API에 접근할 수 없다")
    fun reject_non_admin() {
        mockMvc.perform(get("/api/admin/failed-notifications").with(user(user)))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 실패 알림 관리자 API에 접근할 수 없다")
    fun reject_unauthenticated() {
        mockMvc.perform(get("/api/admin/failed-notifications"))
            .andExpect(status().isUnauthorized)
    }

    private fun saveDead(key: String): Notification {
        var notification = requested(key)
        repeat(Notification.MAX_ATTEMPTS) { notification = notification.markFailed("외부 채널 실패") }
        return persistencePort.save(notification)
    }

    private fun requested(key: String): Notification =
        Notification.request(
            dedupeKey = "$key:FCM",
            targetIdentifier = recipientIdOf(key).toString(),
            method = NotificationMethod.FCM,
            rendered = NotificationTemplate.render(NotificationType.DELIVERY_FAILED_NOTICE, "ImHere"),
        )

    private fun recipientIdOf(key: String): UUID = UUID.nameUUIDFromBytes(key.toByteArray())

    private fun saveToken(targetIdentifier: String) {
        val ownerId = UUID.fromString(targetIdentifier)
        fcmTokenPersistencePort.save(
            FcmToken(ownerId = ownerId, fcmToken = "token-$ownerId", deviceType = DeviceType.AOS)
        )
    }
}
