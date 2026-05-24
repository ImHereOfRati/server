package com.kdongsu5509.terms.adapter.out

import com.kdongsu5509.shared.BaseEntity
import com.kdongsu5509.terms.domain.TermTypes
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "terms",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_terms_type_version",
            columnNames = ["type", "version"]
        )
    ]
)
class TermJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var version: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: TermTypes,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false)
    var effectiveDate: LocalDateTime,

    @Column(nullable = false)
    var isRequired: Boolean
) : BaseEntity()
