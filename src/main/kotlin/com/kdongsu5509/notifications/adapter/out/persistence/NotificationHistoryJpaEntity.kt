package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.shared.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "notification_history")
class NotificationHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val receiverEmail: String,

    @Column(nullable = false)
    val senderNickname: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val body: String,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = true)
    val path: String?,

    @Column(nullable = false)
    var isRead: Boolean = false,
) : BaseTimeEntity()
