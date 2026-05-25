package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.shared.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "fcm_token")
class FcmTokenJpaEntity(
    @Column(nullable = false)
    var token: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var deviceType: DeviceType
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
