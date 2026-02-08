package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.common.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "terms_versions")
class TermsVersionJpaEntity(
    @Column(nullable = false)
    var version: String,

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    var content: String,

    @Column(nullable = false)
    var isActive: Boolean = false,

    @Column(nullable = false)
    var effectiveDate: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id")
    var terms: TermsDefinitionJpaEntity // 어떤 약관의 버전인지 명시
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val versionId: Long? = null
}