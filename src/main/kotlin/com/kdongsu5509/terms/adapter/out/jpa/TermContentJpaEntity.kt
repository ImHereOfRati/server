package com.kdongsu5509.terms.adapter.out.jpa

import com.kdongsu5509.user.adapter.out.persistence.common.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "term_content")
class TermContentJpaEntity(
    @Column(nullable = false)
    var version: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false)
    var effectiveDate: LocalDateTime,

    @Column(nullable = false)
    var isActive: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_policy_id")
    var termPolicy: TermPolicyJpaEntity
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}
