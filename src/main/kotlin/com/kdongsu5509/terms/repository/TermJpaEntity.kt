package com.kdongsu5509.terms.repository

import com.kdongsu5509.shared.repository.BaseEntity
import com.kdongsu5509.terms.domain.TermTypes
import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.GenerationType.IDENTITY
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
    @GeneratedValue(strategy = IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var version: Long,

    @Enumerated(STRING)
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
