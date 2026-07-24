package com.kdongsu5509.agreement.repository.jpa

import com.kdongsu5509.agreement.domain.AgreementStatus
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "user_agreement",
    indexes = [
        Index(
            name = "idx_user_agreement_history",
            columnList = "user_id, terms_version_id, occurred_at",
        )
    ],
)
@EntityListeners(AuditingEntityListener::class)
class AgreementJpaEntity(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Column(name = "terms_version_id", nullable = false)
    val termId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val action: AgreementStatus,
) {
    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID? = null

    @CreatedDate
    @Column(name = "occurred_at", nullable = false, updatable = false)
    var occurredAt: LocalDateTime = LocalDateTime.now()
}
