package com.kdongsu5509.user.adapter.out.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "one_time_tokens")
class OneTimeTokenEntity(
    @Id
    @Column(name = "token_value")
    val tokenValue: String,

    @Column(nullable = false)
    val username: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime
)
