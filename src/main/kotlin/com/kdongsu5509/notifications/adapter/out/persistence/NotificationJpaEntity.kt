package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.shared.repository.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "notification",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_notification_dedupe_key", columnNames = ["dedupe_key"])
    ],
    indexes = [
        Index(
            name = "idx_notification_inbox",
            columnList = "target_identifier, method, status, created_at"
        ),
        Index(name = "idx_notification_status", columnList = "status, created_at"),
        Index(name = "idx_notification_provider_message_id", columnList = "provider_message_id"),
    ]
)
class NotificationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 120)
    val dedupeKey: String,

    @Column(nullable = false)
    val targetIdentifier: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val method: NotificationMethod,

    @Column(nullable = false)
    val senderAlias: String,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 500)
    val body: String,

    @Convert(converter = ExtraDataConverter::class)
    @Column(nullable = false, length = 2000)
    val extraData: Map<String, String>,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: NotificationStatus,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(nullable = true, length = 500)
    var lastError: String? = null,

    @Column(nullable = true)
    var sentAt: java.time.LocalDateTime? = null,

    @Column(nullable = true, length = 80)
    var providerMessageId: String? = null,

    @Column(nullable = true, length = 30)
    var providerStatus: String? = null,

    @Column(nullable = false)
    var isRead: Boolean = false,
) : BaseTimeEntity()
