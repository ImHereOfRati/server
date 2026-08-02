package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.shared.repository.BaseTimeEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "fcm_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_fcm_token_owner_id", columnNames = ["owner_id"])
    ]
)
class FcmTokenJpaEntity(
    @Column(nullable = false)
    var token: String,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var deviceType: DeviceType
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
